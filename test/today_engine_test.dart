import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/today_engine.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore() async {
    SharedPreferences.setMockInitialValues({});
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('Today Engine resumes the first unfinished item', () async {
    final store = await makeStore();
    final plan = StudyPlan(
      totalMinutes: 75,
      items: [
        StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
        StudyItem(title: 'Physics', topic: 'Motion', minutes: 50),
      ],
    );

    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    final snapshot = TodayEngine(store).build();

    expect(snapshot.nextItem?.title, 'Physics');
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentBlockIndex, 0);
    expect(snapshot.remainingMinutes, 50);
  });

  test('Today Engine exposes no next item after the whole plan is complete', () async {
    final store = await makeStore();
    final plan = StudyPlan(
      totalMinutes: 50,
      items: [
        StudyItem(title: 'Biology', topic: 'Cells', minutes: 25),
        StudyItem(title: 'Chemistry', topic: 'Atoms', minutes: 25),
      ],
    );

    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(1, 25);
    final snapshot = TodayEngine(store).build();

    expect(snapshot.nextItem, isNull);
    expect(snapshot.completedMinutes, 50);
    expect(snapshot.remainingMinutes, 0);
    expect(snapshot.progress, 1.0);
  });
}
