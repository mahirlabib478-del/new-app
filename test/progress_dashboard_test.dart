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
    expect(find.text('25/25m'), findsOneWidget);
    expect(find.text('0/25m'), findsOneWidget);
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
}
