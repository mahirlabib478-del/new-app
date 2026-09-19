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
  String get appearance => isBangla ? 'চেহারা ও থিম' : 'Appearance & Theme';
  String get fontSize => isBangla ? 'ফন্ট সাইজ' : 'Font Size';
  String get fontSizeSub => isBangla ? 'পড়ার সুবিধার জন্য লেখার আকার' : 'Text scaling for comfortable reading';
  String get fontSmall => isBangla ? 'ছোট' : 'Small';
  String get fontNormal => isBangla ? 'স্বাভাবিক' : 'Normal';
  String get fontLarge => isBangla ? 'বড়' : 'Large';
  String get fontExtraLarge => isBangla ? 'খুব বড়' : 'Extra Large';
  String get dailyGoal => isBangla ? 'দৈনিক লক্ষ্য' : 'Daily goal';
  String get reminders => isBangla ? 'রিমাইন্ডার' : 'Reminders';
  String get studyReminder => isBangla ? 'স্টাডি রিমাইন্ডার' : 'Study reminder';
  String get breakReminder => isBangla ? 'বিরতির রিমাইন্ডার' : 'Break reminder';
  String get planReminder => isBangla ? 'প্ল্যান রিমাইন্ডার' : 'Plan reminder';
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
  String get notificationPermissionTitle => isBangla ? 'রিমাইন্ডার চালু করুন' : 'Turn on reminders';
  String get notificationPermissionBody => isBangla ? 'স্টাডি OS আপনার নির্ধারিত সময়ে পড়াশোনা ও বিরতির নোটিফিকেশন পাঠাবে।' : 'Study OS uses notifications to remind you about study and break times.';
  String get allowNotifications => isBangla ? 'নোটিফিকেশন চালু করুন' : 'Allow notifications';
  String get notificationLater => isBangla ? 'এখন নয়' : 'Not now';
  String get backgroundReminderHint => isBangla ? 'কিছু ফোনে ব্যাকগ্রাউন্ড রেস্ট্রিকশন থাকলে রিমাইন্ডার দেরি হতে পারে। ব্যাটারি/অটোস্টার্টে Study OS-কে অনুমতি দিন।' : 'Some phones restrict background apps. Allow Study OS to run in the background if reminders are delayed.';
  String get ambientSound => isBangla ? 'অ্যাম্বিয়েন্ট সাউন্ড' : 'Ambient sound';
  String get ambientSoundSubtitle => isBangla ? 'মনোযোগ বাড়াতে ব্যাকগ্রাউন্ড সাউন্ড চালান' : 'Mask distractions with soothing background sound';
  String get ambientSoundVolume => isBangla ? 'ভলিউম' : 'Volume';
  String get autoPlayWithFocus => isBangla ? 'ফোকাস শুরু হলে স্বয়ংক্রিয়ভাবে চালু' : 'Auto-play when focus starts';
  String get noSound => isBangla ? 'শব্দহীন' : 'Muted';
}
