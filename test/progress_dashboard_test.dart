import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/progress_dashboard.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Progress uses item-level plan progress when aggregate is stale', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final plan = StudyPlan(
      totalMinutes: 50,
      items: [
        StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
        StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
      ],
    );
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(MaterialApp(home: ProgressDashboard(store: store)));
    await tester.pumpAndSettle();

    expect(find.text('25 / 50 min'), findsOneWidget);
    expect(find.text('50% plan complete'), findsOneWidget);
  });

  testWidgets('Progress falls back to aggregate progress for legacy plans', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final plan = StudyPlan(
      totalMinutes: 50,
      items: [
        StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
        StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
      ],
    );
    await store.savePlan(plan);
    await store.addCompletedMinutes(10);

    await tester.pumpWidget(MaterialApp(home: ProgressDashboard(store: store)));
    await tester.pumpAndSettle();

    expect(find.text('10 / 50 min'), findsOneWidget);
    expect(find.text('20% plan complete'), findsOneWidget);
  });

  testWidgets('Progress weekly total uses the last seven calendar days', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final now = DateTime.now();

    await store.addDailyStudyMinutes(30, date: now);
    await store.addDailyStudyMinutes(25, date: now.subtract(const Duration(days: 3)));
    await store.addDailyStudyMinutes(15, date: now.subtract(const Duration(days: 6)));
    await store.addDailyStudyMinutes(100, date: now.subtract(const Duration(days: 7)));

    await tester.pumpWidget(MaterialApp(home: ProgressDashboard(store: store)));
    await tester.pumpAndSettle();

    expect(find.text('70 focused minutes this week'), findsOneWidget);
  });

  testWidgets('Progress daily goal refreshes after saving a new goal', (tester) async {
    SharedPreferences.setMockInitialValues({'daily_goal_minutes': 120});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(MaterialApp(home: ProgressDashboard(store: store)));
    await tester.pumpAndSettle();

    expect(find.text('0 / 120 min'), findsOneWidget);
    await tester.tap(find.byTooltip('Change goal'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '90');
    await tester.tap(find.text('Save'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));

    expect(store.dailyGoalMinutes, 90);
    expect(find.text('0 / 90 min'), findsOneWidget);
  });
}
