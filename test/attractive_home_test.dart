import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:new_app/models/study_models.dart';
import 'package:new_app/services/app_language.dart';
import 'package:new_app/services/local_store.dart';
import 'package:new_app/widgets/attractive_home.dart';

void main() {
  testWidgets('attractive home renders journey and stats', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    final plan = StudyPlan(totalMinutes: 50, items: [
      StudyItem(title: 'Algebra', minutes: 25, topic: 'Quadratic equations'),
      StudyItem(title: 'Physics', minutes: 25, topic: 'Motion'),
    ]);
    await store.savePlan(plan);

    await tester.pumpWidget(MaterialApp(
      home: AttractiveHome(
        store: store,
        language: AppLanguage.english,
        onOpenFocus: ({StudyPlan? plan}) async {},
        onRegularStudy: () {},
        onExam: (_) {},
      ),
    ));

    expect(find.text('TODAY’S MISSION'), findsOneWidget);
    expect(find.text('Your study journey'), findsOneWidget);
    expect(find.text('Algebra'), findsOneWidget);
    expect(find.text('Regular Study'), findsOneWidget);
    expect(find.text('Exam Preparation'), findsOneWidget);
  });
}
