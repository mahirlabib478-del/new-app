import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

void main() {
  test('reminder sync refreshes the scheduler timezone before scheduling', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': false,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final scheduler = _TimezoneTrackingScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();

    expect(scheduler.initializeCalls, 1);
    expect(scheduler.refreshTimeZoneCalls, 1);
    expect(scheduler.refreshOccurredAfterInitialize, isTrue);
  });
}

class _TimezoneTrackingScheduler implements ReminderScheduler {
  int initializeCalls = 0;
  int refreshTimeZoneCalls = 0;
  bool refreshOccurredAfterInitialize = false;

  @override
  Future<void> initialize() async {
    initializeCalls++;
  }

  @override
  Future<void> refreshTimeZone() async {
    refreshTimeZoneCalls++;
    refreshOccurredAfterInitialize = initializeCalls > 0;
  }

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {}

  @override
  Future<void> showNow({required int id, required String title, required String body}) async {}

  @override
  Future<void> cancel(int id) async {}

  @override
  Future<bool?> requestPermissions() async => null;
}
