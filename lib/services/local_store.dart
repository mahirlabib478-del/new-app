import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/study_models.dart';

class LocalStore {
  LocalStore(this.prefs);
  final SharedPreferences prefs;

  static const _planKey = 'today_plan';
  static const _themeKey = 'theme_mode';
  static const _minutesKey = 'completed_minutes';
  static const _xpKey = 'study_xp';
  static const _streakKey = 'study_streak';
  static const _lastStudyKey = 'last_study_date';
  static const _sessionsKey = 'study_sessions';

  Future<void> savePlan(StudyPlan plan) async {
    await prefs.setString(_planKey, jsonEncode(plan.toJson()));
  }

  StudyPlan? loadPlan() {
    final raw = prefs.getString(_planKey);
    if (raw == null) return null;
    final json = jsonDecode(raw) as Map<String, dynamic>;
    final items = (json['items'] as List<dynamic>? ?? [])
        .map((e) => StudyItem.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList();
    return StudyPlan(totalMinutes: json['totalMinutes'] as int? ?? 0, items: items);
  }

  bool get darkMode => prefs.getBool(_themeKey) ?? true;
  Future<void> setDarkMode(bool value) => prefs.setBool(_themeKey, value);

  int get completedMinutes => prefs.getInt(_minutesKey) ?? 0;
  int get xp => prefs.getInt(_xpKey) ?? 0;
  int get streak => prefs.getInt(_streakKey) ?? 0;
  int get sessions => prefs.getInt(_sessionsKey) ?? 0;
  int get level => (xp ~/ 250) + 1;
  int get levelProgress => xp % 250;

  Future<void> addCompletedMinutes(int value) async {
    final minutes = value.clamp(0, 1440);
    if (minutes <= 0) return;
    await prefs.setInt(_minutesKey, completedMinutes + minutes);
    await prefs.setInt(_xpKey, xp + minutes * 2);
    await prefs.setInt(_sessionsKey, sessions + 1);
    final today = _dateKey(DateTime.now());
    final last = prefs.getString(_lastStudyKey);
    if (last != today) {
      final yesterday = _dateKey(DateTime.now().subtract(const Duration(days: 1)));
      final nextStreak = last == yesterday ? streak + 1 : 1;
      await prefs.setInt(_streakKey, nextStreak);
      await prefs.setString(_lastStudyKey, today);
    }
  }

  String _dateKey(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
