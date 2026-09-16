import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Study Hub removes planner before focus starts', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(StudyOS(store: store));
    await tester.tap(find.text('Study').last);
    await tester.pumpAndSettle();
    expect(find.text('Choose your next move'), findsOneWidget);

    await tester.tap(find.text('Exam Preparation').last);
    await tester.pumpAndSettle();
    expect(find.text('EXAM MODE'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Physics');
    await tester.tap(find.byIcon(Icons.add_rounded));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Generate plan'));
    await tester.pumpAndSettle();
    expect(find.text('Your exam plan is ready'), findsOneWidget);

    await tester.tap(find.text('Start exam plan'));
    await tester.pumpAndSettle();

    expect(find.text('Choose your next move'), findsNothing);
    expect(find.text('Focus mode'), findsOneWidget);
    expect(find.text('Physics'), findsOneWidget);
  });
}
