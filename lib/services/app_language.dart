import 'package:shared_preferences/shared_preferences.dart';

enum AppLanguage { english, bangla }

class AppLanguageStore {
  AppLanguageStore(this.prefs);
  final SharedPreferences prefs;
  static const _key = 'app_language';

  AppLanguage get language {
    final value = prefs.getString(_key);
    return value == 'bn' ? AppLanguage.bangla : AppLanguage.english;
  }

  Future<void> setLanguage(AppLanguage language) =>
      prefs.setString(_key, language == AppLanguage.bangla ? 'bn' : 'en');
}

class AppStrings {
  const AppStrings(this.language);
  final AppLanguage language;

  bool get isBangla => language == AppLanguage.bangla;

  String get languageName => isBangla ? 'বাংলা' : 'English';
  String get languageLabel => isBangla ? 'ভাষা' : 'Language';
  String get english => 'English';
  String get bangla => 'বাংলা';
  String get profile => isBangla ? 'প্রোফাইল' : 'Profile';
  String get appearance => isBangla ? 'চেহারা' : 'Appearance';
  String get dailyGoal => isBangla ? 'দৈনিক লক্ষ্য' : 'Daily goal';
  String get reminders => isBangla ? 'রিমাইন্ডার' : 'Reminders';
  String get studyReminder => isBangla ? 'স্টাডি রিমাইন্ডার' : 'Study reminder';
  String get breakReminder => isBangla ? 'বিরতির রিমাইন্ডার' : 'Break reminder';
  String get planReminder => isBangla ? 'প্ল্যান রিমাইন্ডার' : 'Plan reminder';
  String get soundEffects => isBangla ? 'সাউন্ড ইফেক্ট' : 'Sound effects';
  String get soundEffectsSubtitle => isBangla ? 'ট্যাপ, সম্পন্ন ও রিওয়ার্ড সাউন্ড' : 'Tap, completion and reward sounds';
  String get studyTime => isBangla ? 'স্টাডির সময়' : 'Study time';
  String get planTime => isBangla ? 'প্ল্যানের সময়' : 'Plan time';
  String get afterFocusBlock => isBangla ? 'একটি ফোকাস ব্লক শেষ হলে' : 'After a completed focus block';
  String get cancel => isBangla ? 'বাতিল' : 'Cancel';
  String get save => isBangla ? 'সংরক্ষণ' : 'Save';
  String get minutes => isBangla ? 'মিনিট' : 'minutes';
  String get focusedStudy => isBangla ? 'ফোকাসড স্টাডি' : 'of focused study';
  String get dailyStudyGoal => isBangla ? 'দৈনিক স্টাডি লক্ষ্য' : 'Daily study goal';
  String get buildConsistency => isBangla ? 'নিয়মিত পড়ুন। গভীরভাবে ফোকাস করুন।' : 'Build consistency. Focus deeply.';
  String get deepFocus => isBangla ? 'গভীর ফোকাস, কম ভিজ্যুয়াল বিভ্রান্তি' : 'Deep focus, low visual noise';
  String get coolCalm => isBangla ? 'শান্ত ও ঠান্ডা অনুভূতি' : 'Cool and calm';
  String get naturalGrounded => isBangla ? 'প্রাকৃতিক ও স্থির' : 'Natural and grounded';
  String get warmBright => isBangla ? 'উষ্ণ ও উজ্জ্বল' : 'Warm and bright';
  String get dailyGoalUpdated => isBangla ? 'দৈনিক লক্ষ্য আপডেট হয়েছে।' : 'Daily goal updated.';
}
