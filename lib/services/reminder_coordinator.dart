import 'local_store.dart';
import 'notification_service.dart';
import 'reminder_policy.dart';
import 'reminder_settings.dart';
import 'today_engine.dart';

abstract interface class ReminderScheduler {
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  });

  Future<void> showNow({required int id, required String title, required String body});

  Future<void> cancel(int id);

  Future<bool?> requestPermissions();
}

class ReminderCoordinator {
  const ReminderCoordinator({
    required this.store,
    required this.settingsStore,
    required this.scheduler,
    this.policy = const ReminderPolicy(),
  });

  static const studyId = 1001;
  static const breakId = 1002;
  static const planId = 1003;

  final LocalStore store;
  final ReminderSettingsStore settingsStore;
  final ReminderScheduler scheduler;
  final ReminderPolicy policy;

  Future<void> requestPermissions() async {
    try {
      await scheduler.requestPermissions();
    } on Exception {
      // Notification permission is best-effort; it must never block Study OS.
    }
  }

  Future<void> sync() async {
    final settings = settingsStore.settings;
    final snapshot = TodayEngine(store).build();

    final studyRequest = policy.studyReminder(
      hasRemainingWork: snapshot.hasPlan && !snapshot.isComplete,
      goalRemainingMinutes: snapshot.goalRemainingMinutes,
    );
    await _syncDaily(
      enabled: settings.studyEnabled,
      request: studyRequest,
      id: studyId,
      hour: settings.studyHour,
      minute: settings.studyMinute,
    );

    final planRequest = policy.planReminder(
      hasPlan: snapshot.hasPlan,
      hasRemainingWork: snapshot.hasPlan && !snapshot.isComplete,
    );
    await _syncDaily(
      enabled: settings.planEnabled,
      request: planRequest,
      id: planId,
      hour: settings.planHour,
      minute: settings.planMinute,
    );

    if (!settings.breakEnabled) await _safeCancel(breakId);
  }

  Future<void> notifyFocusBlockCompleted() async {
    if (!settingsStore.settings.breakEnabled) return;
    final request = policy.breakReminder(focusSessionCompleted: true);
    if (request == null) return;
    try {
      await scheduler.showNow(id: breakId, title: request.title, body: request.body);
    } on Exception {
      // A notification failure must not interrupt the study flow.
    }
  }

  Future<void> _syncDaily({
    required bool enabled,
    required ReminderRequest? request,
    required int id,
    required int hour,
    required int minute,
  }) async {
    if (!enabled || request == null) {
      await _safeCancel(id);
      return;
    }

    try {
      await scheduler.scheduleDailyReminder(
        id: id,
        title: request.title,
        body: request.body,
        hour: hour,
        minute: minute,
      );
    } on Exception {
      // Unsupported platforms and OS-level failures must not block the app.
    }
  }

  Future<void> _safeCancel(int id) async {
    try {
      await scheduler.cancel(id);
    } on Exception {
      // Best-effort cancellation keeps settings and study flow usable offline.
    }
  }
}
