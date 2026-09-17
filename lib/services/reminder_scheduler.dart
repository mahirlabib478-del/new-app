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

/// Optional timezone refresh capability for schedulers that cache timezone state.
///
/// The capability is resolved dynamically so existing ReminderScheduler test
/// doubles and third-party implementations remain source-compatible.
extension ReminderSchedulerTimeZoneRefresh on ReminderScheduler {
  Future<void> refreshTimeZone() async {
    try {
      await (this as dynamic).refreshTimeZone();
    } on NoSuchMethodError {
      // Schedulers without timezone state do not need to do anything.
    }
  }
}
