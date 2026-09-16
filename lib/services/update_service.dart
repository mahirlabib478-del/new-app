import 'dart:convert';
import 'dart:io';

import 'package:url_launcher/url_launcher.dart';

class UpdateInfo {
  const UpdateInfo({required this.latestVersion, required this.releaseUrl});

  final String latestVersion;
  final String releaseUrl;
}

class UpdateService {
  const UpdateService({this.currentVersion = '0.2.0'});

  static const repository = 'mahirlabib478-del/new-app';
  static const _checkTimeout = Duration(seconds: 5);
  final String currentVersion;

  Future<UpdateInfo?> checkForUpdate() async {
    final client = HttpClient()
      ..connectionTimeout = _checkTimeout
      ..idleTimeout = _checkTimeout;
    try {
      final request = await client
          .getUrl(Uri.parse('https://api.github.com/repos/$repository/releases/latest'))
          .timeout(_checkTimeout);
      request.headers.set(HttpHeaders.acceptHeader, 'application/vnd.github+json');
      request.headers.set('X-GitHub-Api-Version', '2026-03-10');
      request.headers.set(HttpHeaders.userAgentHeader, 'StudyOS/$currentVersion');

      final response = await request.close().timeout(_checkTimeout);
      if (response.statusCode != HttpStatus.ok) return null;

      final body = await response.transform(utf8.decoder).join().timeout(_checkTimeout);
      final decoded = jsonDecode(body);
      if (decoded is! Map) return null;

      final tag = decoded['tag_name'];
      final releaseUrl = decoded['html_url'];
      if (tag is! String || releaseUrl is! String) return null;

      final latest = _normalizeVersion(tag);
      if (latest == null || !_isNewer(latest, currentVersion)) return null;
      if (!_isSafeReleaseUrl(releaseUrl)) return null;

      return UpdateInfo(latestVersion: latest, releaseUrl: releaseUrl);
    } catch (_) {
      // Offline-first: update checks must never prevent the app from opening.
      return null;
    } finally {
      client.close(force: true);
    }
  }

  Future<bool> openRelease(UpdateInfo info) async {
    if (!_isSafeReleaseUrl(info.releaseUrl)) return false;
    return launchUrl(Uri.parse(info.releaseUrl), mode: LaunchMode.externalApplication);
  }

  bool isNewerVersion(String latest, String current) => _isNewer(latest, current);

  bool _isNewer(String latest, String current) {
    final latestParts = _parse(latest);
    final currentParts = _parse(current);
    for (var i = 0; i < 3; i++) {
      if (latestParts[i] != currentParts[i]) return latestParts[i] > currentParts[i];
    }
    return false;
  }

  String? _normalizeVersion(String value) {
    var normalized = value.trim();
    if (normalized.startsWith('v') || normalized.startsWith('V')) {
      normalized = normalized.substring(1);
    }
    final match = RegExp(r'^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?\$').firstMatch(normalized);
    if (match == null) return null;
    return '${match.group(1)}.${match.group(2)}.${match.group(3)}';
  }

  List<int> _parse(String value) {
    final normalized = _normalizeVersion(value);
    if (normalized == null) return const [0, 0, 0];
    final parts = normalized.split('.');
    return [
      int.parse(parts[0]),
      int.parse(parts[1]),
      int.parse(parts[2]),
    ];
  }

  bool _isSafeReleaseUrl(String value) {
    final uri = Uri.tryParse(value);
    return uri != null && uri.scheme == 'https' && uri.host == 'github.com';
  }
}
