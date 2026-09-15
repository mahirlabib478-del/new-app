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
}
