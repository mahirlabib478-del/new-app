import 'dart:convert';
import 'dart:io';

import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';

class UpdateInfo {
  const UpdateInfo({required this.latestVersion, required this.releaseUrl, this.apkUrl, this.isMandatory = false});
  final String latestVersion;
  final String releaseUrl;
  final String? apkUrl;
  final bool isMandatory;
}

class UpdatePolicy {
  const UpdatePolicy({required this.latestVersion, required this.minimumSupportedVersion, required this.releaseUrl, this.apkUrl});
  final String latestVersion;
  final String minimumSupportedVersion;
  final String releaseUrl;
  final String? apkUrl;

  Map<String, dynamic> toJson() => {
        'latestVersion': latestVersion,
        'minimumSupportedVersion': minimumSupportedVersion,
        'releaseUrl': releaseUrl,
        'apkUrl': apkUrl ?? '',
      };

  factory UpdatePolicy.fromJson(Map<String, dynamic> json) {
    final latest = json['latestVersion'];
    final minimum = json['minimumSupportedVersion'];
    final release = json['releaseUrl'];
    final apk = json['apkUrl'];
    if (latest is! String || minimum is! String || release is! String) throw const FormatException('Invalid update policy');
    return UpdatePolicy(
      latestVersion: latest,
      minimumSupportedVersion: minimum,
      releaseUrl: release,
      apkUrl: apk is String && apk.trim().isNotEmpty ? apk : null,
    );
  }
}

class _Version {
  const _Version(this.major, this.minor, this.patch, this.build);

  final int major;
  final int minor;
  final int patch;
  final int build;

  List<int> get parts => [major, minor, patch, build];
}

class UpdateService {
  const UpdateService({this.currentVersion = '0.2.0', this.prefs});

  static const repository = 'mahirlabib478-del/new-app';
  static const policyUrl = 'https://raw.githubusercontent.com/$repository/main/update.json';
  static const _checkTimeout = Duration(seconds: 5);
  static const _policyCacheKey = 'update_policy_cache';

  final String currentVersion;
  final SharedPreferences? prefs;

  Future<UpdateInfo?> checkForUpdate() async {
    final policy = await fetchPolicy();
    if (policy == null) return null;
    final latest = _normalizeVersion(policy.latestVersion);
    final minimum = _normalizeVersion(policy.minimumSupportedVersion);
    if (latest == null || minimum == null || !_isSafeReleaseUrl(policy.releaseUrl)) return null;
    if (policy.apkUrl != null && !_isSafeDownloadUrl(policy.apkUrl!)) return null;
    final mandatory = isMandatoryVersion(minimum, currentVersion);
    final optional = _isNewer(latest, currentVersion);
    if (!mandatory && !optional) return null;
    return UpdateInfo(latestVersion: _formatVersion(latest), releaseUrl: policy.releaseUrl, apkUrl: policy.apkUrl, isMandatory: mandatory);
  }

  Future<UpdatePolicy?> fetchPolicy() async {
    try {
      final client = HttpClient()..connectionTimeout = _checkTimeout..idleTimeout = _checkTimeout;
      try {
        final request = await client.getUrl(Uri.parse(policyUrl)).timeout(_checkTimeout);
        request.headers.set(HttpHeaders.userAgentHeader, 'StudyOS/$currentVersion');
        final response = await request.close().timeout(_checkTimeout);
        if (response.statusCode != HttpStatus.ok) return _loadCachedPolicy();
        final body = await response.transform(utf8.decoder).join().timeout(_checkTimeout);
        final decoded = jsonDecode(body);
        if (decoded is! Map) return _loadCachedPolicy();
        final policy = UpdatePolicy.fromJson(Map<String, dynamic>.from(decoded));
        if (!_isValidPolicy(policy)) return _loadCachedPolicy();
        await prefs?.setString(_policyCacheKey, jsonEncode(policy.toJson()));
        return policy;
      } finally {
        client.close(force: true);
      }
    } catch (_) {
      return _loadCachedPolicy();
    }
  }

  Future<bool> openRelease(UpdateInfo info) async {
    final target = info.apkUrl ?? info.releaseUrl;
    if (!_isSafeDownloadUrl(target)) return false;
    return launchUrl(Uri.parse(target), mode: LaunchMode.externalApplication);
  }

  bool isMandatoryVersion(String minimumSupported, String current) {
    final minimum = _normalizeVersion(minimumSupported);
    final normalizedCurrent = _normalizeVersion(current);
    if (minimum == null || normalizedCurrent == null) return false;
    return _compareVersions(normalizedCurrent, minimum) < 0;
  }

  bool isNewerVersion(String latest, String current) => _isNewer(latest, current);

  bool _isNewer(String latest, String current) {
    final latestNormalized = _normalizeVersion(latest);
    final currentNormalized = _normalizeVersion(current);
    if (latestNormalized == null || currentNormalized == null) return false;
    return _compareVersions(latestNormalized, currentNormalized) > 0;
  }

  _Version? _normalizeVersion(String value) {
    var normalized = value.trim();
    if (normalized.startsWith('v') || normalized.startsWith('V')) normalized = normalized.substring(1);
    final match = RegExp(r'^(\\d+)\\.(\\d+)\\.(\\d+)(?:\\+(\\d+))?(?:-[^+]+)?$').firstMatch(normalized);
    if (match == null) return null;
    return _Version(
      int.parse(match.group(1)!),
      int.parse(match.group(2)!),
      int.parse(match.group(3)!),
      int.tryParse(match.group(4) ?? '0') ?? 0,
    );
  }

  int _compareVersions(_Version left, _Version right) {
    for (var i = 0; i < left.parts.length; i++) {
      if (left.parts[i] != right.parts[i]) return left.parts[i].compareTo(right.parts[i]);
    }
    return 0;
  }

  String _formatVersion(_Version version) {
    return '\${version.major}.\${version.minor}.\${version.patch}\${version.build > 0 ? '+\${version.build}' : ''}';
  }

  UpdatePolicy? _loadCachedPolicy() {
    final raw = prefs?.getString(_policyCacheKey);
    if (raw == null) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! Map) return null;
      final policy = UpdatePolicy.fromJson(Map<String, dynamic>.from(decoded));
      return _isValidPolicy(policy) ? policy : null;
    } catch (_) {
      return null;
    }
  }

  bool _isValidPolicy(UpdatePolicy policy) {
    final latest = _normalizeVersion(policy.latestVersion);
    final minimum = _normalizeVersion(policy.minimumSupportedVersion);
    if (latest == null || minimum == null || !_isSafeReleaseUrl(policy.releaseUrl)) return false;
    if (policy.apkUrl != null && !_isSafeDownloadUrl(policy.apkUrl!)) return false;
    return _compareVersions(minimum, latest) <= 0;
  }

  bool _isSafeReleaseUrl(String value) {
    final uri = Uri.tryParse(value);
    return uri != null && uri.scheme == 'https' && uri.host == 'github.com';
  }

  bool _isSafeDownloadUrl(String value) {
    final uri = Uri.tryParse(value);
    if (uri == null || uri.scheme != 'https') return false;
    return uri.host == 'github.com' || uri.host == 'objects.githubusercontent.com';
  }
}
