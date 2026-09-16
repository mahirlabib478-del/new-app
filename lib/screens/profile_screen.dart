import 'package:flutter/material.dart';

import '../services/local_store.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.store,
    required this.themeKey,
    required this.onThemeChanged,
  });

  final LocalStore store;
  final String themeKey;
  final Future<void> Function(String key) onThemeChanged;

  static const _presets = <String, _ThemeOption>{
    'midnight': _ThemeOption('Midnight', Icons.nights_stay_rounded, 'Deep focus, low visual noise'),
    'ocean': _ThemeOption('Ocean', Icons.water_rounded, 'Cool and calm'),
    'forest': _ThemeOption('Forest', Icons.forest_rounded, 'Natural and grounded'),
    'sunrise': _ThemeOption('Sunrise', Icons.wb_sunny_rounded, 'Warm and bright'),
  };

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  Future<void> _editGoal(BuildContext context, int current) async {
    final controller = TextEditingController(text: current.toString());
    final value = await showDialog<int>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Daily study goal'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Minutes', suffixText: 'min'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              final parsed = int.tryParse(controller.text.trim());
              if (parsed == null) return;
              Navigator.pop(dialogContext, parsed);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (value == null) return;
    await widget.store.setDailyGoalMinutes(value);
    if (!mounted) return;
    setState(() {});
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Daily goal updated.')));
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final goal = widget.store.dailyGoalMinutes;
    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 30,
                    backgroundColor: scheme.primaryContainer,
                    child: Icon(Icons.person_rounded, color: scheme.onPrimaryContainer, size: 30),
                  ),
                  const SizedBox(width: 14),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Study OS', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
                        SizedBox(height: 4),
                        Text('Build consistency. Focus deeply.'),
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
                const Padding(
                  padding: EdgeInsets.fromLTRB(18, 18, 18, 8),
                  child: Text('APPEARANCE', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1)),
                ),
                ...ProfileScreen._presets.entries.map(
                  (entry) => ListTile(
                    onTap: () => widget.onThemeChanged(entry.key),
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
            child: ListTile(
              leading: const Icon(Icons.flag_rounded),
              title: const Text('Daily goal', style: TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text('$goal minutes of focused study'),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: () => _editGoal(context, goal),
            ),
          ),
        ],
      ),
    );
  }
}

class _ThemeOption {
  const _ThemeOption(this.name, this.icon, this.description);
  final String name;
  final IconData icon;
  final String description;
}
