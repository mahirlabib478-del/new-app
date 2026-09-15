import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/study_models.dart';

class FocusTimerState {
  const FocusTimerState({
    required this.index,
    required this.blockIndex,
    required this.remainingSeconds,
    required this.running,
    this.deadlineMillis,
  });

  final int index;
  final int blockIndex;
  final int remainingSeconds;
  final bool running;
  final int? deadlineMillis;

  Map<String, dynamic> toJson() => {
        'index': index,
        'blockIndex': blockIndex,
        'remainingSeconds': remainingSeconds,
        'running': running,
        'deadlineMillis': deadlineMillis,
      };

  factory FocusTimerState.fromJson(Map<String, dynamic> json) => FocusTimerState(
        index: (json['index'] as num?)?.toInt() ?? 0,
        blockIndex: (json['blockIndex'] as num?)?.toInt() ?? 0,
        remainingSeconds: (json['remainingSeconds'] as num?)?.toInt() ?? 0,
        running: json['running'] as bool? ?? false,
        deadlineMillis: (json['deadlineMillis'] as num?)?.toInt(),
      );
}

class BreakTimerState {
  const BreakTimerState({
    required this.index,
    required this.blockIndex,
    required this.breakMinutes,
    required this.remainingSeconds,
    required this.running,
    this.deadlineMillis,
  });

  final int index;
  final int blockIndex;
  final int breakMinutes;
  final int remainingSeconds;
  final bool running;
  final int? deadlineMillis;

  Map<String, dynamic> toJson() => {
        'index': index,
        'blockIndex': blockIndex,
        'breakMinutes': breakMinutes,
        'remainingSeconds': remainingSeconds,
        'running': running,
        'deadlineMillis': deadlineMillis,
      };

  factory BreakTimerState.fromJson(Map<String, dynamic> json) => BreakTimerState(
        index: (json['index'] as num?)?.toInt() ?? 0,
        blockIndex: (json['blockIndex'] as num?)?.toInt() ?? 0,
        breakMinutes: (json['breakMinutes'] as num?)?.toInt() ?? 5,
        remainingSeconds: (json['remainingSeconds'] as num?)?.toInt() ?? 300,
        running: json['running'] as bool? ?? false,
        deadlineMillis: (json['deadlineMillis'] as num?)?.toInt(),
      );
}

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
  static const _historyKey = 'study_daily_history';
  static const _dailyGoalKey = 'daily_goal_minutes';
  static const _xpKey = 'study_xp';
  static const _streakKey = 'study_streak';
  static const _lastStudyKey = 'last_study_date';
  static const _sessionsKey = 'study_sessions';
  static const _indexKey = 'current_plan_index';
  static const _blockKey = 'current_block_index';
  static const _focusTimerKey = 'focus_timer_state';
  static const _breakTimerKey = 'break_timer_state';

  Future<void> savePlan(StudyPlan plan) async {
    await prefs.setString(_planKey, jsonEncode(plan.toJson()));
    await prefs.setString(_planDateKey, _dateKey(DateTime.now()));
    await prefs.setInt(_planMinutesKey, 0);
    await prefs.remove(_itemMinutesKey);
    await clearFocusTimerState();
    await clearBreakTimerState();
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
  int get dailyGoalMinutes => (prefs.getInt(_dailyGoalKey) ?? 120).clamp(15, 720).toInt();
  int get xp => prefs.getInt(_xpKey) ?? 0;
  int get streak => prefs.getInt(_streakKey) ?? 0;
  int get sessions => prefs.getInt(_sessionsKey) ?? 0;
  int get level => (xp ~/ 250) + 1;
  int get levelProgress => xp % 250;
  int get currentPlanIndex => prefs.getInt(_indexKey) ?? 0;
  int get currentBlockIndex => prefs.getInt(_blockKey) ?? 0;

  Future<void> setDailyGoalMinutes(int value) async {
    final goal = value.clamp(15, 720).toInt();
    await prefs.setInt(_dailyGoalKey, goal);
  }

  FocusTimerState? get focusTimerState {
    final raw = prefs.getString(_focusTimerKey);
    if (raw == null) return null;
    try {
      return FocusTimerState.fromJson(Map<String, dynamic>.from(jsonDecode(raw) as Map));
    } catch (_) {
      return null;
    }
  }

  Future<void> saveFocusTimerState(FocusTimerState state) async {
    await prefs.setString(_focusTimerKey, jsonEncode(state.toJson()));
  }

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

  Future<void> saveBreakTimerState(BreakTimerState state) async {
    await prefs.setString(_breakTimerKey, jsonEncode(state.toJson()));
  }

  Future<void> clearBreakTimerState() async => prefs.remove(_breakTimerKey);

  Map<String, int> get dailyStudyMinutes {
    final raw = prefs.getString(_historyKey);
    if (raw == null) return <String, int>{};
    try {
      final map = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      return map.map((key, value) => MapEntry(key, (value as num).toInt().clamp(0, 1440).toInt()));
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
    final requested = value.clamp(0, 1440).toInt();
    if (requested <= 0) return;

    final plan = loadPlan();
    final planBudget = plan?.allocatedMinutes ?? requested;
    final planRemaining = (planBudget - planCompletedMinutes).clamp(0, 1440).toInt();
    if (planRemaining <= 0) return;

    if (itemIndex != null && (plan == null || itemIndex < 0 || itemIndex >= plan.items.length)) return;

    var minutes = requested > planRemaining ? planRemaining : requested;
    if (itemIndex != null && plan != null) {
      final itemRemaining =
          (plan.items[itemIndex].minutes - itemCompletedMinutes(itemIndex)).clamp(0, 1440).toInt();
      if (itemRemaining <= 0) return;
      if (minutes > itemRemaining) minutes = itemRemaining;
    }
    if (minutes <= 0) return;

    await prefs.setInt(_minutesKey, completedMinutes + minutes);
    await prefs.setInt(_planMinutesKey, planCompletedMinutes + minutes);
    await prefs.setInt(_xpKey, xp + minutes * 2);
    await prefs.setInt(_sessionsKey, sessions + 1);
    await addDailyStudyMinutes(minutes);

    if (itemIndex != null) {
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
