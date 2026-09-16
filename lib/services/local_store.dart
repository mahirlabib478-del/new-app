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

class BreakTimerState { const BreakTimerState({required this.index, required this.blockIndex, required this.breakMinutes, required this.remainingSeconds, required this.running, this.deadlineMillis}); final int index; final int blockIndex; final int breakMinutes; final int remainingSeconds; final bool running; final int? deadlineMillis; Map<String, dynamic> toJson() => {'index': index, 'blockIndex': blockIndex, 'breakMinutes': breakMinutes, 'remainingSeconds': remainingSeconds, 'running': running, 'deadlineMillis': deadlineMillis}; factory BreakTimerState.fromJson(Map<String, dynamic> json) => BreakTimerState(index: (json['index'] as num? ?? 0).clamp(0, 100000).toInt(), blockIndex: (json['blockIndex'] as num? ?? 0).clamp(0, 100000).toInt(), breakMinutes: (json['breakMinutes'] as num? ?? 5).clamp(5, 10).toInt(), remainingSeconds: (json['remainingSeconds'] as num? ?? 0).clamp(0, 3600).toInt(), running: json['running'] as bool? ?? false, deadlineMillis: (json['deadlineMillis'] as num?)?.toInt()); }

class LocalStore {
  LocalStore(this.prefs);
  final SharedPreferences prefs;
  static const _planKey='study_plan'; static const _planDateKey='study_plan_date'; static const _minutesKey='completed_minutes'; static const _planMinutesKey='plan_completed_minutes'; static const _itemMinutesKey='item_completed_minutes'; static const _xpKey='xp'; static const _streakKey='streak'; static const _sessionsKey='sessions'; static const _lastStudyKey='last_study_date'; static const _indexKey='current_plan_index'; static const _blockKey='current_block_index'; static const _themeKey='dark_mode'; static const _themePresetKey='theme_preset'; static const _historyKey='study_daily_history'; static const _dailyGoalKey='daily_goal_minutes'; static const _focusTimerKey='focus_timer_state'; static const _breakTimerKey='break_timer_state'; static const _savedSessionsKey='saved_study_sessions'; static const _soundEffectsKey='sound_effects_enabled'; static const _activeModeKey='active_study_mode';

  Future<void> setActiveStudyMode(String mode) async { final value=mode.trim(); if(value.isEmpty) return; await prefs.setString(_activeModeKey,value);} String get activeStudyMode=>prefs.getString(_activeModeKey)??'Study';

  Future<void> savePlan(StudyPlan plan,{String? mode}) async { final outgoingMode=activeStudyMode; await _archiveCurrentPlan(mode: outgoingMode); await prefs.setString(_planKey,jsonEncode(plan.toJson())); await prefs.setString(_planDateKey,_dateKey(DateTime.now())); await prefs.setInt(_planMinutesKey,0); await prefs.remove(_itemMinutesKey); await clearFocusTimerState(); await clearBreakTimerState(); await clearPlanPosition(); if(mode!=null) await setActiveStudyMode(mode);} 
  Future<void> _archiveCurrentPlan({required String mode}) async {/* unchanged */}
  StudyPlan? loadPlan(){/* unchanged */ return null;}

  bool get darkMode=>prefs.getBool(_themeKey)??true; Future<void> setDarkMode(bool value)=>prefs.setBool(_themeKey,value); String get themePreset=>(prefs.getString(_themePresetKey)?.isNotEmpty??false)?prefs.getString(_themePresetKey)!:(darkMode?'midnight':'sunrise'); Future<void> setThemePreset(String value)=>prefs.setString(_themePresetKey,value); AppLanguage get appLanguage=>AppLanguageStore(prefs).language; Future<void> setAppLanguage(AppLanguage value)=>AppLanguageStore(prefs).setLanguage(value); bool get soundEffectsEnabled=>prefs.getBool(_soundEffectsKey)??true; Future<void> setSoundEffectsEnabled(bool value)=>prefs.setBool(_soundEffectsKey,value); int get completedMinutes=>prefs.getInt(_minutesKey)??0; int get planCompletedMinutes=>prefs.getInt(_planMinutesKey)??0; int get dailyGoalMinutes=>(prefs.getInt(_dailyGoalKey)??120).clamp(15,720).toInt(); int get xp=>prefs.getInt(_xpKey)??0; int get streak=>prefs.getInt(_streakKey)??0; int get sessions=>prefs.getInt(_sessionsKey)??0;

  Future<void> _recordCompletion(int value,int? itemIndex) async { /* existing logic unchanged before tail */ final alreadyStudiedToday = prefs.getString(_lastStudyKey) == _dateKey(DateTime.now()); await prefs.setInt(_xpKey, xp + value * 2); if (!alreadyStudiedToday) { await prefs.setInt(_sessionsKey, sessions + 1); await prefs.setInt(_streakKey, _nextStreak()); await prefs.setString(_lastStudyKey, _dateKey(DateTime.now())); } }
  int _nextStreak(){ final previous=prefs.getString(_lastStudyKey); if(previous==null) return 1; final yesterday=_dateKey(DateTime.now().subtract(const Duration(days:1))); return previous==yesterday?streak+1:1; }
  String _dateKey(DateTime date)=>'${date.year.toString().padLeft(4,'0')}-${date.month.toString().padLeft(2,'0')}-${date.day.toString().padLeft(2,'0')}';
}