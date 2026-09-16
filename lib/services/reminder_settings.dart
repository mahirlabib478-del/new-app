import 'package:shared_preferences/shared_preferences.dart';

class ReminderSettings {
  const ReminderSettings({
    required this.studyEnabled,
    required this.breakEnabled,
    required this.planEnabled,
    required this.studyHour,
    required this.studyMinute,
    required this.planHour,
    required this.planMinute,
  });

  final bool studyEnabled;
  final bool breakEnabled;
  final bool planEnabled;
  final int studyHour;
  final int studyMinute;
  final int planHour;
  final int planMinute;

  static const defaults = ReminderSettings(
    studyEnabled: false,
    breakEnabled: true,
    planEnabled: true,
    studyHour: 19,
    studyMinute: 0,
    planHour: 9,
    planMinute: 0,
  );

  ReminderSettings copyWith({
    bool? studyEnabled,
    bool? breakEnabled,
    bool? planEnabled,
    int? studyHour,
    int? studyMinute,
    int? planHour,
    int? planMinute,
  }) {
    return ReminderSettings(
      studyEnabled: studyEnabled ?? this.studyEnabled,
      breakEnabled: breakEnabled ?? this.breakEnabled,
      planEnabled: planEnabled ?? this.planEnabled,
      studyHour: studyHour ?? this.studyHour,
      studyMinute: studyMinute ?? this.studyMinute,
      planHour: planHour ?? this.planHour,
      planMinute: planMinute ?? this.planMinute,
    );
  }
}

class ReminderSettingsStore {
  const ReminderSettingsStore(this.prefs);

  final SharedPreferences prefs;

  static const _studyEnabledKey = 'reminder_study_enabled';
  static const _breakEnabledKey = 'reminder_break_enabled';
  static const _planEnabledKey = 'reminder_plan_enabled';
  static const _studyHourKey = 'reminder_study_hour';
  static const _studyMinuteKey = 'reminder_study_minute';
  static const _planHourKey = 'reminder_plan_hour';
  static const _planMinuteKey = 'reminder_plan_minute';

  ReminderSettings get settings => ReminderSettings(
        studyEnabled: prefs.getBool(_studyEnabledKey) ?? ReminderSettings.defaults.studyEnabled,
        breakEnabled: prefs.getBool(_breakEnabledKey) ?? ReminderSettings.defaults.breakEnabled,
        planEnabled: prefs.getBool(_planEnabledKey) ?? ReminderSettings.defaults.planEnabled,
        studyHour: _bounded(prefs.getInt(_studyHourKey), ReminderSettings.defaults.studyHour, 0, 23),
        studyMinute: _bounded(prefs.getInt(_studyMinuteKey), ReminderSettings.defaults.studyMinute, 0, 59),
        planHour: _bounded(prefs.getInt(_planHourKey), ReminderSettings.defaults.planHour, 0, 23),
        planMinute: _bounded(prefs.getInt(_planMinuteKey), ReminderSettings.defaults.planMinute, 0, 59),
      );

  Future<void> save(ReminderSettings value) async {
    await prefs.setBool(_studyEnabledKey, value.studyEnabled);
    await prefs.setBool(_breakEnabledKey, value.breakEnabled);
    await prefs.setBool(_planEnabledKey, value.planEnabled);
    await prefs.setInt(_studyHourKey, value.studyHour.clamp(0, 23).toInt());
    await prefs.setInt(_studyMinuteKey, value.studyMinute.clamp(0, 59).toInt());
    await prefs.setInt(_planHourKey, value.planHour.clamp(0, 23).toInt());
    await prefs.setInt(_planMinuteKey, value.planMinute.clamp(0, 59).toInt());
  }

  static int _bounded(int? value, int fallback, int min, int max) =>
      (value ?? fallback).clamp(min, max).toInt();
}
