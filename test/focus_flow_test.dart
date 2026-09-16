import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/focus_flow.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore() async {
    SharedPreferences.setMockInitialValues({});
    return LocalStore(await SharedPreferences.getInstance());
  }

  testWidgets('completion screen treats allocated minutes as the plan budget', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(
      totalMinutes: 60,
      items: [
        StudyItem(title: 'Math', minutes: 30),
      ],
    );

    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 30);

    await tester.pumpWidget(
      MaterialApp(
        home: CompletionScreen(plan: plan, store: store),
      ),
    );

    expect(find.text('30 / 30 minutes completed'), findsOneWidget);
    expect(find.text('100%'), findsOneWidget);
  });

  testWidgets('FocusScreen redirects an already completed plan to Completion', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Math', minutes: 25)],
    );

    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(
      MaterialApp(
        home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Study complete'), findsOneWidget);
    expect(find.text('25 / 25 minutes completed'), findsOneWidget);
    expect(store.currentPlanIndex, 0);
    expect(store.currentBlockIndex, 0);
  });

  testWidgets('Break Continue records the completed block before showing Completion', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(
      totalMinutes: 25,
      items: [StudyItem(title: 'Physics', minutes: 25)],
    );

    await store.savePlan(plan);

    await tester.pumpWidget(
      MaterialApp(
        home: BreakScreen(
          store: store,
          plan: plan,
          index: 0,
          blockIndex: 0,
          completed: 25,
        ),
      ),
    );
    await tester.tap(find.text('Continue'));
    await tester.pumpAndSettle();

    expect(find.text('Study complete'), findsOneWidget);
    expect(find.text('25 / 25 minutes completed'), findsOneWidget);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.focusTimerState, isNull);
    expect(store.breakTimerState, isNull);
  });
}
