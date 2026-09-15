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
    final plan = StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Math', minutes: 25)],
    );

    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 40);

    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.completedMinutes, 25);
  });

  test('saving a new plan resets plan-specific progress', () async {
    final store = await makeStore();
    final first = StudyPlan(
      totalMinutes: 50,
      items: [StudyItem(title: 'Math', minutes: 50)],
    );
    final second = StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Physics', minutes: 25)],
    );

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
    final plan = StudyPlan(
      totalMinutes: 50,
      items: [StudyItem(title: 'Biology', minutes: 50)],
    );

    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(0, 25);

    expect(store.sessions, 2);
    expect(store.completedMinutes, 50);
    expect(store.xp, 100);
  });

  test('focus timer state survives store recreation and savePlan clears it', () async {
    final store = await makeStore();
    final state = FocusTimerState(
      index: 1,
      blockIndex: 2,
      remainingSeconds: 317,
      running: true,
      deadlineMillis: 1234567890,
    );

    await store.saveFocusTimerState(state);
    final restored = store.focusTimerState;

    expect(restored?.index, 1);
    expect(restored?.blockIndex, 2);
    expect(restored?.remainingSeconds, 317);
    expect(restored?.running, isTrue);
    expect(restored?.deadlineMillis, 1234567890);

    await store.savePlan(StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Chemistry', minutes: 25)],
    ));
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
    await store.savePlan(StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Physics', minutes: 25)],
    ));

    await store.addItemCompletedMinutes(0, 25);

    expect(store.studyMinutesOn(DateTime.now()), 25);
  });
}
