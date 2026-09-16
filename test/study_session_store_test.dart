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
    final first = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 25), StudyItem(title: 'Physics', topic: 'Motion', minutes: 25)]);
    final second = StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Chemistry', topic: 'Atoms', minutes: 25)]);
    await store.savePlan(first, mode: 'Regular Study');
    await store.addItemCompletedMinutes(0, 25);
    await store.setPlanPosition(1, 0);
    await store.savePlan(second, mode: 'Exam Preparation');
    final saved = StudySessionStore(store).sessions;
    expect(saved, hasLength(1));
    expect(saved.single.plan.items.first.title, 'Math');
    expect(saved.single.itemProgress, {0: 25});
    expect(saved.single.currentIndex, 1);
    expect(saved.single.mode, 'Regular Study');
    expect(store.activeStudyMode, 'Exam Preparation');
    expect(store.loadPlan()?.items.single.title, 'Chemistry');
  });

  test('running session snapshot updates instead of creating duplicates', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 25), StudyItem(title: 'Physics', topic: 'Motion', minutes: 25)]);
    await store.savePlan(plan, mode: 'Regular Study');
    final sessionStore = StudySessionStore(store);
    await sessionStore.archiveCurrentPlan();
    await store.addItemCompletedMinutes(0, 25);
    await store.setPlanPosition(1, 0);
    await sessionStore.archiveCurrentPlan();
    final saved = sessionStore.sessions;
    expect(saved, hasLength(1));
    expect(saved.single.itemProgress, {0: 25});
    expect(saved.single.currentIndex, 1);
    expect(saved.single.mode, 'Regular Study');
  });

  test('canonical plan identity matches equivalent plans without string comparison in UI code', () async {
    final store = await makeStore();
    final first = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 25), StudyItem(title: 'Physics', topic: 'Motion', minutes: 25)]);
    final equivalent = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 25), StudyItem(title: 'Physics', topic: 'Motion', minutes: 25)]);
    final different = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 30), StudyItem(title: 'Physics', topic: 'Motion', minutes: 20)]);
    final sessionStore = StudySessionStore(store);
    expect(sessionStore.samePlan(first, equivalent), isTrue);
    expect(sessionStore.samePlan(first, different), isFalse);
  });

  test('deleting a saved session does not affect the active plan', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]), mode: 'Regular Study');
    await store.addItemCompletedMinutes(0, 25);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Physics', minutes: 25)]), mode: 'Exam Preparation');
    final sessions = StudySessionStore(store).sessions;
    expect(sessions, hasLength(1));
    await StudySessionStore(store).delete(sessions.single.id);
    expect(StudySessionStore(store).sessions, isEmpty);
    expect(store.loadPlan()?.items.single.title, 'Physics');
  });

  test('restoring a saved session brings back its item progress, position and mode', () async {
    final store = await makeStore();
    final first = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 25), StudyItem(title: 'Physics', topic: 'Motion', minutes: 25)]);
    await store.savePlan(first, mode: 'Regular Study');
    await store.addItemCompletedMinutes(0, 25);
    await store.setPlanPosition(1, 0);
    await store.savePlan(StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Chemistry', minutes: 25)]), mode: 'Exam Preparation');
    final sessionStore = StudySessionStore(store);
    final saved = sessionStore.sessions.single;
    expect(await sessionStore.restore(saved.id), isTrue);
    expect(store.loadPlan()?.items[1].title, 'Physics');
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.currentPlanIndex, 1);
    expect(store.activeStudyMode, 'Regular Study');
    expect(sessionStore.sessions, isEmpty);
  });

  test('completed active plan is removed from saved sessions', () async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', topic: 'Algebra', minutes: 25)]);
    await store.savePlan(plan, mode: 'Regular Study');
    final sessionStore = StudySessionStore(store);
    await sessionStore.archiveCurrentPlan();
    await store.addItemCompletedMinutes(0, 25);
    await sessionStore.archiveCurrentPlan();
    expect(sessionStore.sessions, isEmpty);
  });
}
