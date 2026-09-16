import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/screens/profile_screen.dart';
import 'package:study_os/services/app_language.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Profile refreshes daily goal after saving', (tester) async {
    SharedPreferences.setMockInitialValues({'daily_goal_minutes': 120});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);

    await tester.pumpWidget(MaterialApp(
      home: ProfileScreen(
        store: store,
        prefs: prefs,
        themeKey: 'midnight',
        onThemeChanged: (_) async {},
        language: AppLanguage.english,
        onLanguageChanged: (_) async {},
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('120 minutes of focused study'), findsOneWidget);
    await tester.tap(find.text('Daily goal'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '90');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(store.dailyGoalMinutes, 90);
    expect(find.text('90 minutes of focused study'), findsOneWidget);
    expect(find.text('120 minutes of focused study'), findsNothing);
  });

  testWidgets('Profile exposes all supported theme presets', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    String? selectedTheme;

    await tester.pumpWidget(MaterialApp(
      home: ProfileScreen(
        store: store,
        prefs: prefs,
        themeKey: 'midnight',
        onThemeChanged: (key) async => selectedTheme = key,
        language: AppLanguage.english,
        onLanguageChanged: (_) async {},
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Midnight'), findsOneWidget);
    expect(find.text('Ocean'), findsOneWidget);
    expect(find.text('Forest'), findsOneWidget);
    expect(find.text('Sunrise'), findsOneWidget);

    await tester.tap(find.text('Ocean'));
    await tester.pumpAndSettle();
    expect(selectedTheme, 'ocean');
  });

  testWidgets('Profile calls language change callback', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    AppLanguage? selectedLanguage;

    await tester.pumpWidget(MaterialApp(
      home: ProfileScreen(
        store: store,
        prefs: prefs,
        themeKey: 'midnight',
        onThemeChanged: (_) async {},
        language: AppLanguage.english,
        onLanguageChanged: (language) async => selectedLanguage = language,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Language'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('বাংলা'));
    await tester.pumpAndSettle();

    expect(selectedLanguage, AppLanguage.bangla);
  });
}
