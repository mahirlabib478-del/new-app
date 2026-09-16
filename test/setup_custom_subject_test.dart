import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/screens/setup_screen.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  Future<void> openSetup(WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(MaterialApp(home: Setup(store: LocalStore(prefs))));
    await tester.pumpAndSettle();
  }

  testWidgets('adds and selects a custom subject', (tester) async {
    await openSetup(tester);

    final field = find.byType(TextField).last;
    await tester.enterText(field, 'Accounting');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    expect(find.text('Accounting'), findsOneWidget);
    final checkbox = find.byType(CheckboxListTile).last;
    expect(tester.widget<CheckboxListTile>(checkbox).value, isTrue);
  });

  testWidgets('rejects a custom subject that duplicates a default subject', (tester) async {
    await openSetup(tester);

    final field = find.byType(TextField).last;
    await tester.enterText(field, 'physics');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    expect(find.text('physics'), findsNothing);
    expect(find.text('That subject is already in the list.'), findsOneWidget);
  });
}
