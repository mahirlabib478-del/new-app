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

/// Optional capability for schedulers that can report whether notifications
/// are currently allowed by the platform.
abstract interface class ReminderSchedulerNotificationAware {
  Future<bool?> areNotificationsEnabled();
}

/// Optional capability for schedulers that cache timezone state.
abstract interface class ReminderSchedulerTimeZoneAware {
  Future<void> refreshTimeZone();
  String? get timeZoneFingerprint;
}
