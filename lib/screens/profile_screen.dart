import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/app_language.dart';
import '../services/local_store.dart';
import '../services/reminder_coordinator.dart';
import '../services/reminder_settings.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.store,
    required this.prefs,
    required this.themeKey,
    required this.onThemeChanged,
    required this.language,
    required this.onLanguageChanged,
    this.reminderCoordinator,
  });

  final LocalStore store;
  final SharedPreferences prefs;
  final String themeKey;
  final Future<void> Function(String key) onThemeChanged;
  final AppLanguage language;
  final Future<void> Function(AppLanguage language) onLanguageChanged;
  final ReminderCoordinator? reminderCoordinator;

  static const _presets = <String, _ThemeOption>{
    'midnight': _ThemeOption('Midnight', Icons.nights_stay_rounded, 'Deep focus, low visual noise'),
    'ocean': _ThemeOption('Ocean Dark', Icons.water_rounded, 'Cool and calm'),
    'forest': _ThemeOption('Forest', Icons.forest_rounded, 'Natural and grounded'),
    'sunrise': _ThemeOption('Sunrise', Icons.wb_sunny_rounded, 'Warm and bright'),
    'ocean_light': _ThemeOption('Ocean', Icons.water_drop_rounded, 'Fresh and focused'),
    'mint': _ThemeOption('Mint', Icons.spa_rounded, 'Soft and refreshing'),
    'rose': _ThemeOption('Rose', Icons.local_florist_rounded, 'Warm and gentle'),
    'peach': _ThemeOption('Peach', Icons.wb_sunny_outlined, 'Friendly and energetic'),
    'lavender': _ThemeOption('Lavender', Icons.auto_awesome_rounded, 'Calm and creative'),
    'sky': _ThemeOption('Sky', Icons.cloud_rounded, 'Light and airy'),
  };

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  late ReminderSettings settings;
  bool? notificationsEnabled;

  AppStrings get strings => AppStrings(widget.language);

  @override
  void initState() {
    super.initState();
    settings = ReminderSettingsStore(widget.prefs).settings;
    WidgetsBinding.instance.addPostFrameCallback((_) => _refreshNotificationStatus());
  }

  Future<void> _refreshNotificationStatus() async {
    final enabled = await widget.reminderCoordinator.areNotificationsEnabled();
    if (mounted) setState(() => notificationsEnabled = enabled);
  }

  Future<void> _saveSettings(ReminderSettings next) async {
    // Reflect the user's choice immediately. Persistence and notification sync
    // happen afterwards so a slow platform/network operation cannot make a tap
    // look ignored.
    if (mounted) setState(() => settings = next);

    try {
      await ReminderSettingsStore(widget.prefs).save(next);
      await _syncReminders(settingsToSync: next);
      await _refreshNotificationStatus();
    } on Exception {
      // Reminder failures must never block settings changes or normal app use.
    }
  }

  Future<void> _syncReminders({required ReminderSettings settingsToSync}) async {
    final anyEnabled = settingsToSync.studyEnabled || settingsToSync.breakEnabled || settingsToSync.planEnabled;
    if (!anyEnabled) {
      await widget.reminderCoordinator!.sync(settingsOverride: settingsToSync);
      return;
    }

    try {
      // Settings can start enabled from persisted defaults, so a time change
      // or another reminder action must also be able to recover a missing
      // runtime grant.
      if (anyEnabled) {
        final granted = await widget.reminderCoordinator!.requestPermissions();
        if (!granted) return;
      }
      await widget.reminderCoordinator!.sync(settingsOverride: settingsToSync);
    } on Exception {
      // Reminder failures must never block settings changes or normal app use.
    }
  }

  Future<void> _pickTime({required bool study}) async {
    final initial = TimeOfDay(
      hour: study ? settings.studyHour : settings.planHour,
      minute: study ? settings.studyMinute : settings.planMinute,
    );
    final picked = await showTimePicker(context: context, initialTime: initial);
    if (picked == null) return;
    await _saveSettings(study
        ? settings.copyWith(studyHour: picked.hour, studyMinute: picked.minute)
        : settings.copyWith(planHour: picked.hour, planMinute: picked.minute));
  }

  String _formatTime(int hour, int minute) => TimeOfDay(hour: hour, minute: minute).format(context);

  Future<void> _editGoal(BuildContext context, int current) async {
    final value = await showDialog<int>(
      context: context,
      builder: (dialogContext) => _DailyGoalDialog(initialMinutes: current, strings: strings),
    );
    if (value == null) return;
    await widget.store.setDailyGoalMinutes(value);
    if (!mounted) return;
    setState(() {});
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(strings.dailyGoalUpdated)));
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final goal = widget.store.dailyGoalMinutes;
    final tapColor = scheme.primary.withValues(alpha: 0.18);
    return Scaffold(
      appBar: AppBar(title: Text(strings.profile)),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 6, 20, 24),
        children: [
          Card(
            color: scheme.primaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 30,
                    backgroundColor: scheme.surface,
                    child: Icon(Icons.person_rounded, color: scheme.primary, size: 30),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Study OS',
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                fontWeight: FontWeight.w900,
                                color: scheme.onPrimaryContainer,
                              ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          strings.buildConsistency,
                          style: TextStyle(color: scheme.onPrimaryContainer),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 18, 18, 8),
                  child: Text(strings.appearance.toUpperCase(), style: const TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1)),
                ),
                ...ProfileScreen._presets.entries.map(
                  (entry) => ListTile(
                    onTap: () => widget.onThemeChanged(entry.key),
                    splashColor: tapColor,
                    enableFeedback: true,
                    leading: Icon(entry.value.icon),
                    title: Text(entry.value.name, style: const TextStyle(fontWeight: FontWeight.w800)),
                    subtitle: Text(entry.value.description),
                    trailing: Icon(
                      widget.themeKey == entry.key ? Icons.radio_button_checked_rounded : Icons.radio_button_unchecked_rounded,
                      color: widget.themeKey == entry.key ? scheme.primary : scheme.outline,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.translate_rounded),
                  title: Text(strings.languageLabel, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text(strings.languageName),
                  trailing: const Icon(Icons.chevron_right_rounded),
                  splashColor: tapColor,
                  enableFeedback: true,
                  onTap: () => _pickLanguage(context),
                ),
                ListTile(
                  leading: const Icon(Icons.flag_rounded),
                  title: Text(strings.dailyGoal, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text('$goal ${strings.focusedStudy}'),
                  trailing: const Icon(Icons.chevron_right_rounded),
                  splashColor: tapColor,
                  enableFeedback: true,
                  onTap: () => _editGoal(context, goal),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (notificationsEnabled == false)
                  const Padding(
                    padding: EdgeInsets.fromLTRB(18, 14, 18, 2),
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: Icon(Icons.notifications_off_rounded),
                      title: Text('Notifications are blocked', style: TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: Text('Allow notifications in Android Settings to receive study and break reminders.'),
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 18, 18, 4),
                  child: Text(strings.reminders.toUpperCase(), style: const TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1)),
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.menu_book_rounded),
                  title: Text(strings.studyReminder, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text('${strings.studyTime}: ${_formatTime(settings.studyHour, settings.studyMinute)}'),
                  value: settings.studyEnabled,
                  onChanged: (value) => _saveSettings(settings.copyWith(studyEnabled: value)),
                ),
                ListTile(
                  enabled: settings.studyEnabled,
                  leading: const Icon(Icons.schedule_rounded),
                  title: Text(strings.studyTime),
                  trailing: TextButton(
                    onPressed: settings.studyEnabled ? () => _pickTime(study: true) : null,
                    child: Text(_formatTime(settings.studyHour, settings.studyMinute)),
                  ),
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.coffee_rounded),
                  title: Text(strings.breakReminder, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text(strings.afterFocusBlock),
                  value: settings.breakEnabled,
                  onChanged: (value) => _saveSettings(settings.copyWith(breakEnabled: value)),
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.event_note_rounded),
                  title: Text(strings.planReminder, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text('${strings.planTime}: ${_formatTime(settings.planHour, settings.planMinute)}'),
                  value: settings.planEnabled,
                  onChanged: (value) => _saveSettings(settings.copyWith(planEnabled: value)),
                ),
                ListTile(
                  enabled: settings.planEnabled,
                  leading: const Icon(Icons.schedule_rounded),
                  title: Text(strings.planTime),
                  trailing: TextButton(
                    onPressed: settings.planEnabled ? () => _pickTime(study: false) : null,
                    child: Text(_formatTime(settings.planHour, settings.planMinute)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _pickLanguage(BuildContext context) async {
    final selected = await showDialog<AppLanguage>(
      context: context,
      builder: (dialogContext) => SimpleDialog(
        title: Text(strings.languageLabel),
        children: [
          RadioGroup<AppLanguage>(
            groupValue: widget.language,
            onChanged: (value) => Navigator.pop(dialogContext, value),
            child: Column(
              children: [
                RadioListTile<AppLanguage>(value: AppLanguage.english, title: Text(strings.english)),
                RadioListTile<AppLanguage>(value: AppLanguage.bangla, title: Text(strings.bangla)),
              ],
            ),
          ),
        ],
      ),
    );
    if (selected != null) await widget.onLanguageChanged(selected);
  }
}

class _DailyGoalDialog extends StatefulWidget {
  const _DailyGoalDialog({required this.initialMinutes, required this.strings});
  final int initialMinutes;
  final AppStrings strings;
  @override
  State<_DailyGoalDialog> createState() => _DailyGoalDialogState();
}

class _DailyGoalDialogState extends State<_DailyGoalDialog> {
  late final TextEditingController controller;
  @override
  void initState() { super.initState(); controller = TextEditingController(text: widget.initialMinutes.toString()); }
  @override
  void dispose() { controller.dispose(); super.dispose(); }
  void save() { final parsed = int.tryParse(controller.text.trim()); if (parsed == null) return; Navigator.pop(context, parsed); }
  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text(widget.strings.dailyStudyGoal),
        content: TextField(controller: controller, keyboardType: TextInputType.number, decoration: InputDecoration(labelText: widget.strings.minutes, suffixText: 'min'), onSubmitted: (_) => save()),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: Text(widget.strings.cancel)),
          FilledButton(onPressed: save, child: Text(widget.strings.save)),
        ],
      );
}

class _ThemeOption {
  const _ThemeOption(this.name, this.icon, this.description);
  final String name;
  final IconData icon;
  final String description;
}
