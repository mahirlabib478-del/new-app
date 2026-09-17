abstract interface class ReminderScheduler {
  Future<void> initialize() async {}

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

/// Optional capability for schedulers that cache timezone state.
abstract interface class ReminderSchedulerTimeZoneAware {
  Future<void> refreshTimeZone();
  String? get timeZoneFingerprint;
}

/// Refreshes timezone state when the scheduler supports the optional capability.
extension ReminderSchedulerTimeZoneRefresh on ReminderScheduler {
  Future<void> refreshTimeZone() async {
    final scheduler = this;
    if (scheduler is ReminderSchedulerTimeZoneAware) {
      await scheduler.refreshTimeZone();
    }
  }

  String? get timeZoneFingerprint {
    final scheduler = this;
    if (scheduler is ReminderSchedulerTimeZoneAware) {
      return scheduler.timeZoneFingerprint;
    }
    return null;
  }
}
