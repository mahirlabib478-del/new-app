import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/study_models.dart';
import 'app_language.dart';

class FocusTimerState {
  const FocusTimerState({required this.index, required this.blockIndex, required this.remainingSeconds, required this.running, this.deadlineMillis});
  final int index;
  final int blockIndex;
  final int remainingSeconds;
  final bool running;
  final int? deadlineMillis;
  Map<String, dynamic> toJson() => {'index': index, 'blockIndex': blockIndex, 'remainingSeconds': remainingSeconds, 'running': running, 'deadlineMillis': deadlineMillis};
  factory FocusTimerState.fromJson(Map<String, dynamic> json) => FocusTimerState(index: (json['index'] as num? ?? 0).clamp(0, 100000).toInt(), blockIndex: (json['blockIndex'] as num? ?? 0).clamp(0, 100000).toInt(), remainingSeconds: (json['remainingSeconds'] as num? ?? 0).clamp(0, 86400).toInt(), running: json['running'] as bool? ?? false, deadlineMillis: (json['deadlineMillis'] as num?)?.toInt());
}

class BreakTimerState {
  const BreakTimerState({required this.index, required this.blockIndex, required this.breakMinutes, required this.remainingSeconds, required this.running, this.deadlineMillis});
  final int index;
  final int blockIndex;
  final int breakMinutes;
  final int remainingSeconds;
  final bool running;
  final int? deadlineMillis;
  Map<String, dynamic> toJson() => {'index': index, 'blockIndex': blockIndex, 'breakMinutes': breakMinutes, 'remainingSeconds': remainingSeconds, 'running': running, 'deadlineMillis': deadlineMillis};
  factory BreakTimerState.fromJson(Map<String, dynamic> json) => BreakTimerState(index: (json['index'] as num? ?? 0).clamp(0, 100000).toInt(), blockIndex: (json['blockIndex'] as num? ?? 0).clamp(0, 100000).toInt(), breakMinutes: (json['breakMinutes'] as num? ?? 5).clamp(5, 10).toInt(), remainingSeconds: (json['remainingSeconds'] as num? ?? 0).clamp(0, 3600).toInt(), running: json['running'] as bool? ?? false, deadlineMillis: (json['deadlineMillis'] as num?)?.toInt());
}

class LocalStore {
  LocalStore(this.prefs);
  final SharedPreferences prefs;
  static const _planKey = 'study_plan';
  static const _planDateKey = 'study_plan_date';
  static const _minutesKey = 'completed_minutes';
  static const _planMinutesKey = 'plan_completed_minutes';
  static const _itemMinutesKey = 'item_completed_minutes';
  static const _xpKey = 'xp';
  static const _streakKey = 'streak';
  static const _sessionsKey = 'sessions';
  static const _lastStudyKey = 'last_study_date';
  static const _indexKey = 'current_plan_index';
  static const _blockKey = 'current_block_index';
  static const _themeKey = 'dark_mode';
  static const _themePresetKey = 'theme_preset';
  static const _historyKey = 'study_daily_history';
  static const _dailyGoalKey = 'daily_goal_minutes';
  static const _focusTimerKey = 'focus_timer_state';
  static const _breakTimerKey = 'break_timer_state';
  static const _savedSessionsKey = 'saved_study_sessions';
  static const _soundEffectsKey = 'sound_effects_enabled';
  static const _activeModeKey = 'active_study_mode';

  Future<void> setActiveStudyMode(String mode) async {
    final value = mode.trim();
    if (value.isEmpty) return;
    await prefs.setString(_activeModeKey, value);
  }

  String get activeStudyMode => prefs.getString(_activeModeKey) ?? 'Study';

  Future<void> savePlan(StudyPlan plan, {String? mode}) async {
    final outgoingMode = activeStudyMode;
    await _archiveCurrentPlan(mode: outgoingMode);
    await prefs.setString(_planKey, jsonEncode(plan.toJson()));
    await prefs.setString(_planDateKey, _dateKey(DateTime.now()));
    await prefs.setInt(_planMinutesKey, 0);
    await prefs.remove(_itemMinutesKey);
    await clearFocusTimerState();
    await clearBreakTimerState();
    await clearPlanPosition();
    if (mode != null) await setActiveStudyMode(mode);
  }

  Future<void> _archiveCurrentPlan({required String mode}) async {
    final raw = prefs.getString(_planKey);
    if (raw == null) return;
    final savedDate = prefs.getString(_planDateKey);
    if (savedDate != null && savedDate != _dateKey(DateTime.now())) return;
    final plan = loadPlan();
    if (plan == null || plan.items.isEmpty) return;
    final completed = planCompletedMinutes.clamp(0, plan.allocatedMinutes).toInt();
    if (completed >= plan.allocatedMinutes) return;
    final rawSessions = prefs.getString(_savedSessionsKey);
    List<dynamic> sessions = const [];
    try {
      final decoded = rawSessions == null ? const [] : jsonDecode(rawSessions);
      if (decoded is List) sessions = List<dynamic>.from(decoded);
    } catch (_) {}
    final fingerprint = jsonEncode(plan.toJson());
    String? existingId;
    sessions.removeWhere((value) {
      if (value is! Map) return false;
      final existingPlan = value['plan'];
      if (existingPlan is! Map) return false;
      if (jsonEncode(Map<String, dynamic>.from(existingPlan)) != fingerprint) return false;
      existingId = value['id'] as String? ?? existingId;
      return true;
    });
    final focus = focusTimerState;
    final focusMatchesPosition = focus != null && focus.index == currentPlanIndex && focus.blockIndex == currentBlockIndex;
    if (focusMatchesPosition) {
      sessions.add({
        'id': existingId ?? DateTime.now().microsecondsSinceEpoch.toString(),
        'mode': mode,
        'savedAt': DateTime.now().toIso8601String(),
        'plan': jsonDecode(raw),
        'itemProgress': itemCompletedMinutesMap.map((key, value) => MapEntry(key.toString(), value)),
        'planCompletedMinutes': completed,
        'currentIndex': currentPlanIndex,
        'currentBlockIndex': currentBlockIndex,
        'focusRemainingSeconds': focus.remainingSeconds,
        'focusRunning': focus.running,
        if (focus.deadlineMillis != null) 'focusDeadlineMillis': focus.deadlineMillis,
      });
    } else {
      sessions.add({
        'id': existingId ?? DateTime.now().microsecondsSinceEpoch.toString(),
        'mode': mode,
        'savedAt': DateTime.now().toIso8601String(),
        'plan': jsonDecode(raw),
        'itemProgress': itemCompletedMinutesMap.map((key, value) => MapEntry(key.toString(), value)),
        'planCompletedMinutes': completed,
        'currentIndex': currentPlanIndex,
        'currentBlockIndex': currentBlockIndex,
      });
    }
    await prefs.setString(_savedSessionsKey, jsonEncode(sessions));
  }

  StudyPlan? loadPlan() {
    final raw = prefs.getString(_planKey);
    if (raw == null) return null;
    final savedDate = prefs.getString(_planDateKey);
    final today = _dateKey(DateTime.now());
    if (savedDate != null && savedDate != today) return null;
    try {
      final json = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      final totalMinutes = ((json['totalMinutes'] as num?)?.toInt() ?? 0).clamp(0, 1440).toInt();
      final items = (json['items'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map((item) => StudyItem.fromJson(Map<String, dynamic>.from(item)))
          .where((item) => item.minutes >= 0)
          .toList();
      if (items.isEmpty) return null;
      final allocated = items.fold<int>(0, (sum, item) => sum + item.minutes);
      if (allocated > totalMinutes) return null;
      return StudyPlan(totalMinutes: totalMinutes, items: items);
    } catch (_) {
      return null;
    }
  }

  bool get darkMode => prefs.getBool(_themeKey) ?? true;
  Future<void> setDarkMode(bool value) => prefs.setBool(_themeKey, value);
  String get themePreset => (prefs.getString(_themePresetKey)?.isNotEmpty ?? false) ? prefs.getString(_themePresetKey)! : (darkMode ? 'midnight' : 'sunrise');
  Future<void> setThemePreset(String value) => prefs.setString(_themePresetKey, value);
  AppLanguage get appLanguage => AppLanguageStore(prefs).language;
  Future<void> setAppLanguage(AppLanguage value) => AppLanguageStore(prefs).setLanguage(value);
  bool get soundEffectsEnabled => prefs.getBool(_soundEffectsKey) ?? true;
  Future<void> setSoundEffectsEnabled(bool value) => prefs.setBool(_soundEffectsKey, value);
  int get completedMinutes => prefs.getInt(_minutesKey) ?? 0;
  int get planCompletedMinutes => prefs.getInt(_planMinutesKey) ?? 0;
  int get dailyGoalMinutes => (prefs.getInt(_dailyGoalKey) ?? 120).clamp(15, 720).toInt();
  int get xp => prefs.getInt(_xpKey) ?? 0;
  int get streak => prefs.getInt(_streakKey) ?? 0;
  int get sessions => prefs.getInt(_sessionsKey) ?? 0;
  int get level => (xp ~/ 250) + 1;
  int get levelProgress => xp % 250;
  int get currentPlanIndex => prefs.getInt(_indexKey) ?? 0;
  int get currentBlockIndex => prefs.getInt(_blockKey) ?? 0;
  Future<void> setDailyGoalMinutes(int value) async => prefs.setInt(_dailyGoalKey, value.clamp(15, 720).toInt());

  FocusTimerState? get focusTimerState {
    final raw = prefs.getString(_focusTimerKey);
    if (raw == null) return null;
    try {
      return FocusTimerState.fromJson(Map<String, dynamic>.from(jsonDecode(raw) as Map));
    } catch (_) {
      return null;
    }
  }
  Future<void> saveFocusTimerState(FocusTimerState state) async => prefs.setString(_focusTimerKey, jsonEncode(state.toJson()));
  Future<void> clearFocusTimerState() async => prefs.remove(_focusTimerKey);

  BreakTimerState? get breakTimerState {
    final raw = prefs.getString(_breakTimerKey);
    if (raw == null) return null;
    try {
      return BreakTimerState.fromJson(Map<String, dynamic>.from(jsonDecode(raw) as Map));
    } catch (_) {
      return null;
    }
  }
  Future<void> saveBreakTimerState(BreakTimerState state) async => prefs.setString(_breakTimerKey, jsonEncode(state.toJson()));
  Future<void> clearBreakTimerState() async => prefs.remove(_breakTimerKey);

  Map<String, int> get dailyStudyMinutes {
    final raw = prefs.getString(_historyKey);
    if (raw == null) return <String, int>{};
    try {
      final map = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      final result = <String, int>{};
      for (final entry in map.entries) {
        if (entry.value is num) result[entry.key] = entry.value.toInt().clamp(0, 1440).toInt();
      }
      return result;
    } catch (_) {
      return <String, int>{};
    }
  }

  int studyMinutesOn(DateTime date) => dailyStudyMinutes[_dateKey(date)] ?? 0;

  Future<void> addDailyStudyMinutes(int value, {DateTime? date}) async {
    final minutes = value.clamp(0, 1440).toInt();
    if (minutes <= 0) return;
    final key = _dateKey(date ?? DateTime.now());
    final history = dailyStudyMinutes;
    history[key] = ((history[key] ?? 0) + minutes).clamp(0, 1440).toInt();
    await prefs.setString(_historyKey, jsonEncode(history));
  }

  int itemCompletedMinutes(int index) {
    if (index < 0) return 0;
    final raw = prefs.getString(_itemMinutesKey);
    if (raw == null) return 0;
    try {
      final map = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      final value = map['$index'];
      return value is num ? value.toInt().clamp(0, 1440).toInt() : 0;
    } catch (_) {
      return 0;
    }
  }

  Map<int, int> get itemCompletedMinutesMap {
    final raw = prefs.getString(_itemMinutesKey);
    if (raw == null) return <int, int>{};
    try {
      final map = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      final result = <int, int>{};
      for (final entry in map.entries) {
        final index = int.tryParse(entry.key);
        final value = entry.value;
        if (index != null && index >= 0 && value is num && value >= 0) result[index] = value.toInt().clamp(0, 1440).toInt();
      }
      return result;
    } catch (_) {
      return <int, int>{};
    }
  }

  Future<void> setPlanPosition(int index, int blockIndex) async {
    await prefs.setInt(_indexKey, index.clamp(0, 100000).toInt());
    await prefs.setInt(_blockKey, blockIndex.clamp(0, 100000).toInt());
  }

  Future<void> clearPlanPosition() async {
    await prefs.remove(_indexKey);
    await prefs.remove(_blockKey);
  }

  Future<void> addCompletedMinutes(int value) => _recordCompletion(value, null);
  Future<void> addItemCompletedMinutes(int index, int value) => _recordCompletion(value, index);

  Future<void> _recordCompletion(int value, int? itemIndex) async {
    final requested = value.clamp(0, 1440).toInt();
    if (requested <= 0) return;
    final plan = loadPlan();
    final planBudget = plan?.allocatedMinutes ?? requested;
    final itemMap = plan == null ? const <int, int>{} : itemCompletedMinutesMap;
    if (itemIndex == null && itemMap.isNotEmpty) return;
    var completedBefore = planCompletedMinutes;
    if (itemIndex != null && plan != null && itemMap.isNotEmpty) completedBefore = plan.items.asMap().entries.fold<int>(0, (sum, entry) => sum + (itemMap[entry.key] ?? 0).clamp(0, entry.value.minutes).toInt());
    final planRemaining = (planBudget - completedBefore).clamp(0, 1440).toInt();
    if (planRemaining <= 0) return;
    if (itemIndex != null && (plan == null || itemIndex < 0 || itemIndex >= plan.items.length)) return;
    var minutes = requested > planRemaining ? planRemaining : requested;
    if (itemIndex != null && plan != null) {
      final itemBudget = plan.items[itemIndex].minutes;
      final itemBefore = (itemMap[itemIndex] ?? 0).clamp(0, itemBudget).toInt();
      final itemRemaining = (itemBudget - itemBefore).clamp(0, itemBudget).toInt();
      if (itemRemaining <= 0) return;
      if (minutes > itemRemaining) minutes = itemRemaining;
      final nextMap = <int, int>{...itemMap, itemIndex: itemBefore + minutes};
      await prefs.setString(_itemMinutesKey, jsonEncode(nextMap.map((key, value) => MapEntry(key.toString(), value))));
    }
    await prefs.setInt(_planMinutesKey, (completedBefore + minutes).clamp(0, planBudget).toInt());
    await prefs.setInt(_minutesKey, (completedMinutes + minutes).clamp(0, 1000000000).toInt());
    await addDailyStudyMinutes(minutes);
    final alreadyStudiedToday = prefs.getString(_lastStudyKey) == _dateKey(DateTime.now());
    await prefs.setInt(_xpKey, xp + minutes * 2);
    await prefs.setInt(_sessionsKey, sessions + 1);
    if (!alreadyStudiedToday) { await prefs.setInt(_streakKey, _nextStreak()); await prefs.setString(_lastStudyKey, _dateKey(DateTime.now())); }
  }

  int _nextStreak() {
    final previous = prefs.getString(_lastStudyKey);
    if (previous == null) return 1;
    final yesterday = _dateKey(DateTime.now().subtract(const Duration(days: 1)));
    return previous == yesterday ? streak + 1 : 1;
  }

  String _dateKey(DateTime date) => '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
