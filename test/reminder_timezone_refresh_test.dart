import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
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

  test('timezone changes invalidate an existing daily reminder schedule', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(
      StudyPlan(
        totalMinutes: 25,
        items: [StudyItem(title: 'Math', minutes: 25)],
      ),
    );

    final scheduler = _TimezoneTrackingScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    scheduler.timeZoneFingerprintValue = 'America/New_York';
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.studyId]);
    expect(
      scheduler.cancelled.where((id) => id == ReminderCoordinator.studyId).length,
      0,
    );
  });
}

class _TimezoneTrackingScheduler implements ReminderScheduler, ReminderSchedulerTimeZoneAware {
  int initializeCalls = 0;
  int refreshTimeZoneCalls = 0;
  bool refreshOccurredAfterInitialize = false;
  String timeZoneFingerprintValue = 'Asia/Dhaka';
  final List<int> scheduled = <int>[];
  final List<int> cancelled = <int>[];

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
  String get timeZoneFingerprint => timeZoneFingerprintValue;

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {
    scheduled.add(id);
  }

  @override
  Future<void> showNow({required int id, required String title, required String body}) async {}

  @override
  Future<void> cancel(int id) async {
    cancelled.add(id);
  }

  @override
  Future<bool> areNotificationsEnabled() async => true;

  @override
  Future<bool?> requestPermissions() async => null;
}
