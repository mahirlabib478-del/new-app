import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/today_engine.dart';

void main() {
  Future<StudyOS> buildApp(LocalStore store, SharedPreferences prefs) async {
    return StudyOS(store: store, prefs: prefs, checkForUpdate: () async => null);
  }

  testWidgets('Home shows current plan item and can open focus', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
    ]));

    await tester.pumpWidget(await buildApp(store, prefs));
    await tester.pumpAndSettle();

    expect(find.text('Physics'), findsOneWidget);
    expect(find.textContaining('50'), findsWidgets);
    await tester.scrollUntilVisible(find.text('Motion'), 400, scrollable: find.byType(Scrollable).first);
    await tester.pumpAndSettle();
    expect(find.text('Motion'), findsOneWidget);

    final missionButton = find.byKey(const ValueKey<String>('home-mission-action'));
    expect(missionButton, findsOneWidget);
    await tester.ensureVisible(missionButton);
    await tester.tap(missionButton);
    await tester.pumpAndSettle();
    expect(find.text('Focus'), findsOneWidget);
  });

  testWidgets('Home progress summary remains visible after partial completion', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
    ]));
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(await buildApp(store, prefs));
    await tester.pumpAndSettle();
    expect(find.textContaining('25'), findsWidgets);
    expect(find.widgetWithText(FilledButton, 'Start'), findsOneWidget);
  });

  testWidgets('Home shows goal reached state without negative remaining text', (tester) async {
    SharedPreferences.setMockInitialValues({'daily_goal_minutes': 60});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.savePlan(StudyPlan(totalMinutes: 60, items: [StudyItem(title: 'Physics', topic: 'Motion', minutes: 60)]));
    await store.addItemCompletedMinutes(0, 60);

    await tester.pumpWidget(await buildApp(store, prefs));
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
    expect(snapshot.plan?.items[snapshot.currentIndex].title, 'Physics');
    expect(snapshot.plan?.items[snapshot.currentIndex].topic, 'Motion');
  });
}
