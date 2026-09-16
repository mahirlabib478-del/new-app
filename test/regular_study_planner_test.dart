import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/regular_study_planner.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Regular Study preserves total budget when topics are split',
      (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    StudyPlan? startedPlan;

    await tester.pumpWidget(
      RegularStudyPlanner(
        store: store,
        total: 60,
        subjects: const ['Math'],
        onStartPlan: (plan) async => startedPlan = plan,
      ),
    );
    await tester.pumpAndSettle();

    final topicField = find.byType(TextField);
    await tester.enterText(topicField, 'Algebra');
    await tester.tap(find.byIcon(Icons.add_rounded));
    await tester.pumpAndSettle();
    await tester.enterText(topicField, 'Geometry');
    await tester.tap(find.byIcon(Icons.add_rounded));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Split evenly'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Start focused study'));
    await tester.pumpAndSettle();

    expect(find.text('Unassigned time'), findsNothing);
    expect(startedPlan, isNotNull);
    expect(startedPlan!.totalMinutes, 60);
    expect(startedPlan!.allocatedMinutes, 60);
    expect(startedPlan!.items.map((item) => item.minutes), containsAll(<int>[30, 30]));
  });

  testWidgets('Regular Study rejects duplicate topic names case-insensitively',
      (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(
      RegularStudyPlanner(
        store: store,
        total: 60,
        subjects: const ['Math'],
        onStartPlan: (_) async {},
      ),
    );
    await tester.pumpAndSettle();

    final topicField = find.byType(TextField);
    await tester.enterText(topicField, 'Algebra');
    await tester.tap(find.byIcon(Icons.add_rounded));
    await tester.pumpAndSettle();
    await tester.enterText(topicField, ' algebra ');
    await tester.tap(find.byIcon(Icons.add_rounded));
    await tester.pumpAndSettle();

    expect(find.text('Algebra'), findsOneWidget);
    expect(find.text('That topic is already added.'), findsOneWidget);
  });
}
