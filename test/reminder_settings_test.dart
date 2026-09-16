import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/reminder_settings.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<ReminderSettingsStore> makeStore([Map<String, Object> values = const {}]) async {
    SharedPreferences.setMockInitialValues(values);
    return ReminderSettingsStore(await SharedPreferences.getInstance());
  }

  test('uses safe defaults for a new install', () async {
    final store = await makeStore();
    expect(store.settings.studyEnabled, isFalse);
    expect(store.settings.breakEnabled, isTrue);
    expect(store.settings.planEnabled, isTrue);
    expect(store.settings.studyHour, 19);
    expect(store.settings.planHour, 9);
  });

  test('settings persist and restore', () async {
    final store = await makeStore();
    final value = store.settings.copyWith(
      studyEnabled: true,
      breakEnabled: false,
      planEnabled: false,
      studyHour: 21,
      studyMinute: 30,
      planHour: 8,
      planMinute: 15,
    );
    await store.save(value);

    expect(store.settings.studyEnabled, isTrue);
    expect(store.settings.breakEnabled, isFalse);
    expect(store.settings.planEnabled, isFalse);
    expect(store.settings.studyHour, 21);
    expect(store.settings.studyMinute, 30);
    expect(store.settings.planHour, 8);
    expect(store.settings.planMinute, 15);
  });

  test('malformed persisted times are clamped safely', () async {
    final store = await makeStore({
      'reminder_study_hour': 99,
      'reminder_study_minute': -5,
      'reminder_plan_hour': 24,
      'reminder_plan_minute': 90,
    });
    expect(store.settings.studyHour, 23);
    expect(store.settings.studyMinute, 0);
    expect(store.settings.planHour, 23);
    expect(store.settings.planMinute, 59);
  });
}
