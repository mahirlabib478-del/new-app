import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Study Hub removes planner before focus starts', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(
      StudyOS(
        store: store,
        checkForUpdate: () async => null,
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Study').last);
    await tester.pumpAndSettle();
    expect(find.text('Choose your next move'), findsOneWidget);

    await tester.tap(find.text('Exam Preparation').last);
    await tester.pumpAndSettle();
    expect(find.text('EXAM MODE'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Physics');
    await tester.tap(find.byIcon(Icons.add_rounded));
    await tester.pumpAndSettle();

    final plannerList = find.byType(ListView).first;
    final plannerScrollable = find
        .descendant(
          of: plannerList,
          matching: find.byType(Scrollable),
        )
        .first;
    final generatePlan = find.text('Generate plan');
    await tester.scrollUntilVisible(generatePlan, 400, scrollable: plannerScrollable);
    await tester.pumpAndSettle();
    await tester.tap(generatePlan);
    await tester.pumpAndSettle();
    expect(find.text('Your exam plan is ready'), findsOneWidget);

    final startExamPlan = find.text('Start exam plan');
    await tester.ensureVisible(startExamPlan);
    await tester.pumpAndSettle();
    await tester.tap(startExamPlan);
    await tester.pumpAndSettle();

    expect(find.text('Choose your next move'), findsNothing);
    expect(find.text('Focus mode'), findsOneWidget);
    expect(find.text('Physics'), findsOneWidget);
  });

  testWidgets('Home reflects item-level plan progress and next action', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(
      StudyOS(
        store: store,
        checkForUpdate: () async => null,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('25 min left'), findsOneWidget);
    expect(find.text('25 min completed • ${store.xp} XP'), findsOneWidget);
    expect(find.text('Physics'), findsOneWidget);
    expect(find.text('Motion'), findsOneWidget);
    expect(find.text('Start'), findsOneWidget);
  });

  testWidgets('Home Start opens the current unfinished item in Focus mode', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
      StudyItem(title: 'Physics', topic: 'Motion', minutes: 25),
    ]);
    await store.savePlan(plan);
    await store.addItemCompletedMinutes(0, 25);

    await tester.pumpWidget(
      StudyOS(
        store: store,
        checkForUpdate: () async => null,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Start'));
    await tester.pumpAndSettle();

    expect(find.text('Focus mode'), findsOneWidget);
    expect(find.text('Physics'), findsOneWidget);
    expect(find.text('Motion'), findsOneWidget);
    expect(find.text('Math'), findsNothing);
  });
}
