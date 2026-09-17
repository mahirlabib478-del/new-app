import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

class FakeScheduler implements ReminderScheduler {
  final scheduled = <int>[];
  final cancelled = <int>[];
  final shown = <int>[];
  var permissionRequests = 0;
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
    return true;
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
    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
    expect(scheduler.initializeCalls, 1);
  });

  test('latest settings override schedules a newly enabled study reminder', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final next = ReminderSettings.defaults.copyWith(studyEnabled: true);
    await coordinator.sync(settingsOverride: next);

    expect(scheduler.scheduled, contains(ReminderCoordinator.studyId));
  });

  test('focus completion resyncs daily reminders after the plan becomes complete', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
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
    expect(scheduler.scheduled, contains(ReminderCoordinator.studyId));

    await store.addItemCompletedMinutes(0, 25);
    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
    expect(scheduler.shown, contains(ReminderCoordinator.breakId));
  });

  test('study reminder is cancelled when the daily goal is reached before the plan is complete', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_plan_enabled': true,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.setDailyGoalMinutes(25);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 25),
    ]));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
    expect(scheduler.scheduled, contains(ReminderCoordinator.studyId));
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));

    await store.addItemCompletedMinutes(0, 25);
    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.cancelled, contains(ReminderCoordinator.studyId));
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });

  test('disabled break reminder prevents focus completion notification', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_break_enabled': false,
    });
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

  test('enabled break reminder shows a notification after focus completion', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_break_enabled': true,
    });
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.shown, [ReminderCoordinator.breakId]);
    expect(scheduler.initializeCalls, 2);
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

    expect(scheduler.permissionRequests, 1);
    expect(scheduler.initializeCalls, 1);
  });

  test('permission request failure does not escape into the study flow', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..permissionError = Exception('denied');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.requestPermissions();

    expect(scheduler.permissionRequests, 1);
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

    await coordinator.sync();

    expect(scheduler.scheduled, isEmpty);
  });

  test('daily schedule failure is swallowed and retried on the next sync', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scheduler = FakeScheduler()..scheduleError = Exception('os failure');
    final coordinator = ReminderCoordinator(
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();
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

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
    expect(scheduler.shown, isEmpty);
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
    expect(scheduler.cancelled, [
      ReminderCoordinator.studyId,
      ReminderCoordinator.planId,
      ReminderCoordinator.studyId,
    ]);
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

    await Future.wait([
      coordinator.sync(),
      coordinator.sync(),
    ]);

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
  });

  test('latest concurrent settings override wins the coalesced refresh', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
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
      store: LocalStore(prefs),
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    final first = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(
        studyEnabled: false,
        planEnabled: true,
      ),
    );
    await started.future;
    final second = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(
        studyEnabled: true,
        planEnabled: false,
      ),
    );
    gate.complete();

    await Future.wait([first, second]);

    expect(scheduler.scheduled, [
      ReminderCoordinator.planId,
      ReminderCoordinator.studyId,
    ]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });
}
