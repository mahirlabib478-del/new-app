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
  ReminderSettings? _pendingSettingsOverride;
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
    _pendingSettingsOverride = settingsOverride;
    if (_syncing) return;

    _syncing = true;
    try {
      do {
        _syncRequested = false;
        final override = _pendingSettingsOverride;
        _pendingSettingsOverride = null;
        await _syncOnce(settingsOverride: override);
      } while (_syncRequested);
    } finally {
      _syncing = false;
      _pendingSettingsOverride = null;
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

    final breakRequest = policy.breakReminder(
      hasCompletedFocus: snapshot.hasCompletedFocusToday,
    );
    await _syncDaily(
      enabled: settings.breakEnabled,
      request: breakRequest,
      id: breakId,
      hour: settings.breakHour,
      minute: settings.breakMinute,
    );
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

    final fingerprint = '$id|${request.kind}|${request.title}|${request.body}|$hour|$minute';
    if (_scheduledFingerprints[id] == fingerprint) return;

    try {
      await _safeCancel(id);
      await scheduler.scheduleDaily(
        id: id,
        title: request.title,
        body: request.body,
        hour: hour,
        minute: minute,
      );
      _scheduledFingerprints[id] = fingerprint;
    } on Exception {
      // A transient scheduling failure should not break the study flow.
    }
  }

  Future<void> notifyFocusBlockCompleted() async {
    final settings = settingsStore.settings;
    if (settings.breakEnabled) {
      final snapshot = TodayEngine(store).build();
      final request = policy.breakReminder(
        hasCompletedFocus: snapshot.hasCompletedFocusToday,
      );
      if (request != null) {
        try {
          await scheduler.initialize();
          await scheduler.showNow(
            id: breakId,
            title: request.title,
            body: request.body,
          );
        } on Exception {
          // Focus completion remains successful if notifications fail.
        }
      }
    }
    await sync();
  }

  Future<void> _safeCancel(int id) async {
    try {
      await scheduler.cancel(id);
    } on Exception {
      // Cancellation is best-effort.
    }
  }
}
