import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore([Map<String, Object> values = const {}]) async {
    SharedPreferences.setMockInitialValues(values);
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('completed minutes are capped by the planned item budget', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 40);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.completedMinutes, 25);
  });

  test('completion cannot exceed actual allocation when total budget is larger', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 60, items: [StudyItem(title: 'Math', minutes: 25)]);
    await store.savePlan(plan);
    await store.addCompletedMinutes(60);
    expect(store.planCompletedMinutes, 25);
    expect(store.completedMinutes, 25);
    expect(store.sessions, 1);
    expect(store.xp, 50);
  });

  test('invalid item indexes do not mutate study totals', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    await store.addItemCompletedMinutes(-1, 10);
    await store.addItemCompletedMinutes(1, 10);
    expect(store.completedMinutes, 0);
    expect(store.planCompletedMinutes, 0);
    expect(store.sessions, 0);
    expect(store.xp, 0);
    expect(store.studyMinutesOn(DateTime.now()), 0);
  });

  test('saving a new plan resets plan-specific progress', () async {
    final store = await makeStore();
    final first = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]);
    final second = StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Physics', minutes: 25)]);
    await store.savePlan(first);
    await store.addItemCompletedMinutes(0, 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.itemCompletedMinutes(0), 25);
    await store.savePlan(second);
    expect(store.planCompletedMinutes, 0);
    expect(store.itemCompletedMinutes(0), 0);
  });

  test('focus blocks increment sessions while preserving total XP', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Biology', minutes: 50)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(0, 25);
    expect(store.sessions, 2);
    expect(store.completedMinutes, 50);
    expect(store.xp, 100);
  });

  test('focus timer state survives store recreation and savePlan clears it', () async {
    final store = await makeStore();
    final state = FocusTimerState(index: 1, blockIndex: 2, remainingSeconds: 317, running: true, deadlineMillis: 1234567890);
    await store.saveFocusTimerState(state);
    final restored = store.focusTimerState;
    expect(restored?.index, 1);
    expect(restored?.blockIndex, 2);
    expect(restored?.remainingSeconds, 317);
    expect(restored?.running, isTrue);
    expect(restored?.deadlineMillis, 1234567890);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Chemistry', minutes: 25)]));
    expect(store.focusTimerState, isNull);
  });

  test('daily history accumulates completed focus minutes by date', () async {
    final store = await makeStore();
    final date = DateTime(2026, 9, 15);
    await store.addDailyStudyMinutes(25, date: date);
    await store.addDailyStudyMinutes(15, date: date);
    expect(store.studyMinutesOn(date), 40);
    expect(store.dailyStudyMinutes, containsPair('2026-09-15', 40));
  });

  test('recording completion also updates daily history', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Physics', minutes: 25)]));
    await store.addItemCompletedMinutes(0, 25);
    expect(store.studyMinutesOn(DateTime.now()), 25);
  });

  test('break timer state survives persistence and is cleared with a new plan', () async {
    final store = await makeStore();
    final state = BreakTimerState(index: 0, blockIndex: 1, breakMinutes: 10, remainingSeconds: 487, running: true, deadlineMillis: 1234567890);
    await store.saveBreakTimerState(state);
    final restored = store.breakTimerState;
    expect(restored?.index, 0);
    expect(restored?.blockIndex, 1);
    expect(restored?.breakMinutes, 10);
    expect(restored?.remainingSeconds, 487);
    expect(restored?.running, isTrue);
    expect(restored?.deadlineMillis, 1234567890);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'English', minutes: 25)]));
    expect(store.breakTimerState, isNull);
  });

  test('daily goal defaults to 120 minutes and persists within safe bounds', () async {
    final store = await makeStore();
    expect(store.dailyGoalMinutes, 120);
    await store.setDailyGoalMinutes(180);
    expect(store.dailyGoalMinutes, 180);
    await store.setDailyGoalMinutes(5);
    expect(store.dailyGoalMinutes, 15);
    await store.setDailyGoalMinutes(1000);
    expect(store.dailyGoalMinutes, 720);
  });

  test('zero-minute items keep their indexes when a plan is loaded', () async {
    final store = await makeStore();
    final plan = StudyPlan(
      totalMinutes: 50,
      items: [
        StudyItem(title: 'Math', minutes: 25),
        StudyItem(title: 'Optional', minutes: 0),
        StudyItem(title: 'Physics', minutes: 25),
      ],
    );
    await store.savePlan(plan);

    final loaded = store.loadPlan();
    expect(loaded?.items.length, 3);
    expect(loaded?.items[1].title, 'Optional');
    expect(loaded?.items[2].title, 'Physics');

    await store.addItemCompletedMinutes(2, 25);
    expect(store.itemCompletedMinutes(2), 25);
    expect(store.itemCompletedMinutes(1), 0);
    expect(store.planCompletedMinutes, 25);
  });

  test('model parsing accepts numeric json values and ignores malformed item entries', () {
    final plan = StudyPlan.fromJson({
      'totalMinutes': 90.9,
      'items': [
        {'title': 'Math', 'minutes': 25.8, 'topic': 'Algebra'},
        'not-an-item',
        {'title': 'Physics', 'minutes': 20},
      ],
    });
    expect(plan.totalMinutes, 90);
    expect(plan.items.length, 2);
    expect(plan.items.first.minutes, 25);
    expect(plan.items.last.title, 'Physics');
  });

  test('persisted plans normalize an unsafe total budget', () async {
    final store = await makeStore({
      'study_plan': jsonEncode({
        'totalMinutes': -50,
        'items': [
          {'title': 'Math', 'minutes': 25},
          {'title': 'Optional', 'minutes': 0},
        ],
      }),
      'study_plan_date': '2026-09-15',
    });
    final loaded = store.loadPlan();
    expect(loaded?.totalMinutes, 0);
    expect(loaded?.items.length, 2);
    expect(loaded?.items[1].minutes, 0);
  });

  test('persisted item progress ignores negative and non-numeric values', () async {
    final store = await makeStore({
      'item_completed_minutes': jsonEncode({
        '0': -10,
        '1': 30.5,
        'bad': 'value',
      }),
    });
    expect(store.itemCompletedMinutesMap, {1: 30});
    expect(store.itemCompletedMinutes(0), 0);
    expect(store.itemCompletedMinutes(1), 30);
  });

  test('malformed persisted timer payloads are ignored safely', () async {
    final store = await makeStore({
      'focus_timer_state': '{not-json',
      'break_timer_state': jsonEncode({'index': 'bad'}),
    });
    expect(store.focusTimerState, isNull);
    expect(store.breakTimerState, isNotNull);
    expect(store.breakTimerState?.index, 0);
  });

  test('non-positive completion never changes totals or streak', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]));
    await store.addItemCompletedMinutes(0, 0);
    await store.addItemCompletedMinutes(0, -10);
    expect(store.completedMinutes, 0);
    expect(store.planCompletedMinutes, 0);
    expect(store.sessions, 0);
    expect(store.xp, 0);
    expect(store.streak, 0);
  });

  test('multiple completions on the same day do not increment streak twice', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]));
    await store.addItemCompletedMinutes(0, 25);
    expect(store.streak, 1);
    await store.addItemCompletedMinutes(0, 25);
    expect(store.streak, 1);
  });
}
