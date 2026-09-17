import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/services/notification_service.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

void main() {
  setUpAll(tz.initializeTimeZones);

  test('next daily occurrence stays on today when the time is still ahead', () {
    final now = tz.TZDateTime.utc(2026, 9, 16, 18, 30);

    final next = NotificationService.nextDailyOccurrence(now, 19, 0);

    expect(next, tz.TZDateTime.utc(2026, 9, 16, 19, 0));
  });

  test('next daily occurrence rolls to tomorrow when the time has passed', () {
    final now = tz.TZDateTime.utc(2026, 9, 16, 19, 30);

    final next = NotificationService.nextDailyOccurrence(now, 19, 0);

    expect(next, tz.TZDateTime.utc(2026, 9, 17, 19, 0));
  });

  test('same-time daily occurrence rolls to tomorrow instead of firing immediately', () {
    final now = tz.TZDateTime.utc(2026, 9, 16, 19, 0);

    final next = NotificationService.nextDailyOccurrence(now, 19, 0);

    expect(next, tz.TZDateTime.utc(2026, 9, 17, 19, 0));
  });

  test('daily occurrence preserves the supplied timezone location', () {
    final location = tz.getLocation('Asia/Dhaka');
    final now = tz.TZDateTime(location, 2026, 9, 16, 18, 30);

    final next = NotificationService.nextDailyOccurrence(now, 19, 0);

    expect(next.location, same(location));
    expect(next.year, 2026);
    expect(next.month, 9);
    expect(next.day, 16);
    expect(next.hour, 19);
    expect(next.minute, 0);
  });

  test('reminder time is safely clamped to a valid clock value', () {
    final now = tz.TZDateTime.utc(2026, 9, 16, 8, 0);

    final next = NotificationService.nextDailyOccurrence(now, 99, -4);

    expect(next, tz.TZDateTime.utc(2026, 9, 16, 23, 0));
  });
}
