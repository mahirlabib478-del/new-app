import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

class FakeScheduler implements ReminderScheduler, ReminderSchedulerNotificationAware {
  final scheduled = <int>[];
  final cancelled = <int>[];
  final shown = <int>[];
  var permissionRequests = 0;
  bool notificationsEnabled = true;
  var initializeCalls = 0;
  Exception? initializeError;
  Exception? permissionError;
  Exception? scheduleError;
  Exception? cancelError;
  Exception? showError;
  Future<void> Function()? onInitialize;

  @override
  Future<void> initialize() async {
    initializeCalls++;
    if (onInitialize != null) await onInitialize!();
    if (initializeError != null) throw initializeError!;
  }

  @override
  Future<void> cancel(int id) async {
    cancelled.add(id);
    if (cancelError != null) throw cancelError!;
  }

  @override
  Future<bool?> requestPermissions() async {
    permissionRequests++;
    if (permissionError != null) throw permissionError!;
    return notificationsEnabled;
  }

  @override
  Future<bool?> areNotificationsEnabled() async => notificationsEnabled;

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {
    if (scheduleError != null) throw scheduleError!;
    scheduled.add(id);
  }

  @override
  Future<void> showNow({required int id, required String title, required String body}) async {
    if (showError != null) throw showError!;
    shown.add(id);
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
    SharedPreferences.setMockInitialValues({'reminder_study_enabled': false});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync(settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: true));

    expect(scheduler.scheduled, [ReminderCoordinator.studyId]);
  });

  test('focus completion resyncs daily reminders after the plan becomes complete', () async {
    SharedPreferences.setMockInitialValues({'reminder_study_enabled': true});
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
    await store.addItemCompletedMinutes(0, 25);
    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('study reminder is cancelled when the daily goal is reached before the plan is complete', () async {
    SharedPreferences.setMockInitialValues({'reminder_study_enabled': true});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]));
    await store.setDailyGoalMinutes(25);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    await store.addItemCompletedMinutes(0, 25);
    await coordinator.sync();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('disabled break reminder prevents focus completion notification', () async {
    SharedPreferences.setMockInitialValues({'reminder_break_enabled': false});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.shown, isEmpty);
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

    expect(await coordinator.requestPermissions(), isTrue);

    expect(scheduler.permissionRequests, 1);
  });

  test('denied notification permission is reported and prevents scheduling', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..notificationsEnabled = false;
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    expect(await coordinator.requestPermissions(), isFalse);
    await coordinator.sync();

    expect(scheduler.scheduled, isEmpty);
  });

  test('permission request failure does not escape into the study flow', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..permissionError = Exception('permission denied');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await expectLater(coordinator.requestPermissions(), completes);
  });

  test('scheduler initialization failure leaves the study flow usable', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..initializeError = Exception('unsupported');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await expectLater(coordinator.sync(), completes);
  });

  test('daily schedule failure is swallowed and retried on the next sync', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..scheduleError = Exception('schedule failed');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await expectLater(coordinator.sync(), completes);
    scheduler.scheduleError = null;
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('focus notification failure still refreshes daily reminders', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..showError = Exception('show failed');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await expectLater(coordinator.notifyFocusBlockCompleted(), completes);
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
    final settings = ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true);
    await ReminderSettingsStore(prefs).save(settings);

    await coordinator.sync(settingsOverride: settings);
    await coordinator.sync(settingsOverride: settings);

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
    expect(scheduler.cancelled, [ReminderCoordinator.studyId, ReminderCoordinator.planId, ReminderCoordinator.studyId]);
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
    final store = LocalStore(prefs);
    final gate = Completer<void>();
    final started = Completer<void>();
    final scheduler = FakeScheduler();
    scheduler.onInitialize = () async {
      if (scheduler.initializeCalls == 1) {
        started.complete();
        await gate.future;
      } else if (scheduler.initializeCalls == 2) {
        await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
      }
    };
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final first = coordinator.sync(settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true));
    await started.future;
    final second = coordinator.sync(settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false));
    gate.complete();

    await Future.wait([first, second]);

    expect(scheduler.scheduled, [ReminderCoordinator.planId, ReminderCoordinator.studyId]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });

  test('latest sync without an override uses the persisted settings', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final settingsStore = ReminderSettingsStore(prefs);
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final gate = Completer<void>();
    final started = Completer<void>();
    final scheduler = FakeScheduler();
    scheduler.onInitialize = () async {
      if (scheduler.initializeCalls == 1) {
        started.complete();
        await gate.future;
      }
    };
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: settingsStore,
      scheduler: scheduler,
    );

    final first = coordinator.sync(settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true));
    await started.future;
    await settingsStore.save(ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false));
    final second = coordinator.sync();
    gate.complete();

    await Future.wait([first, second]);

    expect(scheduler.scheduled, [ReminderCoordinator.studyId]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });

  test('changed daily reminder time cancels and reschedules the reminder', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final settingsStore = ReminderSettingsStore(prefs);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: settingsStore,
      scheduler: scheduler,
    );

    await coordinator.sync();
    await settingsStore.save(ReminderSettings.defaults.copyWith(
      studyEnabled: true,
      breakEnabled: false,
      planEnabled: false,
      studyHour: 20,
      studyMinute: 15,
    ));
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.studyId]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('disabling and re-enabling a daily reminder restores its schedule', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final settingsStore = ReminderSettingsStore(prefs);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: settingsStore,
      scheduler: scheduler,
    );

    await coordinator.sync();
    await settingsStore.save(ReminderSettings.defaults.copyWith(
      studyEnabled: false,
      breakEnabled: false,
      planEnabled: false,
    ));
    await coordinator.sync();
    await settingsStore.save(ReminderSettings.defaults.copyWith(
      studyEnabled: true,
      breakEnabled: false,
      planEnabled: false,
    ));
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.studyId]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('reschedule failure is retried without marking the new schedule complete', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final settingsStore = ReminderSettingsStore(prefs);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: settingsStore,
      scheduler: scheduler,
    );

    await coordinator.sync();
    await settingsStore.save(ReminderSettings.defaults.copyWith(
      studyEnabled: true,
      breakEnabled: false,
      planEnabled: false,
      studyHour: 20,
      studyMinute: 15,
    ));
    scheduler.scheduleError = Exception('reschedule failed');
    await coordinator.sync();
    scheduler.scheduleError = null;
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.studyId]);
    expect(scheduler.cancelled.where((id) => id == ReminderCoordinator.studyId).length, 3);
  });
}
