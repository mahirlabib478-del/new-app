import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/today_engine.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore(Map<String, Object> values) async {
    SharedPreferences.setMockInitialValues(values);
    return LocalStore(await SharedPreferences.getInstance());
  }

  StudyPlan plan([List<StudyItem>? items]) => StudyPlan(
        totalMinutes: (items ?? [StudyItem(title: 'Math', minutes: 50)])
            .fold<int>(0, (sum, item) => sum + item.minutes),
        items: items ?? [StudyItem(title: 'Math', minutes: 50)],
      );

  test('Today Engine resumes the first unfinished item', () async {
    final p = plan([
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 50),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentBlockIndex, 0);
    expect(snapshot.currentItemCompletedMinutes, 0);
  });

  test('Today Engine resumes a partially completed item at its next block', () async {
    final p = plan([StudyItem(title: 'Physics', minutes: 50)]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 0);
    expect(snapshot.currentBlockIndex, 1);
    expect(snapshot.currentItemCompletedMinutes, 25);
  });

  test('Today Engine skips completed items and keeps their progress out of next action', () async {
    final p = plan([
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'0': 25, '1': 0}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.completedMinutes, 25);
    expect(snapshot.remainingMinutes, 25);
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
  });

  test('Today Engine skips zero-minute items without shifting the real index', () async {
    final p = plan([
      StudyItem(title: 'Placeholder', minutes: 0),
      StudyItem(title: 'Physics', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
  });

  test('Today Engine reports the correct block after an exact 25-minute boundary', () async {
    final p = plan([StudyItem(title: 'Physics', minutes: 50)]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.currentBlockIndex, 1);
  });

  test('Today Engine exposes no next item after the whole plan is complete', () async {
    final p = plan([StudyItem(title: 'Physics', minutes: 25)]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem, isNull);
    expect(snapshot.remainingMinutes, 0);
  });

  test('Today Engine prefers item progress when aggregate progress is stale', () async {
    final p = plan([
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 50,
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.completedMinutes, 25);
    expect(snapshot.nextItem?.title, 'Physics');
  });

  test('Today Engine recognizes a completed item map even when aggregate is zero', () async {
    final p = plan([StudyItem(title: 'Physics', minutes: 25)]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 0,
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.completedMinutes, 25);
    expect(snapshot.remainingMinutes, 0);
  });

  test('Today Engine resumes from aggregate-only legacy progress', () async {
    final p = plan([
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 50),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 25,
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentItemCompletedMinutes, 0);
  });

  test('Today Engine skips zero-minute items for aggregate-only progress', () async {
    final p = plan([
      StudyItem(title: 'Placeholder', minutes: 0),
      StudyItem(title: 'Physics', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 0,
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
  });

  test('Today Engine exposes daily goal progress from study history', () async {
    final store = await makeStore({'daily_goal_minutes': 120});
    await store.addDailyStudyMinutes(30);

    final snapshot = TodayEngine(store).build();
    expect(snapshot.todayCompletedMinutes, 30);
    expect(snapshot.goalRemainingMinutes, 90);
    expect(snapshot.goalProgress, closeTo(0.25, 0.0001));
  });

  test('Today Engine caps daily goal progress and reports when goal is reached', () async {
    final store = await makeStore({'daily_goal_minutes': 60});
    await store.addDailyStudyMinutes(90);

    final snapshot = TodayEngine(store).build();
    expect(snapshot.todayCompletedMinutes, 90);
    expect(snapshot.goalRemainingMinutes, 0);
    expect(snapshot.goalProgress, 1.0);
    expect(snapshot.dailyGoalReached, isTrue);
  });

  test('Today Engine recommends one focus block when both plan and goal remain', () async {
    final p = plan([StudyItem(title: 'Math', minutes: 50)]);
    final store = await makeStore({'study_plan': jsonEncode(p.toJson()), 'study_plan_date': _todayKey()});

    final snapshot = TodayEngine(store).build();
    expect(snapshot.recommendedFocusMinutes, 25);
    expect(snapshot.recommendationReason, 'Continue the first unfinished study item.');
  });

  test('Today Engine explains when a short item can be finished in one block', () async {
    final p = plan([StudyItem(title: 'Math', minutes: 15)]);
    final store = await makeStore({'study_plan': jsonEncode(p.toJson()), 'study_plan_date': _todayKey()});

    final snapshot = TodayEngine(store).build();
    expect(snapshot.recommendedFocusMinutes, 15);
    expect(snapshot.recommendationReason, 'Finish this remaining study item.');
  });

  test('Today Engine limits recommendation to remaining daily goal when it is smaller', () async {
    final p = plan([StudyItem(title: 'Math', minutes: 50)]);
    final store = await makeStore({'study_plan': jsonEncode(p.toJson()), 'study_plan_date': _todayKey(), 'daily_goal_minutes': 60});
    await store.addDailyStudyMinutes(45);

    final snapshot = TodayEngine(store).build();
    expect(snapshot.recommendedFocusMinutes, 15);
    expect(snapshot.recommendationReason, 'Use this block to move toward your daily goal.');
  });

  test('Today Engine recommends zero minutes after the plan is complete', () async {
    final p = plan([StudyItem(title: 'Math', minutes: 25)]);
    final store = await makeStore({'study_plan': jsonEncode(p.toJson()), 'study_plan_date': _todayKey(), 'item_completed_minutes': jsonEncode({'0': 25})});

    final snapshot = TodayEngine(store).build();
    expect(snapshot.recommendedFocusMinutes, 0);
    expect(snapshot.recommendationReason, 'Today’s plan is complete.');
  });

  test('Today Engine summarizes remaining items and focus blocks', () async {
    final p = plan([
      StudyItem(title: 'Math', minutes: 30),
      StudyItem(title: 'Physics', minutes: 25),
      StudyItem(title: 'Chemistry', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'0': 25, '1': 5}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.remainingItemCount, 3);
    expect(snapshot.remainingMinutes, 50);
    expect(snapshot.estimatedFocusBlocksRemaining, 2);
  });

  test('Today Engine counts only real unfinished items', () async {
    final p = plan([
      StudyItem(title: 'Placeholder', minutes: 0),
      StudyItem(title: 'Math', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'item_completed_minutes': jsonEncode({'1': 25}),
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.remainingItemCount, 0);
    expect(snapshot.estimatedFocusBlocksRemaining, 0);
  });

  test('Today Engine summarizes legacy aggregate progress across remaining items', () async {
    final p = plan([
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 50),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(p.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 35,
    });

    final snapshot = TodayEngine(store).build();
    expect(snapshot.remainingItemCount, 1);
    expect(snapshot.remainingMinutes, 40);
    expect(snapshot.estimatedFocusBlocksRemaining, 2);
  });
}

String _todayKey() {
  final now = DateTime.now();
  return '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
}
