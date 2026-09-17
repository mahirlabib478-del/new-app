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
  int _syncGeneration = 0;
  int _completedGeneration = 0;
  ReminderSettings? _pendingSettingsOverride;
  bool _pendingPersistedSettings = false;
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
    _syncGeneration++;
    if (settingsOverride != null) {
      _pendingSettingsOverride = settingsOverride;
      _pendingPersistedSettings = false;
    } else {
      // An override-less refresh explicitly supersedes an older queued override
      // and reads the latest persisted settings when its turn runs.
      _pendingSettingsOverride = null;
      _pendingPersistedSettings = true;
    }
    if (_syncing) return;

    _syncing = true;
    try {
      while (_completedGeneration < _syncGeneration) {
        final generation = _syncGeneration;
        final override = _pendingSettingsOverride;
        final usePersistedSettings = _pendingPersistedSettings || override == null;
        _pendingSettingsOverride = null;
        _pendingPersistedSettings = false;
        await _syncOnce(
          settingsOverride: usePersistedSettings ? null : override,
        );
        _completedGeneration = generation;
      }
    } finally {
      _syncing = false;
      _pendingSettingsOverride = null;
      _pendingPersistedSettings = false;
      _completedGeneration = _syncGeneration;
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

    if (!settings.breakEnabled) {
      // Break reminders are event-driven rather than daily-scheduled. Cancel the
      // fixed notification ID when disabled so a previously shown break reminder
      // cannot remain visible after the setting changes or an app restart.
      await _safeCancel(breakId);
    }

    final snapshot = TodayEngine(store).build();

    final studyRequest = policy.studyReminder(
      hasRemainingWork: snapshot.hasPlan && !snapshot.isComplete,
      goalRemainingMinutes: snapshot.goalRemainingMinutes,
      remainingWorkMinutes: snapshot.remainingMinutes,
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
      await scheduler.scheduleDailyReminder(
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
      final request = policy.breakReminder(focusSessionCompleted: true);
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
