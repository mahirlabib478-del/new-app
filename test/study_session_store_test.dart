import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/study_session_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore() async {
    SharedPreferences.setMockInitialValues({});
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('starting a second plan preserves the unfinished first session', () async {
    final store = await makeStore();
    final first = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]);
    final second = StudyPlan(totalMinutes: 25, items: [
      StudyItem(title: 'Chemistry', topic: 'Atoms', minutes: 25),
    ]);

    await store.savePlan(first);
    await store.addItemCompletedMinutes(0, 25);
    await store.setPlanPosition(1, 0);
    await store.savePlan(second);

    final saved = StudySessionStore(store).sessions;
    expect(saved, hasLength(1));
    expect(saved.single.plan.items.first.title, 'Math');
    expect(saved.single.itemProgress, {0: 25});
    expect(saved.single.currentIndex, 1);
    expect(store.loadPlan()?.items.single.title, 'Chemistry');
  });

  test('deleting a saved session does not affect the active plan', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', minutes: 50),
    ]));
    await store.addItemCompletedMinutes(0, 25);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [
      StudyItem(title: 'Physics', minutes: 25),
    ]));

    final sessions = StudySessionStore(store).sessions;
    expect(sessions, hasLength(1));
    await StudySessionStore(store).delete(sessions.single.id);

    expect(StudySessionStore(store).sessions, isEmpty);
    expect(store.loadPlan()?.items.single.title, 'Physics');
  });

  test('restoring a saved session brings back its item progress and position', () async {
    final store = await makeStore();
    final first = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]);
    await store.savePlan(first);
    await store.addItemCompletedMinutes(0, 25);
    await store.setPlanPosition(1, 0);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Chemistry', minutes: 25)]));

    final sessionStore = StudySessionStore(store);
    final saved = sessionStore.sessions.single;
    expect(await sessionStore.restore(saved.id), isTrue);
    expect(store.loadPlan()?.items[1].title, 'Physics');
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.currentPlanIndex, 1);
    expect(sessionStore.sessions, isEmpty);
  });
}
