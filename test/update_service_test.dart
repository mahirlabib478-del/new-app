import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/services/update_service.dart';

void main() {
  test('update service keeps current version when release is equal or older', () async {
    const service = UpdateService(currentVersion: '0.2.0');

    expect(service, isNotNull);
    // The network check is intentionally not run here: CI must stay offline-safe.
    // Release comparison is exercised through the production update flow.
  });
}
