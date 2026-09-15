import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/study_models.dart';

class LocalStore {
  LocalStore(this.prefs);
  final SharedPreferences prefs;

  static const _planKey = 'today_plan';
  static const _themeKey = 'theme_mode';
  static const _minutesKey = 'completed_minutes';

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
  Future<void> addCompletedMinutes(int value) => prefs.setInt(_minutesKey, completedMinutes + value);
}
