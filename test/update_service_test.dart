import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/update_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const service = UpdateService(currentVersion: '0.2.0');

  test('version comparison handles equal, older and newer releases', () {
    expect(service.isNewerVersion('0.2.0', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.1.9', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.2.1', '0.2.0'), isTrue);
    expect(service.isNewerVersion('1.0.0', '0.99.99'), isTrue);
  });

  test('version comparison normalizes v prefix and release metadata', () {
    expect(service.isNewerVersion('v0.3.0', '0.2.9'), isTrue);
    expect(service.isNewerVersion('V0.3.0-beta.1', '0.3.0'), isFalse);
    expect(service.isNewerVersion('0.3.0+12', '0.2.9'), isTrue);
  });

  test('malformed versions are never treated as newer', () {
    expect(service.isNewerVersion('latest', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.3', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.3.0.1', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.3.x', '0.2.0'), isFalse);
  });

  test('minimum supported version becomes mandatory only when current is below it', () {
    expect(service.isMandatoryVersion('0.2.0', '0.2.0'), isFalse);
    expect(service.isMandatoryVersion('0.2.1', '0.2.0'), isTrue);
    expect(service.isMandatoryVersion('v1.0.0', '0.9.9'), isTrue);
    expect(service.isMandatoryVersion('0.1.9', '0.2.0'), isFalse);
    expect(service.isMandatoryVersion('invalid', '0.2.0'), isFalse);
  });

  test('cached policy is used when the network check fails', () async {
    final policy = const UpdatePolicy(
      latestVersion: '0.4.0',
      minimumSupportedVersion: '0.2.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
    );
    SharedPreferences.setMockInitialValues({
      'update_policy_cache': jsonEncode(policy.toJson()),
    });
    final prefs = await SharedPreferences.getInstance();
    final cachedService = UpdateService(currentVersion: '0.2.0', prefs: prefs);

    final result = await cachedService.fetchPolicy();
    expect(result?.latestVersion, '0.4.0');
    expect(result?.minimumSupportedVersion, '0.2.0');
  });

  test('malformed cached policy is ignored', () async {
    SharedPreferences.setMockInitialValues({
      'update_policy_cache': '{"latestVersion":"latest"}',
    });
    final prefs = await SharedPreferences.getInstance();
    final cachedService = UpdateService(currentVersion: '0.2.0', prefs: prefs);

    expect(await cachedService.fetchPolicy(), isNull);
  });

  test('invalid cached release URL is ignored', () async {
    final policy = const UpdatePolicy(
      latestVersion: '0.4.0',
      minimumSupportedVersion: '0.2.0',
      releaseUrl: 'http://example.com/release',
    );
    SharedPreferences.setMockInitialValues({
      'update_policy_cache': jsonEncode(policy.toJson()),
    });
    final prefs = await SharedPreferences.getInstance();
    final cachedService = UpdateService(currentVersion: '0.2.0', prefs: prefs);

    expect(await cachedService.fetchPolicy(), isNull);
  });

  test('mandatory update is surfaced from a valid cached policy', () async {
    final policy = const UpdatePolicy(
      latestVersion: '0.4.0',
      minimumSupportedVersion: '0.3.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
    );
    SharedPreferences.setMockInitialValues({
      'update_policy_cache': jsonEncode(policy.toJson()),
    });
    final prefs = await SharedPreferences.getInstance();
    final cachedService = UpdateService(currentVersion: '0.2.0', prefs: prefs);

    final result = await cachedService.checkForUpdate();
    expect(result, isNotNull);
    expect(result!.isMandatory, isTrue);
    expect(result.latestVersion, '0.4.0');
  });
}
