import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/progress_analytics.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore([Map<String, Object>? values]) async {
    SharedPreferences.setMockInitialValues(values ?? {});
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('analytics summarizes the requested window in chronological order', () async {
    final store = await makeStore({'daily_goal_minutes': 60});
    final today = DateTime(2026, 9, 16);
    await store.addDailyStudyMinutes(20, date: today.subtract(const Duration(days: 2)));
    await store.addDailyStudyMinutes(60, date: today.subtract(const Duration(days: 1)));
    await store.addDailyStudyMinutes(90, date: today);

    final summary = ProgressAnalytics(store).build(today: today, days: 3);

    expect(summary.dailyMinutes, [20, 60, 90]);
    expect(summary.totalMinutes, 170);
    expect(summary.averageMinutes, closeTo(170 / 3, 0.0001));
    expect(summary.activeDays, 3);
    expect(summary.bestDayMinutes, 90);
    expect(summary.goalHitDays, 2);
  });

  test('analytics ignores history outside the requested window', () async {
    final store = await makeStore();
    final today = DateTime(2026, 9, 16);
    await store.addDailyStudyMinutes(100, date: today.subtract(const Duration(days: 8)));
    await store.addDailyStudyMinutes(25, date: today.subtract(const Duration(days: 6)));

    final summary = ProgressAnalytics(store).build(today: today, days: 7);

    expect(summary.dailyMinutes, [0, 0, 0, 0, 0, 25, 0]);
    expect(summary.totalMinutes, 25);
    expect(summary.activeDays, 1);
  });

  test('analytics clamps an invalid window to one through thirty days', () async {
    final store = await makeStore();
    final summary = ProgressAnalytics(store).build(days: 100);
    expect(summary.days, 30);
    expect(summary.dailyMinutes.length, 30);
  });
}
