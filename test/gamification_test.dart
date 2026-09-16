import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/gamification.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore() async {
    SharedPreferences.setMockInitialValues({});
    return LocalStore(await SharedPreferences.getInstance());
  }

  test('achievement rules are locked for a new learner', () async {
    final store = await makeStore();
    final achievements = Gamification(store).achievements();

    expect(achievements, hasLength(5));
    expect(achievements.every((achievement) => !achievement.unlocked), isTrue);
    expect(Gamification(store).xpToNextLevel(), 250);
  });

  test('achievements unlock from persisted study metrics', () async {
    final store = await makeStore();
    await store.savePlan(StudyPlan(
      totalMinutes: 180,
      items: [StudyItem(title: 'Math', minutes: 180)],
    ));
    await store.addItemCompletedMinutes(0, 60);
    await store.addItemCompletedMinutes(0, 60);
    await store.addItemCompletedMinutes(0, 60);

    final achievements = Gamification(store).achievements();
    expect(achievements[0].unlocked, isTrue);
    expect(achievements[1].unlocked, isTrue);
    expect(achievements[2].unlocked, isFalse);
    expect(achievements[4].unlocked, isFalse);
    expect(Gamification(store).xpToNextLevel(), 140);
  });
}
