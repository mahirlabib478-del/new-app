import 'local_store.dart';
import 'reminder_policy.dart';
import 'reminder_scheduler.dart';
import 'reminder_settings.dart';
import 'today_engine.dart';

/// Owns reminder policy and synchronization.
///
/// There is deliberately no notification-permission state cache here.
/// Android owns the actual permission state and the notification plugin owns
/// the platform interaction. A sync either registers/cancels the requested
/// notifications or safely leaves the rest of the app untouched.
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
  int _requestedGeneration = 0;
  int _completedGeneration = 0;
  ReminderSettings? _latestOverride;
  bool _latestUsesPersistedSettings = true;

  Future<bool> areNotificationsEnabled() async {
    try {
      await scheduler.initialize();
      return await scheduler.areNotificationsEnabled();
    } on Exception {
      return false;
    }
  }

  Future<bool> requestPermissions() async {
    try {
      await scheduler.initialize();
      return await scheduler.requestPermissions() ?? true;
    } on Exception {
      return false;
    }
  }

  Future<void> sync({ReminderSettings? settingsOverride}) async {
    _requestedGeneration++;
    if (settingsOverride != null) {
      _latestOverride = settingsOverride;
      _latestUsesPersistedSettings = false;
    } else {
      _latestOverride = null;
      _latestUsesPersistedSettings = true;
    }

    if (_syncing) return;

    _syncing = true;
    try {
      while (_completedGeneration < _requestedGeneration) {
        final generation = _requestedGeneration;
        final settings = _latestUsesPersistedSettings
            ? settingsStore.settings
            : (_latestOverride ?? settingsStore.settings);

        _latestOverride = null;
        _latestUsesPersistedSettings = true;

        await _syncOnce(settings);
        _completedGeneration = generation;
      }
    } finally {
      _syncing = false;
      _latestOverride = null;
      _latestUsesPersistedSettings = true;
      _completedGeneration = _requestedGeneration;
    }
  }

  Future<void> _syncOnce(ReminderSettings settings) async {
    try {
      await scheduler.initialize();
      final timeZoneAware = scheduler is ReminderSchedulerTimeZoneAware
          ? scheduler as ReminderSchedulerTimeZoneAware
          : null;
      await timeZoneAware?.refreshTimeZone();
    } on Exception {
      return;
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

    if (!settings.breakEnabled) {
      await _safeCancel(breakId);
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

    // Always replace the platform registration. This removes stale alarms
    // after a time, plan, goal or message change and keeps the source of truth
    // in the persisted app state rather than an in-memory fingerprint.
    try {
      await scheduler.cancel(id);
    } on Exception {
      // Continue: scheduling the new request is still preferable to dropping it.
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
      // Notification failure must never break study flows. The next sync
      // retries because no in-memory success state is recorded.
    }
  }

  Future<void> scheduleFocusBlockCompletion(DateTime at) async {
    try {
      await scheduler.scheduleOnce(
        id: breakId,
        title: 'Take a break',
        body: 'Your focus block is complete. Take a short break before the next block.',
        at: at,
      );
    } on Exception {
      // The foreground completion path still attempts an immediate notification.
    }
  }

  Future<void> cancelFocusBlockCompletion() async {
    await _safeCancel(breakId);
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
          // Focus completion remains successful if notification delivery fails.
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
