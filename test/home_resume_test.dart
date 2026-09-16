import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/services/app_language.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<LocalStore> makeStore(Map<String, Object> values) async {
    SharedPreferences.setMockInitialValues(values);
    return LocalStore(await SharedPreferences.getInstance());
  }

  Widget home(LocalStore store) => MaterialApp(
        home: Home(
          store: store,
          onOpenFocus: ({StudyPlan? plan}) async {},
          onRegularStudy: () {},
          onExam: (_) {},
          language: AppLanguage.english,
        ),
      );

  testWidgets('completed plan does not expose a Resume/Start action', (tester) async {
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(plan.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 50,
      'item_completed_minutes': jsonEncode({'0': 25, '1': 25}),
    });

    await tester.pumpWidget(home(store));
    await tester.pumpAndSettle();

    expect(find.text('0 minutes left'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Start'), findsNothing);
  });

  testWidgets('unfinished plan exposes the current item Start action', (tester) async {
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Math', minutes: 25),
      StudyItem(title: 'Physics', minutes: 25),
    ]);
    final store = await makeStore({
      'study_plan': jsonEncode(plan.toJson()),
      'study_plan_date': _todayKey(),
      'plan_completed_minutes': 25,
      'item_completed_minutes': jsonEncode({'0': 25}),
    });

    await tester.pumpWidget(home(store));
    await tester.pumpAndSettle();

    expect(find.text('25 minutes left'), findsOneWidget);
    expect(find.text('Physics'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Start'), findsOneWidget);
  });
}

String _todayKey() {
  final now = DateTime.now();
  return '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
}
