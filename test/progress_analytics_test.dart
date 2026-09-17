import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/progress_analytics.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore([Map<String, Object> initial = const {}]) async {
    SharedPreferences.setMockInitialValues(initial);
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('analytics summarizes the requested window in chronological order', () async {
    final store = await makeStore();
    final today = DateTime.now();
    await store.addDailyStudyMinutes(30, date: today.subtract(const Duration(days: 2)));
    await store.addDailyStudyMinutes(20, date: today.subtract(const Duration(days: 1)));
    await store.addDailyStudyMinutes(40, date: today);

    final summary = ProgressAnalytics(store).build(today: today, days: 3);

    expect(summary.dailyMinutes, [30, 20, 40]);
    expect(summary.totalMinutes, 90);
    expect(summary.averageMinutes, 30);
    expect(summary.activeDays, 3);
    expect(summary.bestDayMinutes, 40);
  });

  test('analytics ignores history outside the requested window', () async {
    final store = await makeStore();
    final today = DateTime.now();
    await store.addDailyStudyMinutes(100, date: today.subtract(const Duration(days: 8)));
    await store.addDailyStudyMinutes(25, date: today.subtract(const Duration(days: 2)));

    final summary = ProgressAnalytics(store).build(today: today, days: 7);

    expect(summary.dailyMinutes.length, 7);
    expect(summary.totalMinutes, 25);
  });

  test('analytics clamps an invalid window to one through thirty days', () async {
    final store = await makeStore();
    final today = DateTime.now();
    await store.addDailyStudyMinutes(10, date: today);

    final summary = ProgressAnalytics(store).build(today: today, days: 99);
    expect(summary.days, 30);
    expect(summary.dailyMinutes.length, 30);

    final minimum = ProgressAnalytics(store).build(today: today, days: 0);
    expect(minimum.days, 1);
    expect(minimum.dailyMinutes.length, 1);
  });

  test('analytics uses item-level progress as authoritative plan progress', () async {
    final store = await makeStore();
    final today = DateTime.now();
    await store.savePlan(StudyPlan(totalMinutes: 60, items: [
      StudyItem(title: 'Physics', minutes: 30),
      StudyItem(title: 'Math', minutes: 30),
    ]));
    await store.addItemCompletedMinutes(0, 20);
    await store.addItemCompletedMinutes(1, 10);

    final summary = ProgressAnalytics(store).build(today: today, days: 7);

    expect(summary.plannedMinutes, 60);
    expect(summary.planCompletedMinutes, 30);
    expect(summary.planRemainingMinutes, 30);
    expect(summary.planCompletionRate, closeTo(0.5, 0.0001));
    // The completed minutes above are also recorded in today's study
    // history, so the seven-day calendar average is 30 / 7.
    expect(summary.estimatedPlanDaysRemaining, 7);
  });

  test('analytics estimates remaining plan days from the requested calendar average pace', () async {
    final store = await makeStore({'daily_goal_minutes': 60});
    final today = DateTime.now();
    await store.savePlan(StudyPlan(totalMinutes: 120, items: [
      StudyItem(title: 'Physics', minutes: 60),
      StudyItem(title: 'Math', minutes: 60),
    ]));
    await store.addItemCompletedMinutes(0, 20);
    await store.addDailyStudyMinutes(30, date: today.subtract(const Duration(days: 1)));
    await store.addDailyStudyMinutes(30, date: today);

    final summary = ProgressAnalytics(store).build(today: today, days: 2);

    // Today's completion contributes to study history as well, so the
    // calendar-window average is (30 + 50) / 2 = 40 minutes/day.
    expect(summary.averageMinutes, 40);
    expect(summary.planRemainingMinutes, 100);
    expect(summary.estimatedPlanDaysRemaining, 3);
  });
}
