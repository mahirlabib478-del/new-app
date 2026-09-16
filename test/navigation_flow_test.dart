import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/today_engine.dart';

void main() {
  testWidgets('Home reflects item-level plan progress and next action', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(
      StudyOS(
        store: store,
        prefs: prefs,
        checkForUpdate: () async => null,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('25 minutes left'), findsOneWidget);
    expect(find.text('25 minutes completed • ${store.xp} XP'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Motion'), 400, scrollable: find.byType(Scrollable).first);
    expect(find.text('Physics'), findsOneWidget);
    expect(find.text('Motion'), findsOneWidget);
    expect(find.text('Start'), findsOneWidget);
  });

  testWidgets('Home Start opens the current unfinished item in Focus mode', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(
      StudyOS(
        store: store,
        prefs: prefs,
        checkForUpdate: () async => null,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Start').first);
    await tester.pumpAndSettle();

    expect(find.text('Physics'), findsOneWidget);
  });

  testWidgets('Home never shows a negative goal remainder', (tester) async {
    SharedPreferences.setMockInitialValues({
      'study_goal_minutes': 30,
    });
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    final plan = StudyPlan(totalMinutes: 60, items: [
      StudyItem(title: 'Math', minutes: 60),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 60);

    await tester.pumpWidget(
      StudyOS(
        store: store,
        prefs: prefs,
        checkForUpdate: () async => null,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('-'), findsNothing);
    expect(find.text('Goal reached. Keep the momentum.'), findsOneWidget);
  });

  test('Today Engine exposes the same item-level next action used by Home', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]));
    await store.addItemCompletedMinutes(0, 25);

    final snapshot = TodayEngine(store).build();
    expect(snapshot.currentIndex, 1);
    expect(snapshot.currentItem?.title, 'Physics');
    expect(snapshot.currentItem?.topic, 'Motion');
  });
}
