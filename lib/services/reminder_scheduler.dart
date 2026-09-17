abstract interface class ReminderScheduler {
  Future<void> initialize() async {}

  /// Refreshes the scheduler's local timezone from the device.
  ///
  /// Implementations that do not cache timezone state may leave this as a no-op.
  Future<void> refreshTimeZone() async {}

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
