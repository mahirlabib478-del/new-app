import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/screens/setup_screen.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  testWidgets('adds and selects a custom subject', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);

    await tester.pumpWidget(
      MaterialApp(
        home: Setup(store: store),
      ),
    );

    await tester.enterText(find.byType(TextField).last, 'Accounting');
    await tester.tap(find.byTooltip('Add subject'));
    await tester.pump();

    expect(find.text('Accounting'), findsOneWidget);
    final checkbox = find.byType(CheckboxListTile).last;
    expect(tester.widget<CheckboxListTile>(checkbox).value, isTrue);
  });

  testWidgets('rejects a custom subject that duplicates a default subject',
      (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);

    await tester.pumpWidget(
      MaterialApp(
        home: Setup(store: store),
      ),
    );

    await tester.enterText(find.byType(TextField).last, 'physics');
    await tester.tap(find.byTooltip('Add subject'));
    await tester.pump();

    expect(find.text('physics'), findsNothing);
    expect(find.text('That subject is already in the list.'), findsOneWidget);
  });
}
