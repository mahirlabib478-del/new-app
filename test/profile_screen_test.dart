import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/screens/profile_screen.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Profile refreshes daily goal after saving', (tester) async {
    SharedPreferences.setMockInitialValues({
      'daily_goal_minutes': 120,
    });
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(
      MaterialApp(
        home: ProfileScreen(
          store: store,
          themeKey: 'midnight',
          onThemeChanged: (_) async {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('120 minutes of focused study'), findsOneWidget);

    await tester.tap(find.text('Daily goal'));
    await tester.pumpAndSettle();
    final field = find.byType(TextField);
    await tester.enterText(field, '90');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(store.dailyGoalMinutes, 90);
    expect(find.text('90 minutes of focused study'), findsOneWidget);
    expect(find.text('120 minutes of focused study'), findsNothing);
  });
}
