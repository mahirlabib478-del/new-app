import 'local_store.dart';
import 'reminder_policy.dart';
import 'reminder_scheduler.dart';
import 'reminder_settings.dart';
import 'today_engine.dart';

class ReminderCoordinator {
  ReminderCoordinator({
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

  bool _syncing = false;
  bool _syncRequested = false;
  final Map<int, String> _scheduledFingerprints = <int, String>{};

  Future<void> requestPermissions() async {
    try {
      await scheduler.initialize();
      await scheduler.requestPermissions();
    } on Exception {
      // Notification permission is best-effort; it must never block Study OS.
    }
  }

  Future<void> sync({ReminderSettings? settingsOverride}) async {
    _syncRequested = true;
    if (_syncing) return;

    _syncing = true;
    try {
      do {
        _syncRequested = false;
        await _syncOnce(settingsOverride: settingsOverride);
      } while (_syncRequested);
    } finally {
      _syncing = false;
    }
  }

  Future<void> _syncOnce({ReminderSettings? settingsOverride}) async {
    final settings = settingsOverride ?? settingsStore.settings;
    try {
      await scheduler.initialize();
    } on Exception {
      // Unsupported platforms must still be able to use the study app.
      return;
    }

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
    final settings = settingsStore.settings;
    if (settings.breakEnabled) {
      final request = policy.breakReminder(focusSessionCompleted: true);
      if (request != null) {
        try {
          await scheduler.initialize();
          await scheduler.showNow(id: breakId, title: request.title, body: request.body);
        } on Exception {
          // A notification failure must not interrupt the study flow.
        }
      }
    }

    // Focus completion changes Today Engine state, so refresh daily reminders
    // immediately instead of waiting for the next settings change.
    await sync();
  }

  Future<void> _syncDaily({
    required bool enabled,
    required ReminderRequest? request,
    required int id,
    required int hour,
    required int minute,
  }) async {
    if (!enabled || request == null) {
      _scheduledFingerprints.remove(id);
      await _safeCancel(id);
      return;
    }

    final fingerprint = '$id|${request.kind.name}|${request.title}|${request.body}|$hour|$minute';
    if (_scheduledFingerprints[id] == fingerprint) return;

    try {
      // Make refreshes explicitly idempotent even if a scheduler backend
      // changes its replacement semantics for an existing notification ID.
      await scheduler.cancel(id);
      await scheduler.scheduleDailyReminder(
        id: id,
        title: request.title,
        body: request.body,
        hour: hour,
        minute: minute,
      );
      _scheduledFingerprints[id] = fingerprint;
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
