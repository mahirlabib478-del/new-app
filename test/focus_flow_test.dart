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

  StudyPlan singleItemPlan({int totalMinutes = 25, int itemMinutes = 25}) => StudyPlan(
        totalMinutes: totalMinutes,
        items: [StudyItem(title: 'Math', minutes: itemMinutes)],
      );

  testWidgets('completion screen treats allocated minutes as the plan budget', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 60, items: [StudyItem(title: 'Math', minutes: 30)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 30);
    await tester.pumpWidget(MaterialApp(home: CompletionScreen(plan: plan, store: store)));
    expect(find.text('30 / 30 minutes completed'), findsOneWidget);
    expect(find.text('100%'), findsOneWidget);
  });

  testWidgets('FocusScreen redirects an already completed plan to Completion', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    expect(find.text('Study complete'), findsOneWidget);
    expect(find.text('25 / 25 minutes completed'), findsOneWidget);
    expect(store.currentPlanIndex, 0);
    expect(store.currentBlockIndex, 0);
  });

  testWidgets('Focus pause persists a paused state and resume restores a deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump(const Duration(milliseconds: 100));
    final pauseFinder = find.widgetWithText(FilledButton, 'Pause');
    await tester.ensureVisible(pauseFinder);
    await tester.tap(pauseFinder);
    await tester.pump();
    final paused = store.focusTimerState;
    expect(paused, isNotNull);
    expect(paused!.running, isFalse);
    expect(paused.deadlineMillis, isNull);
    expect(paused.remainingSeconds, lessThanOrEqualTo(1500));
    expect(paused.remainingSeconds, greaterThan(0));
    final resumeFinder = find.widgetWithText(FilledButton, 'Resume');
    await tester.ensureVisible(resumeFinder);
    await tester.tap(resumeFinder);
    await tester.pump();
    final resumed = store.focusTimerState;
    expect(resumed, isNotNull);
    expect(resumed!.running, isTrue);
    expect(resumed.deadlineMillis, isNotNull);
    expect(resumed.remainingSeconds, greaterThan(0));
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('backgrounding a running focus timer keeps its persisted deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump(const Duration(seconds: 1));
    final before = store.focusTimerState;
    expect(before, isNotNull);
    expect(before!.running, isTrue);
    expect(before.deadlineMillis, isNotNull);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    final pausedByLifecycle = store.focusTimerState;
    expect(pausedByLifecycle, isNotNull);
    expect(pausedByLifecycle!.running, isTrue);
    expect(pausedByLifecycle.deadlineMillis, isNotNull);
    expect(pausedByLifecycle.remainingSeconds, greaterThan(0));
    expect(pausedByLifecycle.remainingSeconds, lessThanOrEqualTo(before.remainingSeconds));
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('resuming before deadline keeps the focus route and timer running', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump(const Duration(milliseconds: 100));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    final saved = store.focusTimerState;
    expect(saved, isNotNull);
    expect(saved!.running, isTrue);
    expect(saved.deadlineMillis, isNotNull);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(find.text('Focus mode'), findsOneWidget);
    expect(find.text('Pause'), findsOneWidget);
    expect(store.focusTimerState, isNotNull);
    expect(store.focusTimerState!.running, isTrue);
    expect(store.focusTimerState!.deadlineMillis, isNotNull);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('repeated lifecycle resume does not duplicate the focus route', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump(const Duration(milliseconds: 100));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(find.text('Focus mode'), findsOneWidget);
    expect(find.text('Math'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Pause'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('expired persisted focus timer enters Break only once', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.setPlanPosition(0, 0);
    await store.saveFocusTimerState(FocusTimerState(
      index: 0,
      blockIndex: 0,
      remainingSeconds: 0,
      running: true,
      deadlineMillis: DateTime.now().millisecondsSinceEpoch - 1000,
    ));
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    expect(find.text('Break time'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Continue'), findsOneWidget);
    expect(find.text('Break time'), findsNWidgets(1));
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.focusTimerState, isNull);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('expired persisted focus timer records the block before entering Break', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.setPlanPosition(0, 0);
    await store.saveFocusTimerState(FocusTimerState(index: 0, blockIndex: 0, remainingSeconds: 0, running: true, deadlineMillis: DateTime.now().millisecondsSinceEpoch - 1000));
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    expect(find.text('Break time'), findsOneWidget);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.focusTimerState, isNull);
    expect(store.breakTimerState, isNotNull);
  });

  testWidgets('Break Continue does not double-count a block already persisted by Focus', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    final continueFinder = find.text('Continue');
    await tester.ensureVisible(continueFinder);
    await tester.tap(continueFinder);
    await tester.pumpAndSettle();
    expect(find.text('Study complete'), findsOneWidget);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.sessions, 1);
  });

  testWidgets('Break Continue records a directly opened unfinished block', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    final continueFinder = find.text('Continue');
    await tester.ensureVisible(continueFinder);
    await tester.tap(continueFinder);
    await tester.pumpAndSettle();
    expect(find.text('Study complete'), findsOneWidget);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.sessions, 1);
  });

  testWidgets('Break pause persists a paused state and resume restores a deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pump(const Duration(milliseconds: 100));
    final pauseFinder = find.widgetWithText(OutlinedButton, 'Pause break');
    await tester.ensureVisible(pauseFinder);
    await tester.tap(pauseFinder);
    await tester.pump();
    final paused = store.breakTimerState;
    expect(paused, isNotNull);
    expect(paused!.running, isFalse);
    expect(paused.deadlineMillis, isNull);
    expect(paused.remainingSeconds, greaterThan(0));
    final resumeFinder = find.widgetWithText(OutlinedButton, 'Resume break');
    await tester.ensureVisible(resumeFinder);
    await tester.tap(resumeFinder);
    await tester.pump();
    final resumed = store.breakTimerState;
    expect(resumed, isNotNull);
    expect(resumed!.running, isTrue);
    expect(resumed.deadlineMillis, isNotNull);
    expect(resumed.remainingSeconds, greaterThan(0));
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('backgrounding a running break timer keeps its persisted deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pump(const Duration(seconds: 1));
    final before = store.breakTimerState;
    expect(before, isNotNull);
    expect(before!.running, isTrue);
    expect(before.deadlineMillis, isNotNull);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    final pausedByLifecycle = store.breakTimerState;
    expect(pausedByLifecycle, isNotNull);
    expect(pausedByLifecycle!.running, isTrue);
    expect(pausedByLifecycle.deadlineMillis, isNotNull);
    expect(pausedByLifecycle.remainingSeconds, greaterThan(0));
    expect(pausedByLifecycle.remainingSeconds, lessThanOrEqualTo(before.remainingSeconds));
    await tester.pumpWidget(const SizedBox());
  });
}