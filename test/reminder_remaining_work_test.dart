import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/reminder_coordinator.dart';
import 'package:study_os/services/reminder_scheduler.dart';
import 'package:study_os/services/reminder_settings.dart';

class CapturingScheduler implements ReminderScheduler {
  String? scheduledBody;

  @override
  Future<void> initialize() async {}

  @override
  Future<void> cancel(int id) async {}

  @override
  Future<bool> areNotificationsEnabled() async => true;

  @override
  Future<bool?> requestPermissions() async => true;

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {
    scheduledBody = body;
  }

  @override
  Future<void> showNow({required int id, required String title, required String body}) async {}
}

void main() {
  test('study reminder uses the actual remaining plan work in its message', () async {
    SharedPreferences.setMockInitialValues({
      'reminder_study_enabled': true,
      'reminder_break_enabled': false,
      'reminder_plan_enabled': false,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(
      StudyPlan(
        totalMinutes: 50,
        items: [StudyItem(title: 'Math', minutes: 50)],
      ),
    );
    await store.setDailyGoalMinutes(120);
    await store.addItemCompletedMinutes(0, 10);

    final scheduler = CapturingScheduler();
    final coordinator = ReminderCoordinator(
      store: store,
      settingsStore: ReminderSettingsStore(prefs),
      scheduler: scheduler,
    );

    await coordinator.sync();

    expect(
      scheduler.scheduledBody,
      'You have 40 minutes of study work remaining. Start a focused block.',
    );
  });
}
