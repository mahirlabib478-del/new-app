import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/study_models.dart';

class LocalStore {
  LocalStore(this.prefs);
  final SharedPreferences prefs;

  static const _planKey = 'today_plan';
  static const _planDateKey = 'today_plan_date';
  static const _themeKey = 'theme_mode';
  static const _themePresetKey = 'theme_preset';
  static const _minutesKey = 'completed_minutes';
  static const _planMinutesKey = 'plan_completed_minutes';
  static const _itemMinutesKey = 'plan_item_completed_minutes';
  static const _xpKey = 'study_xp';
  static const _streakKey = 'study_streak';
  static const _lastStudyKey = 'last_study_date';
  static const _sessionsKey = 'study_sessions';
  static const _indexKey = 'current_plan_index';
  static const _blockKey = 'current_block_index';

  Future<void> savePlan(StudyPlan plan) async {
    await prefs.setString(_planKey, jsonEncode(plan.toJson()));
    await prefs.setString(_planDateKey, _dateKey(DateTime.now()));
    await prefs.setInt(_planMinutesKey, 0);
    await prefs.remove(_itemMinutesKey);
    await setPlanPosition(0, 0);
  }

  StudyPlan? loadPlan() {
    final raw = prefs.getString(_planKey);
    if (raw == null) return null;

    final savedDate = prefs.getString(_planDateKey);
    final today = _dateKey(DateTime.now());
    if (savedDate != null && savedDate != today) return null;

    try {
      final json = jsonDecode(raw) as Map<String, dynamic>;
      final items = (json['items'] as List<dynamic>? ?? [])
          .map((e) => StudyItem.fromJson(Map<String, dynamic>.from(e as Map)))
          .where((item) => item.minutes > 0)
          .toList();
      if (items.isEmpty) return null;
      return StudyPlan(totalMinutes: json['totalMinutes'] as int? ?? 0, items: items);
    } catch (_) {
      return null;
    }
  }

  bool get darkMode => prefs.getBool(_themeKey) ?? true;
  Future<void> setDarkMode(bool value) => prefs.setBool(_themeKey, value);

  String get themePreset {
    final saved = prefs.getString(_themePresetKey);
    if (saved != null && saved.isNotEmpty) return saved;
    return darkMode ? 'midnight' : 'sunrise';
  }

  Future<void> setThemePreset(String value) => prefs.setString(_themePresetKey, value);

  int get completedMinutes => prefs.getInt(_minutesKey) ?? 0;
  int get planCompletedMinutes => prefs.getInt(_planMinutesKey) ?? 0;
  int get xp => prefs.getInt(_xpKey) ?? 0;
  int get streak => prefs.getInt(_streakKey) ?? 0;
  int get sessions => prefs.getInt(_sessionsKey) ?? 0;
  int get level => (xp ~/ 250) + 1;
  int get levelProgress => xp % 250;
  int get currentPlanIndex => prefs.getInt(_indexKey) ?? 0;
  int get currentBlockIndex => prefs.getInt(_blockKey) ?? 0;

  int itemCompletedMinutes(int index) {
    if (index < 0) return 0;
    final raw = prefs.getString(_itemMinutesKey);
    if (raw == null) return 0;
    try {
      final map = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      return (map['$index'] as num?)?.toInt().clamp(0, 1440).toInt() ?? 0;
    } catch (_) {
      return 0;
    }
  }

  Map<int, int> get itemCompletedMinutesMap {
    final raw = prefs.getString(_itemMinutesKey);
    if (raw == null) return <int, int>{};
    try {
      final map = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      return map.map((key, value) => MapEntry(int.tryParse(key) ?? -1, (value as num).toInt()))
        ..removeWhere((key, _) => key < 0);
    } catch (_) {
      return <int, int>{};
    }
  }

  Future<void> setPlanPosition(int index, int blockIndex) async {
    await prefs.setInt(_indexKey, index.clamp(0, 100000).toInt());
    await prefs.setInt(_blockKey, blockIndex.clamp(0, 100000).toInt());
  }

  Future<void> clearPlanPosition() async => setPlanPosition(0, 0);

  Future<void> addCompletedMinutes(int value) => _recordCompletion(value, null);

  Future<void> addItemCompletedMinutes(int index, int value) => _recordCompletion(value, index);

  Future<void> _recordCompletion(int value, int? itemIndex) async {
    final minutes = value.clamp(0, 1440).toInt();
    if (minutes <= 0) return;
    await prefs.setInt(_minutesKey, completedMinutes + minutes);
    await prefs.setInt(_planMinutesKey, planCompletedMinutes + minutes);
    await prefs.setInt(_xpKey, xp + minutes * 2);
    await prefs.setInt(_sessionsKey, sessions + 1);

    if (itemIndex != null && itemIndex >= 0) {
      final map = itemCompletedMinutesMap;
      map[itemIndex] = (map[itemIndex] ?? 0) + minutes;
      await prefs.setString(_itemMinutesKey, jsonEncode(map.map((key, value) => MapEntry(key.toString(), value))));
    }

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
