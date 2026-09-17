import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
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
    expect(summary.consistencyRate, closeTo(1, 0.0001));
    expect(summary.goalCompletionRate, closeTo(170 / 180, 0.0001));
    expect(summary.currentStreak, 3);
    expect(summary.bestStreak, 3);
  });

  test('analytics ignores history outside the requested window', () async {
    final store = await makeStore();
    final today = DateTime(2026, 9, 16);
    await store.addDailyStudyMinutes(100, date: today.subtract(const Duration(days: 8)));
    await store.addDailyStudyMinutes(25, date: today.subtract(const Duration(days: 6)));

    final summary = ProgressAnalytics(store).build(today: today, days: 7);

    expect(summary.dailyMinutes, [25, 0, 0, 0, 0, 0, 0]);
    expect(summary.totalMinutes, 25);
    expect(summary.activeDays, 1);
    expect(summary.consistencyRate, closeTo(1 / 7, 0.0001));
    expect(summary.currentStreak, 0);
    expect(summary.bestStreak, 1);
  });

  test('analytics clamps an invalid window to one through thirty days', () async {
    final store = await makeStore();
    final summary = ProgressAnalytics(store).build(days: 100);
    expect(summary.days, 30);
    expect(summary.dailyMinutes.length, 30);
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
    expect(summary.estimatedPlanDaysRemaining, 1);
  });

  test('analytics estimates remaining plan days from recent average pace', () async {
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

    expect(summary.averageMinutes, 30);
    expect(summary.planRemainingMinutes, 100);
    expect(summary.estimatedPlanDaysRemaining, 4);
  });
}
