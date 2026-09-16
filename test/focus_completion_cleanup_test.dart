import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/focus_flow.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('completed focus clears timer and plan position before completion navigation', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final plan = StudyPlan(totalMinutes: 25, items: [StudyItem(title: 'Math', minutes: 25)]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);
    await store.setPlanPosition(0, 0);
    await store.saveFocusTimerState(FocusTimerState(
      index: 0,
      blockIndex: 0,
      remainingSeconds: 10,
      running: false,
    ));

    await tester.pumpWidget(MaterialApp(home: FocusScreen(store: store, plan: plan, index: 0, blockIndex: 0)));
    await tester.pumpAndSettle();

    expect(find.text('Study complete'), findsOneWidget);
    expect(store.focusTimerState, isNull);
    expect(store.prefs.containsKey('current_plan_index'), isFalse);
    expect(store.prefs.containsKey('current_block_index'), isFalse);
  });
}
