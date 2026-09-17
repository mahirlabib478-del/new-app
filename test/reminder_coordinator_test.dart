import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

class FakeScheduler implements ReminderScheduler {
  final List<int> cancelled = [];
  final List<int> scheduled = [];
  final List<int> shown = [];
  bool permission = true;
  bool failInitialization = false;
  bool failPermission = false;
  bool failSchedule = false;
  bool failShow = false;

  @override
  Future<void> initialize() async {
    if (failInitialization) throw StateError('init failed');
  }

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {
    if (failSchedule) throw StateError('schedule failed');
    scheduled.add(id);
  }

  @override
  Future<void> showNow({
    required int id,
    required String title,
    required String body,
  }) async {
    if (failShow) throw StateError('show failed');
    shown.add(id);
  }

  @override
  Future<void> cancel(int id) async {
    cancelled.add(id);
  }

  @override
  Future<bool?> requestPermissions() async {
    if (failPermission) throw StateError('permission failed');
    return permission;
  }
}

void main() {
  test('default settings schedule only the plan reminder when no plan exists', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('latest settings override schedules a newly enabled study reminder', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final settings = ReminderSettings.defaults.copyWith(studyEnabled: true);
    await coordinator.sync(settingsOverride: settings);

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.planId]);
  });

  test('focus completion resyncs daily reminders after the plan becomes complete', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_plan_enabled': true,
      'reminder_break_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    scheduler.scheduled.clear();
    await store.recordFocusBlock(0, 25);
    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });

  test('study reminder is cancelled when the daily goal is reached before the plan is complete', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_plan_enabled': false,
      'daily_goal_minutes': 25,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    await store.recordFocusBlock(0, 25);
    await coordinator.sync();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('disabled break reminder prevents focus completion notification', () async {
    SharedPreferences.setMockInitialValues({'reminder_break_enabled': false});
    final prefs = await SharedPreferences.getInstance();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: FakeScheduler(),
    );

    await coordinator.notifyFocusBlockCompleted();

    expect((coordinator.scheduler as FakeScheduler).shown, isEmpty);
  });

  test('sync cancels a stale break notification when break reminders are disabled', () async {
    SharedPreferences.setMockInitialValues({'reminder_break_enabled': false});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();

    expect(scheduler.cancelled, contains(ReminderCoordinator.breakId));
  });

  test('enabled break reminder shows a notification after focus completion', () async {
    SharedPreferences.setMockInitialValues({'reminder_break_enabled': true});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.shown, [ReminderCoordinator.breakId]);
  });

  test('permission requests are best effort and forwarded to the scheduler', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final result = await coordinator.requestPermissions();

    expect(result, true);
  });

  test('permission request failure does not escape into the study flow', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..failPermission = true;
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    expect(await coordinator.requestPermissions(), isNull);
  });

  test('scheduler initialization failure leaves the study flow usable', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..failInitialization = true;
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await expectLater(coordinator.sync(), completes);
    expect(scheduler.scheduled, isEmpty);
  });

  test('daily schedule failure is swallowed and retried on the next sync', () async {
    SharedPreferences.setMockInitialValues({'reminder_plan_enabled': true});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..failSchedule = true;
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    expect(scheduler.scheduled, isEmpty);
    scheduler.failSchedule = false;
    await coordinator.sync();
    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('focus notification failure still refreshes daily reminders', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_break_enabled': true,
      'reminder_study_enabled': false,
      'reminder_plan_enabled': true,
    });
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..failShow = true;
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('repeated sync does not reschedule an unchanged reminder', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('a fresh coordinator re-syncs reminders after app restart', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));

    final firstScheduler = FakeScheduler();
    final firstCoordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: firstScheduler,
    );
    await firstCoordinator.sync();

    final restartedScheduler = FakeScheduler();
    final restartedCoordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: restartedScheduler,
    );
    await restartedCoordinator.sync();

    expect(firstScheduler.scheduled, [ReminderCoordinator.studyId]);
    expect(restartedScheduler.cancelled, [ReminderCoordinator.breakId, ReminderCoordinator.studyId, ReminderCoordinator.planId]);
    expect(restartedScheduler.scheduled, [ReminderCoordinator.studyId]);
  });

  test('concurrent sync requests coalesce into one unchanged schedule', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await Future.wait([coordinator.sync(), coordinator.sync()]);

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('latest concurrent settings override wins the coalesced refresh', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final studySettings = ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false);
    final planSettings = ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true);
    await coordinator.sync(settingsOverride: studySettings);
    scheduler.scheduled.clear();
    await coordinator.sync(settingsOverride: planSettings);

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
    expect(scheduler.cancelled, [ReminderCoordinator.studyId, ReminderCoordinator.planId, ReminderCoordinator.studyId]);
  });

  test('latest sync without an override uses the persisted settings', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final settings = ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true);
    await coordinator.sync(settingsOverride: settings);
    scheduler.scheduled.clear();
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId]);
  });
}
