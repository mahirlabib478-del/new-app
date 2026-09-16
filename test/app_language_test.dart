import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/app_language.dart';

void main() {
  test('App language defaults to English', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = AppLanguageStore(prefs);

    expect(store.language, AppLanguage.english);
    expect(AppStrings(store.language).languageName, 'English');
  });

  test('App language persists Bangla selection', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = AppLanguageStore(prefs);

    await store.setLanguage(AppLanguage.bangla);

    expect(store.language, AppLanguage.bangla);
    expect(AppStrings(store.language).profile, 'প্রোফাইল');
    expect(AppStrings(store.language).dailyGoal, 'দৈনিক লক্ষ্য');
  });

  test('App language can switch back to English', () async {
    SharedPreferences.setMockInitialValues({'app_language': 'bn'});
    final prefs = await SharedPreferences.getInstance();
    final store = AppLanguageStore(prefs);

    await store.setLanguage(AppLanguage.english);

    expect(store.language, AppLanguage.english);
    expect(AppStrings(store.language).profile, 'Profile');
  });
}
