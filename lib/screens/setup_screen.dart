import 'package:flutter/material.dart';

import '../models/study_models.dart';
import '../services/local_store.dart';
import 'regular_study_planner.dart';

class Setup extends StatefulWidget {
  const Setup({super.key, required this.store, this.onStartPlan});

  final LocalStore store;
  final Future<void> Function(StudyPlan plan)? onStartPlan;

  @override
  State<Setup> createState() => _SetupState();
}

class _SetupState extends State<Setup> {
  static const _defaultSubjects = <String>[
    'Mathematics',
    'Physics',
    'Chemistry',
    'Biology',
    'English',
    'Computer Science',
  ];

  final _hoursController = TextEditingController(text: '2');
  final _customSubjectController = TextEditingController();
  late final List<String> _subjects;
  final _selected = <String>{};

  @override
  void initState() {
    super.initState();
    _subjects = [..._defaultSubjects];
  }

  @override
  void dispose() {
    _hoursController.dispose();
    _customSubjectController.dispose();
    super.dispose();
  }

  int get totalMinutes {
    final hours = double.tryParse(_hoursController.text.trim().replaceAll(',', '.')) ?? 0;
    return (hours * 60).round().clamp(0, 24 * 60).toInt();
  }

  void _toggleSubject(String subject, bool value) {
    setState(() {
      if (value) {
        _selected.add(subject);
      } else {
        _selected.remove(subject);
      }
    });
  }

  void _addCustomSubject() {
    final name = _customSubjectController.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a subject name first.')),
      );
      return;
    }

    final exists = _subjects.any((subject) => subject.toLowerCase() == name.toLowerCase());
    if (exists) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('That subject is already in the list.')),
      );
      return;
    }

    setState(() {
      _subjects.add(name);
      _selected.add(name);
      _customSubjectController.clear();
    });
  }

  void _removeCustomSubject(String subject) {
    if (_defaultSubjects.contains(subject)) return;
    setState(() {
      _subjects.remove(subject);
      _selected.remove(subject);
    });
  }

  Future<void> _continue() async {
    FocusManager.instance.primaryFocus?.unfocus();
    final total = totalMinutes;
    if (_selected.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select at least one subject.')),
      );
      return;
    }
    if (total <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter at least 1 hour of study time. You can use values such as 1.5.')),
      );
      return;
    }

    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => RegularStudyPlanner(
          store: widget.store,
          total: total,
          subjects: _selected.toList(growable: false),
          onStartPlan: widget.onStartPlan,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final total = totalMinutes;
    return Scaffold(
      appBar: AppBar(title: const Text('Plan your study')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        children: [
          Text(
            'Start with the big picture',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w900,
                ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Choose subjects and set the total study time first. You will allocate that exact budget across topics next.',
          ),
          const SizedBox(height: 18),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'TOTAL STUDY TIME',
                    style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _hoursController,
                    keyboardType: TextInputType.number,
                    textInputAction: TextInputAction.done,
                    onChanged: (_) => setState(() {}),
                    decoration: const InputDecoration(
                      labelText: 'Study hours',
                      suffixText: 'hours',
                      prefixIcon: Icon(Icons.schedule_rounded),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    total > 0 ? '$total minutes available' : 'Enter your total time',
                    style: TextStyle(
                      color: total > 0 ? scheme.primary : scheme.onSurfaceVariant,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.fromLTRB(8, 4, 8, 8),
                    child: Text(
                      'SUBJECTS',
                      style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1),
                    ),
                  ),
                  ..._subjects.map(
                    (subject) => CheckboxListTile(
                      value: _selected.contains(subject),
                      onChanged: (value) => _toggleSubject(subject, value ?? false),
                      title: Text(subject, style: const TextStyle(fontWeight: FontWeight.w700)),
                      secondary: _defaultSubjects.contains(subject)
                          ? null
                          : IconButton(
                              onPressed: () => _removeCustomSubject(subject),
                              tooltip: 'Remove custom subject',
                              icon: const Icon(Icons.close_rounded),
                            ),
                      controlAffinity: ListTileControlAffinity.leading,
                      contentPadding: const EdgeInsets.symmetric(horizontal: 4),
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Divider(),
                  const SizedBox(height: 8),
                  const Text(
                    'ADD YOUR OWN SUBJECT',
                    style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.0),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _customSubjectController,
                    textInputAction: TextInputAction.done,
                    onSubmitted: (_) => _addCustomSubject(),
                    decoration: InputDecoration(
                      labelText: 'Custom subject',
                      hintText: 'e.g. Accounting, History, IELTS',
                      prefixIcon: const Icon(Icons.add_circle_outline_rounded),
                      suffixIcon: IconButton(
                        onPressed: _addCustomSubject,
                        tooltip: 'Add subject',
                        icon: const Icon(Icons.add_rounded),
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Custom subjects are added to this plan and start selected automatically.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _continue,
            icon: const Icon(Icons.arrow_forward_rounded),
            label: const Text('Allocate topics'),
          ),
        ],
      ),
    );
  }
}
