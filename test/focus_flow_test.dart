import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/focus_flow.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('completion screen treats allocated minutes as the plan budget', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
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
}
