import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

class FakeScheduler implements ReminderScheduler, ReminderSchedulerFirstOccurrence {
  final scheduled = <int>[];
  final cancelled = <int>[];
  final shown = <int>[];
  final oneShot = <int>[];
  final firstOccurrences = <int, DateTime>{};
  var permissionRequests = 0;
  bool? permissionResult = true;
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
    return permissionResult;
  }

  @override
  Future<void> scheduleOnce({required int id, required String title, required String body, required DateTime at}) async {
    oneShot.add(id);
  }

  @override
  Future<void> scheduleDailyReminderAt({required int id, required String title, required String body, required DateTime firstAt}) async {
    if (scheduleError != null) throw scheduleError!;
    firstOccurrences[id] = firstAt;
    scheduled.add(id);
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
    if (showError != null) throw showError!;
    shown.add(id);
  }
}

Future<ReminderCoordinator> makeCoordinator({
  ReminderSettings? settings,
  StudyPlan? plan,
}) async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();
  final store = LocalStore(prefs);
  if (plan != null) await store.savePlan(plan);
  final settingsStore = ReminderSettingsStore(prefs);
  if (settings != null) await settingsStore.save(settings);
  return ReminderCoordinator(
    store: store,
    settingsStore: settingsStore,
    scheduler: FakeScheduler(),
  );
}

void main() {
  test('default settings schedule only the plan reminder when no plan exists', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.planId]);
  });

  test('study reminder schedules when a plan has remaining work', () async {
    final coordinator = await makeCoordinator(
      settings: ReminderSettings.defaults.copyWith(
        studyEnabled: true,
        breakEnabled: false,
        planEnabled: false,
      ),
      plan: StudyPlan(totalMinutes: 25, items: [
        StudyItem(title: 'Math', minutes: 25),
      ]),
    );
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId]);
  });

  test('denied permission is reported but does not block a later sync', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;
    scheduler.permissionResult = false;

    expect(await coordinator.requestPermissions(), isFalse);
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.planId]);
  });

  test('permission failures are contained', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;
    scheduler.permissionError = Exception('denied');

    await expectLater(coordinator.requestPermissions(), completes);
    expect(await coordinator.requestPermissions(), isFalse);
  });

  test('scheduler initialization failure is contained', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;
    scheduler.initializeError = Exception('unsupported');

    await expectLater(coordinator.sync(), completes);
  });

  test('schedule failure is retried on the next sync', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;
    scheduler.scheduleError = Exception('failed');

    await coordinator.sync();
    scheduler.scheduleError = null;
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.planId]);
  });

  test('focus completion schedules a one-shot background notification', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;

    final at = DateTime.now().add(const Duration(minutes: 25));
    await coordinator.scheduleFocusBlockCompletion(at);

    expect(scheduler.oneShot, [ReminderCoordinator.breakId]);
  });

  test('focus completion alarm can be cancelled', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.cancelFocusBlockCompletion();

    expect(scheduler.cancelled, [ReminderCoordinator.breakId]);
  });

  test('focus completion notification failure does not break daily sync', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;
    scheduler.showError = Exception('failed');

    await expectLater(coordinator.notifyFocusBlockCompleted(), completes);

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.planId]);
  });

  test('disabled break reminder cancels its notification', () async {
    final coordinator = await makeCoordinator(
      settings: ReminderSettings.defaults.copyWith(breakEnabled: false),
    );
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.sync();

    expect(scheduler.cancelled, contains(ReminderCoordinator.breakId));
  });

  test('enabled break reminder shows after focus completion', () async {
    final coordinator = await makeCoordinator(
      settings: ReminderSettings.defaults.copyWith(
        breakEnabled: true,
        planEnabled: false,
      ),
    );
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.notifyFocusBlockCompleted();

    expect(scheduler.shown, [ReminderCoordinator.breakId]);
  });

  test('each sync replaces the platform registration instead of using stale memory', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.sync();
    await coordinator.sync();

    expect(scheduler.scheduled, [ReminderCoordinator.studyId, ReminderCoordinator.planId, ReminderCoordinator.studyId, ReminderCoordinator.planId]);
    expect(
      scheduler.cancelled.where((id) => id == ReminderCoordinator.planId).length,
      2,
    );
  });

  test('latest concurrent settings override wins', () async {
    final coordinator = await makeCoordinator();
    final scheduler = coordinator.scheduler as FakeScheduler;
    final gate = Completer<void>();
    final started = Completer<void>();

    scheduler.onInitialize = () async {
      if (scheduler.initializeCalls == 1) {
        started.complete();
        await gate.future;
      }
    };

    final first = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(
        studyEnabled: false,
        planEnabled: true,
      ),
    );
    await started.future;

    final second = coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(
        studyEnabled: false,
        planEnabled: false,
      ),
    );
    gate.complete();

    await Future.wait([first, second]);

    expect(scheduler.scheduled, [ReminderCoordinator.planId]);
    expect(scheduler.cancelled, contains(ReminderCoordinator.planId));
  });

  test('changed reminder time is replaced on sync', () async {
    final coordinator = await makeCoordinator(
      settings: ReminderSettings.defaults.copyWith(
        studyEnabled: true,
        breakEnabled: false,
        planEnabled: false,
      ),
      plan: StudyPlan(totalMinutes: 25, items: [
        StudyItem(title: 'Math', minutes: 25),
      ]),
    );
    final scheduler = coordinator.scheduler as FakeScheduler;

    await coordinator.sync();
    await coordinator.sync(
      settingsOverride: ReminderSettings.defaults.copyWith(
        studyEnabled: true,
        breakEnabled: false,
        planEnabled: false,
        studyHour: 20,
        studyMinute: 15,
      ),
    );

    expect(scheduler.scheduled, [
      ReminderCoordinator.studyId,
      ReminderCoordinator.studyId,
    ]);
    expect(
      scheduler.cancelled.where((id) => id == ReminderCoordinator.studyId).length,
      2,
    );
  });

  test('completed plan removes the study reminder and restores plan reminder', () async {
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Math', minutes: 25)],
    ));
    final scheduler = FakeScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await ReminderSettingsStore(prefs).save(
      ReminderSettings.defaults.copyWith(
        studyEnabled: true,
        breakEnabled: false,
        planEnabled: true,
      ),
    );

    await coordinator.sync();
    await store.addItemCompletedMinutes(0, 25);
    await coordinator.sync();

    expect(scheduler.scheduled, contains(ReminderCoordinator.studyId));
    expect(scheduler.scheduled, contains(ReminderCoordinator.planId));
  });
}
