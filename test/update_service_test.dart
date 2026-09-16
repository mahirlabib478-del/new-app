import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/services/update_service.dart';

void main() {
  const service = UpdateService(currentVersion: '0.2.0');

  test('version comparison handles equal, older and newer releases', () {
    expect(service.isNewerVersion('0.2.0', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.1.9', '0.2.0'), isFalse);
    expect(service.isNewerVersion('0.2.1', '0.2.0'), isTrue);
    expect(service.isNewerVersion('1.0.0', '0.99.99'), isTrue);
  });

  test('version comparison normalizes v prefix and ignores release metadata', () {
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

  test('network update check remains offline-safe', () async {
    final result = await service.checkForUpdate();
    expect(result, anyOf(isNull, isA<UpdateInfo>()));
  });
}
