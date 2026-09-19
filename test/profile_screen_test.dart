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
        textScale: 1.0,
        onTextScaleChanged: (_) async {},
      ),
    ));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(
      find.text('Daily goal'),
      300,
      scrollable: find.byType(Scrollable),
    );
    await tester.pumpAndSettle();
    expect(find.text('120 of focused study'), findsOneWidget);

    await tester.tap(find.text('Daily goal'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '90');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(store.dailyGoalMinutes, 90);
    expect(find.text('90 of focused study'), findsOneWidget);
    expect(find.text('120 of focused study'), findsNothing);
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
        textScale: 1.0,
        onTextScaleChanged: (_) async {},
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Midnight'), findsOneWidget);
    expect(find.text('Pitch Black'), findsOneWidget);
    expect(find.text('Espresso'), findsOneWidget);
    expect(find.text('Ocean Dark'), findsOneWidget);
    expect(find.text('Forest'), findsOneWidget);
    expect(find.text('Paper Sepia'), findsOneWidget);
    expect(find.text('Mint'), findsOneWidget);
    expect(find.text('Matcha'), findsOneWidget);
    expect(find.text('Sunrise'), findsOneWidget);
    expect(find.text('Nordic Slate'), findsOneWidget);

    await tester.tap(find.text('Pitch Black'));
    await tester.pumpAndSettle();
    expect(selectedTheme, 'pitch_black');

    await tester.scrollUntilVisible(
      find.text('Paper Sepia'),
      100,
      scrollable: find.byType(Scrollable),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Paper Sepia'));
    await tester.pumpAndSettle();
    expect(selectedTheme, 'paper');
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
        textScale: 1.0,
        onTextScaleChanged: (_) async {},
      ),
    ));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(
      find.text('Language'),
      300,
      scrollable: find.byType(Scrollable),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Language'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('বাংলা'));
    await tester.pumpAndSettle();

    expect(selectedLanguage, AppLanguage.bangla);
  });

  testWidgets('Profile calls text scale change callback', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    double? updatedScale;

    await tester.pumpWidget(MaterialApp(
      home: ProfileScreen(
        store: store,
        prefs: prefs,
        themeKey: 'midnight',
        onThemeChanged: (_) async {},
        language: AppLanguage.english,
        onLanguageChanged: (_) async {},
        textScale: 1.0,
        onTextScaleChanged: (scale) async => updatedScale = scale,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(
      find.text('Font Size'),
      300,
      scrollable: find.byType(Scrollable),
    );
    await tester.pumpAndSettle();

    expect(find.text('Font Size'), findsOneWidget);
    expect(find.text('Large'), findsOneWidget);

    await tester.tap(find.text('Large'));
    await tester.pumpAndSettle();

    expect(updatedScale, 1.15);
  });
}
