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
  final String currentVersion;

  Future<UpdateInfo?> checkForUpdate() async {
    try {
      final client = HttpClient();
      try {
        final request = await client.getUrl(Uri.parse('https://api.github.com/repos/$repository/releases/latest'));
        request.headers.set(HttpHeaders.acceptHeader, 'application/vnd.github+json');
        request.headers.set('X-GitHub-Api-Version', '2026-03-10');
        request.headers.set(HttpHeaders.userAgentHeader, 'StudyOS/$currentVersion');
        final response = await request.close().timeout(const Duration(seconds: 5));
        if (response.statusCode != HttpStatus.ok) return null;
        final body = await response.transform(utf8.decoder).join();
        final json = jsonDecode(body);
        if (json is! Map<String, dynamic>) return null;
        final tag = json['tag_name'] as String?;
        final releaseUrl = json['html_url'] as String?;
        if (tag == null || releaseUrl == null) return null;
        final latest = tag.startsWith('v') ? tag.substring(1) : tag;
        if (!_isNewer(latest, currentVersion)) return null;
        return UpdateInfo(latestVersion: latest, releaseUrl: releaseUrl);
      } finally {
        client.close(force: true);
      }
    } catch (_) {
      // Offline-first: update checks must never prevent the app from opening.
      return null;
    }
  }

  Future<bool> openRelease(UpdateInfo info) async {
    return launchUrl(Uri.parse(info.releaseUrl), mode: LaunchMode.externalApplication);
  }

  bool _isNewer(String latest, String current) {
    final latestParts = _parse(latest);
    final currentParts = _parse(current);
    for (var i = 0; i < 3; i++) {
      if (latestParts[i] != currentParts[i]) return latestParts[i] > currentParts[i];
    }
    return false;
  }

  List<int> _parse(String value) {
    final parts = value.split('.');
    return List<int>.generate(3, (index) {
      final raw = index < parts.length ? parts[index] : '0';
      final digits = raw.replaceFirst(RegExp(r'[^0-9].*'), '');
      return int.tryParse(digits) ?? 0;
    });
  }
}
