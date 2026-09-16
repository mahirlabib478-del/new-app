import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/focus_flow.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/today_engine.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore([Map<String, Object> values = const {}]) async {
    SharedPreferences.setMockInitialValues(values);
    return LocalStore(await SharedPreferences.getInstance());
  }

  StudyPlan singleItemPlan() => StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]);

  Future<Map<String, dynamic>> loadHistory(LocalStore store) async {
    final raw = store.prefs.getString('study_daily_history');
    if (raw == null) return {};
    return Map<String, dynamic>.from(jsonDecode(raw) as Map);
  }

  testWidgets('completion screen treats allocated minutes as the plan budget', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 60, items: [StudyItem(title: 'Math', minutes: 25), StudyItem(title: 'Physics', minutes: 25)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(1, 25);
    await tester.pumpWidget(MaterialApp(home: CompletionScreen(plan: plan, store: store)));
    await tester.pumpAndSettle();
    expect(find.text('50 / 50 minutes completed'), findsOneWidget);
    expect(find.text('100%'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('FocusScreen redirects an already completed plan to Completion', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    expect(find.text('Study complete'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('Focus pause persists a paused state and resume restores a deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    final pauseButton = find.text('Pause');
    await tester.ensureVisible(pauseButton);
    await tester.tap(pauseButton);
    await tester.pump();
    expect(store.focusTimerState, isNotNull);
    expect(store.focusTimerState!.running, isFalse);
    final pausedSeconds = store.focusTimerState!.remainingSeconds;
    await tester.pumpWidget(const SizedBox());
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump();
    expect(find.text('Pause'), findsOneWidget);
    expect(store.focusTimerState?.running, isTrue);
    expect(store.focusTimerState!.remainingSeconds, lessThanOrEqualTo(pausedSeconds));
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
    expect(find.text('Pause'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('expired persisted focus timer enters Break only once', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.setPlanPosition(0, 0);
    await store.saveFocusTimerState(FocusTimerState(index: 0, blockIndex: 0, remainingSeconds: 0, running: true, deadlineMillis: DateTime.now().millisecondsSinceEpoch - 1000));
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    expect(find.text('Break time'), findsOneWidget);
    expect(find.text('Continue'), findsOneWidget);
    expect(find.text('Break time'), findsNWidgets(1));
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.focusTimerState, isNull);
    final history = await loadHistory(store);
    expect(history.values.fold<int>(0, (sum, value) => sum + (value as num).toInt()), 25);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('expired persisted focus timer triggers the break reminder callback', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    var reminderCalls = 0;
    await store.savePlan(plan);
    await store.saveFocusTimerState(FocusTimerState(index: 0, blockIndex: 0, remainingSeconds: 0, running: true, deadlineMillis: DateTime.now().millisecondsSinceEpoch - 1000));
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0, onFocusBlockCompleted: () async => reminderCalls++)));
    await tester.pumpAndSettle();
    expect(reminderCalls, 1);
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

  testWidgets('completed focus block is visible to Today Engine through daily history', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    final snapshot = TodayEngine(store).build();
    expect(snapshot.completedMinutes, 25);
    expect(snapshot.todayCompletedMinutes, 25);
    expect(snapshot.dailyGoalMinutes, 120);
    expect(snapshot.goalRemainingMinutes, 95);
    expect(snapshot.goalProgress, closeTo(25 / 120, 0.0001));
    final history = await loadHistory(store);
    expect(history.values.fold<int>(0, (sum, value) => sum + (value as num).toInt()), 25);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('Break Continue does not double-count a block already persisted by Focus', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    final beforeHistory = await loadHistory(store);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pumpAndSettle();
    final continueFinder = find.text('Continue');
    await tester.ensureVisible(continueFinder);
    await tester.tap(continueFinder);
    await tester.pumpAndSettle();
    expect(find.text('Study complete'), findsOneWidget);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.sessions, 1);
    final afterHistory = await loadHistory(store);
    expect(afterHistory, beforeHistory);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('Break Continue records a directly opened unfinished block', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pumpAndSettle();
    final continueFinder = find.text('Continue');
    await tester.ensureVisible(continueFinder);
    await tester.tap(continueFinder);
    await tester.pumpAndSettle();
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.sessions, 1);
    expect(find.text('Study complete'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('Break Continue moves a multi-block item to its next focus block without double-counting', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 50)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    final beforeHistory = await loadHistory(store);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pumpAndSettle();
    final continueFinder = find.text('Continue');
    await tester.ensureVisible(continueFinder);
    await tester.tap(continueFinder);
    await tester.pumpAndSettle();
    expect(find.text('Focus mode'), findsOneWidget);
    expect(store.itemCompletedMinutes(0), 25);
    expect(store.planCompletedMinutes, 25);
    expect(store.currentPlanIndex, 0);
    expect(store.currentBlockIndex, 1);
    expect(store.sessions, 1);
    final afterHistory = await loadHistory(store);
    expect(afterHistory, beforeHistory);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('Break Continue after the final block reaches Completion and clears plan position', (tester) async {
    final store = await makeStore();
    final plan = StudyPlan(totalMinutes: 50, items: [StudyItem(title: 'Math', minutes: 25), StudyItem(title: 'Physics', minutes: 25)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.addItemCompletedMinutes(1, 25);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 1, blockIndex: 0, completed: 25)));
    await tester.pumpAndSettle();
    final continueFinder = find.text('Continue');
    await tester.ensureVisible(continueFinder);
    await tester.tap(continueFinder);
    await tester.pumpAndSettle();
    expect(find.text('Study complete'), findsOneWidget);
    expect(store.currentPlanIndex, 0);
    expect(store.currentBlockIndex, 0);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('Break pause persists a paused state and resume restores a deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pumpAndSettle();
    final pauseButton = find.text('Pause break');
    await tester.ensureVisible(pauseButton);
    await tester.tap(pauseButton);
    await tester.pump();
    expect(store.breakTimerState, isNotNull);
    expect(store.breakTimerState!.running, isFalse);
    await tester.pumpWidget(const SizedBox());
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pump();
    expect(find.text('Pause break'), findsOneWidget);
    expect(store.breakTimerState?.running, isTrue);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('backgrounding a running focus timer keeps its persisted deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump(const Duration(milliseconds: 100));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    expect(store.focusTimerState?.running, isTrue);
    expect(store.focusTimerState?.deadlineMillis, isNotNull);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('backgrounding a running break timer keeps its persisted deadline', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await tester.pumpWidget(MaterialApp(home: BreakScreen(store: store, plan: plan, index: 0, blockIndex: 0, completed: 25)));
    await tester.pump(const Duration(milliseconds: 100));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    expect(store.breakTimerState?.running, isTrue);
    expect(store.breakTimerState?.deadlineMillis, isNotNull);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('resuming before deadline keeps the focus route and timer running', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.saveFocusTimerState(FocusTimerState(index: 0, blockIndex: 0, remainingSeconds: 100, running: true, deadlineMillis: DateTime.now().millisecondsSinceEpoch + 100000));
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pump();
    expect(find.text('Focus mode'), findsOneWidget);
    expect(store.focusTimerState?.running, isTrue);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('expired persisted focus timer enters Break only once after lifecycle resume', (tester) async {
    final store = await makeStore();
    final plan = singleItemPlan();
    await store.savePlan(plan);
    await store.saveFocusTimerState(FocusTimerState(index: 0, blockIndex: 0, remainingSeconds: 0, running: true, deadlineMillis: DateTime.now().millisecondsSinceEpoch - 1000));
    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();
    expect(find.text('Break time'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });
}
