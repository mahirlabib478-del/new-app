import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_policy.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

class FakeScheduler implements ReminderScheduler {
  final List<int> scheduled = <int>[];
  final List<int> cancelled = <int>[];
  int initializeCalls = 0;
  int requestPermissionCalls = 0;
  int showNowCalls = 0;
  Future<void> Function()? onInitialize;
  Exception? initializeError;
  Exception? requestPermissionError;
  Exception? scheduleError;
  Exception? showNowError;

  @override
  Future<void> initialize() async {
    initializeCalls++;
    if (initializeError != null) throw initializeError!;
    await onInitialize?.call();
  }

  @override
  Future<void> requestPermissions() async {
    requestPermissionCalls++;
    if (requestPermissionError != null) throw requestPermissionError!;
  }

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
  Future<void> showNow({
    required int id,
    required String title,
    required String body,
  }) async {
    showNowCalls++;
    if (showNowError != null) throw showNowError!;
  }

  @override
  Future<void> cancel(int id) async {
    cancelled.add(id);
  }
}

class FixedPolicy extends ReminderPolicy {
  const FixedPolicy();

  @override
  ReminderRequest? studyReminder({
    required bool hasRemainingWork,
    required int goalRemainingMinutes,
  }) => hasRemainingWork ? const ReminderRequest(kind: 'study', title: 'Study', body: 'Study now') : null;

  @override
  ReminderRequest? planReminder({
    required bool hasPlan,
    required bool hasRemainingWork,
  }) => !hasPlan ? const ReminderRequest(kind: 'plan', title: 'Plan', body: 'Plan now') : null;

  @override
  ReminderRequest? breakReminder({required bool focusSessionCompleted}) =>
      focusSessionCompleted ? const ReminderRequest(kind: 'break', title: 'Break', body: 'Take a break') : null;
}

void main() {
  test('default settings schedule only the plan reminder when no plan exists', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
      policy: const FixedPolicy(),
    );

    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('latest settings override schedules a newly enabled study reminder', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    final settingsStore = ReminderSettingsStore(prefs);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: settingsStore,
      scheduler: scheduler,
      policy: const FixedPolicy(),
    );

    await coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false),
    );

    expect(scheduler.scheduled, [ReminderCoordinator.studyId]);
  });

  test('focus completion resyncs daily reminders after the plan becomes complete', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
      policy: const FixedPolicy(),
    );

    await coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false),
    );
    await store.recordCompletedSession(25);
    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('study reminder is cancelled when the daily goal is reached before the plan is complete', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]));
    await store.setDailyGoalMinutes(25);
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
      policy: const FixedPolicy(),
    );

    await coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false),
    );
    await store.recordCompletedSession(25);
    await coordinator.sync();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
  });

  test('disabled break reminder prevents focus completion notification', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.showNowCalls, 0);
  });

  test('enabled break reminder shows a notification after focus completion', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final settingsStore = ReminderSettingsStore(prefs);
    await settingsStore.save(ReminderSettings.defaults.copyWith(breakEnabled: true));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: settingsStore,
      scheduler: scheduler,
    );

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.showNowCalls, 1);
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

    await coordinator.requestPermissions();

    expect(scheduler.initializeCalls, 1);
    expect(scheduler.requestPermissionCalls, 1);
  });

  test('permission request failure does not escape into the study flow', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..requestPermissionError = Exception('denied');
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
    final scheduler = FakeScheduler()..initializeError = Exception('unavailable');
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
    final settingsStore = ReminderSettingsStore(prefs);
    await settingsStore.save(ReminderSettings.defaults.copyWith(planEnabled: true));
    final scheduler = FakeScheduler()..scheduleError = Exception('temporary');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: settingsStore,
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
    final settingsStore = ReminderSettingsStore(prefs);
    await settingsStore.save(ReminderSettings.defaults.copyWith(breakEnabled: true, planEnabled: true));
    final scheduler = FakeScheduler()..showNowError = Exception('temporary');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: settingsStore,
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

  test('concurrent sync requests coalesce into one unchanged schedule', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final started = Completer<void>();
    final gate = Completer<void>();
    scheduler.onInitialize = () async {
      if (scheduler.initializeCalls == 1) {
        started.complete();
        await gate.future;
      }
    };
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final first = coordinator.sync();
    await started.future;
    final second = coordinator.sync();
    gate.complete();

    await Future.wait([first, second]);

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('latest concurrent settings override wins the coalesced refresh', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
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
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final first = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true),
    );
    await started.future;
    final second = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false),
    );
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

    final first = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(studyEnabled: false, planEnabled: true),
    );
    await started.future;
    await settingsStore.save(
      ReminderSettings.defaults.copyWith(studyEnabled: true, planEnabled: false),
    );
    final second = coordinator.sync();
    gate.complete();

    await Future.wait([first, second]);

    expect(scheduler.scheduled, [ReminderCoordinator.studyId]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });
}
