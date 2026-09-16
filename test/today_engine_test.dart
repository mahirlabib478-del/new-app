import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/today_engine.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore([Map<String, Object>? values]) async {
    SharedPreferences.setMockInitialValues(values ?? {});
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('Today Engine resumes the first unfinished item', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 75, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 50),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentBlockIndex, 0);
    expect(snapshot.currentItemCompletedMinutes, 0);
    expect(snapshot.remainingMinutes, 50);
  });

  test('Today Engine resumes a partially completed item at its next block', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 80, items: [StudyItem(title: 'Chemistry', topic: 'Organic', minutes: 80)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 35);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Chemistry');
    expect(snapshot.currentIndex, 0);
    expect(snapshot.currentBlockIndex, 1);
    expect(snapshot.currentItemCompletedMinutes, 35);
    expect(snapshot.completedMinutes, 35);
    expect(snapshot.remainingMinutes, 45);
  });

  test('Today Engine skips completed items and keeps their progress out of next action', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 75, items: [
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 50),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(1, 10);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentBlockIndex, 0);
    expect(snapshot.currentItemCompletedMinutes, 10);
    expect(snapshot.completedMinutes, 35);
    expect(snapshot.remainingMinutes, 40);
  });

  test('Today Engine skips zero-minute items without shifting the real index', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 25, items: [
      StudyItem(title: 'Placeholder', minutes: 0),
      StudyItem(title: 'Math', minutes: 25),
    ]);
    await store.savePlan(plan);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Math');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentBlockIndex, 0);
    expect(snapshot.currentItemCompletedMinutes, 0);
    expect(snapshot.remainingMinutes, 25);
  });

  test('Today Engine reports the correct block after an exact 25-minute boundary', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Physics', minutes: 50)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.currentIndex, 0);
    expect(snapshot.currentBlockIndex, 1);
    expect(snapshot.currentItemCompletedMinutes, 25);
    expect(snapshot.remainingMinutes, 25);
  });

  test('Today Engine exposes no next item after the whole plan is complete', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Biology', topic: 'Cells', minutes: 25),
      StudyItem(title: 'Chemistry', topic: 'Atoms', minutes: 25),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(1, 25);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem, isNull);
    expect(snapshot.isComplete, isTrue);
    expect(snapshot.hasRemainingWork, isFalse);
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentItemCompletedMinutes, 25);
    expect(snapshot.completedMinutes, 50);
    expect(snapshot.remainingMinutes, 0);
    expect(snapshot.progress, 1.0);
  });

  test('Today Engine prefers item progress when aggregate progress is stale', () async {
    final store = await makeStore({
      'study_plan': jsonEncode({'totalMinutes': 50, 'items': [
        {'title': 'Biology', 'minutes': 25},
        {'title': 'Chemistry', 'minutes': 25},
      ]}),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 50,
      'item_completed_minutes': jsonEncode({'0': 25}),
    });
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Chemistry');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.completedMinutes, 25);
    expect(snapshot.remainingMinutes, 25);
    expect(snapshot.progress, 0.5);
    expect(snapshot.isComplete, isFalse);
  });

  test('Today Engine recognizes a completed item map even when aggregate is zero', () async {
    final store = await makeStore({
      'study_plan': jsonEncode({'totalMinutes': 25, 'items': [
        {'title': 'Math', 'minutes': 25},
      ]}),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 0,
      'item_completed_minutes': jsonEncode({'0': 25}),
    });
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem, isNull);
    expect(snapshot.completedMinutes, 25);
    expect(snapshot.remainingMinutes, 0);
    expect(snapshot.progress, 1.0);
    expect(snapshot.isComplete, isTrue);
  });

  test('Today Engine resumes from aggregate-only legacy progress', () async {
    final store = await makeStore({
      'study_plan': jsonEncode({'totalMinutes': 75, 'items': [
        {'title': 'Math', 'minutes': 25},
        {'title': 'Physics', 'minutes': 50},
      ]}),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 35,
    });
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentBlockIndex, 0);
    expect(snapshot.currentItemCompletedMinutes, 10);
    expect(snapshot.completedMinutes, 35);
    expect(snapshot.remainingMinutes, 40);
  });

  test('Today Engine skips zero-minute items for aggregate-only progress', () async {
    final store = await makeStore({
      'study_plan': jsonEncode({'totalMinutes': 25, 'items': [
        {'title': 'Placeholder', 'minutes': 0},
        {'title': 'Math', 'minutes': 25},
      ]}),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 0,
    });
    final snapshot = TodayEngine(store).build();
    expect(snapshot.nextItem?.title, 'Math');
    expect(snapshot.currentIndex, 1);
  });

  test('Today Engine exposes daily goal progress from study history', () async {
    final store = await makeStore({'daily_goal_minutes': 120});
    await store.addDailyStudyMinutes(45);

    final snapshot = TodayEngine(store).build();

    expect(snapshot.dailyGoalMinutes, 120);
    expect(snapshot.todayCompletedMinutes, 45);
    expect(snapshot.goalRemainingMinutes, 75);
    expect(snapshot.goalProgress, closeTo(0.375, 0.0001));
    expect(snapshot.dailyGoalReached, isFalse);
  });

  test('Today Engine caps daily goal progress and reports when goal is reached', () async {
    final store = await makeStore({'daily_goal_minutes': 60});
    await store.addDailyStudyMinutes(90);

    final snapshot = TodayEngine(store).build();

    expect(snapshot.dailyGoalMinutes, 60);
    expect(snapshot.todayCompletedMinutes, 90);
    expect(snapshot.goalRemainingMinutes, 0);
    expect(snapshot.goalProgress, 1.0);
    expect(snapshot.dailyGoalReached, isTrue);
  });
}

String _todayKey() {
  final now = DateTime.now();
  return '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
}
