import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';

class ExamPlannerScreen extends StatefulWidget {
  const ExamPlannerScreen({super.key, required this.nextDay, required this.store});
  final bool nextDay;
  final LocalStore store;
  @override State<ExamPlannerScreen> createState() => _ExamPlannerScreenState();
}

class _ExamPlannerScreenState extends State<ExamPlannerScreen> {
  int studyHours = 4;
  int urgency = 2;
  final subjects = <String>[];
  final controller = TextEditingController();
  final priorities = <String, int>{};

  void addSubject() {
    final value = controller.text.trim();
    if (value.isNotEmpty && !subjects.contains(value)) {
      setState(() {
        subjects.add(value);
        priorities[value] = 2;
        controller.clear();
      });
    }
  }

  List<StudyItem> _generatePlan() {
    final totalMinutes = studyHours * 60;
    if (subjects.isEmpty) return [];

    final weights = subjects.map((s) {
      final priority = priorities[s] ?? 2;
      final urgencyBoost = urgency == 3 ? 0.35 : urgency == 2 ? 0.15 : 0.0;
      return priority.toDouble() + urgencyBoost * priority;
    }).toList();

    final weightTotal = weights.fold<double>(0, (a, b) => a + b);
    final blocks = totalMinutes ~/ 25;
    final remainder = totalMinutes % 25;
    final blockCounts = List<int>.filled(subjects.length, 0);

    for (var i = 0; i < subjects.length; i++) {
      blockCounts[i] = ((blocks * weights[i] / weightTotal).floor());
    }

    var assigned = blockCounts.fold(0, (a, b) => a + b);
    final ranked = List<int>.generate(subjects.length, (i) => i)
      ..sort((a, b) => weights[b].compareTo(weights[a]));
    var cursor = 0;
    while (assigned < blocks) {
      blockCounts[ranked[cursor % ranked.length]]++;
      assigned++;
      cursor++;
    }

    final items = <StudyItem>[];
    for (var i = 0; i < subjects.length; i++) {
      for (var b = 0; b < blockCounts[i]; b++) {
        items.add(StudyItem(
          title: subjects[i],
          minutes: 25,
          topic: widget.nextDay ? 'High-impact revision' : 'Exam preparation',
        ));
      }
    }
    if (remainder > 0) {
      final target = ranked.first;
      items.add(StudyItem(
        title: subjects[target],
        minutes: remainder,
        topic: widget.nextDay ? 'Final review' : 'Flexible review',
      ));
    }
    return items;
  }

  void _showGeneratedPlan() {
    final items = _generatePlan();
    final plan = StudyPlan(totalMinutes: studyHours * 60, items: items);
    widget.store.savePlan(plan);
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Your plan is ready', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 6),
            Text('${items.length} focus blocks • ${studyHours}h total • 25-minute focus cycles'),
            const SizedBox(height: 16),
            ...subjects.map((s) {
              final minutes = items.where((i) => i.title == s).fold<int>(0, (sum, i) => sum + i.minutes);
              return ListTile(
                contentPadding: EdgeInsets.zero,
                leading: CircleAvatar(child: Text('$minutes')),
                title: Text(s, style: const TextStyle(fontWeight: FontWeight.w800)),
                subtitle: Text('Priority: ${priorities[s] == 3 ? 'High' : priorities[s] == 2 ? 'Medium' : 'Low'}'),
                trailing: const Text('min'),
              );
            }),
            const SizedBox(height: 8),
            FilledButton.icon(
              onPressed: () => Navigator.pop(sheetContext),
              icon: const Icon(Icons.check_rounded),
              label: const SizedBox(width: double.infinity, child: Center(child: Text('Save & continue later'))),
            ),
          ]),
        ),
      ),
    );
  }

  @override
  void dispose() { controller.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    final title = widget.nextDay ? 'Next Day Exam' : 'Exam Preparation';
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        Text(widget.nextDay ? 'Win tomorrow' : 'Build your exam plan', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 8),
        Text(widget.nextDay ? 'Prioritize the most important subjects and use every focused block wisely.' : 'Set your available time, then rank subjects by importance.'),
        const SizedBox(height: 24),
        Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('AVAILABLE STUDY TIME', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1)),
          Center(child: Text('$studyHours hours', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w900))),
          Slider(value: studyHours.toDouble(), min: 1, max: 12, divisions: 11, onChanged: (v) => setState(() => studyHours = v.round())),
        ]))),
        const SizedBox(height: 16),
        Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('EXAM URGENCY', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1)),
          const SizedBox(height: 10),
          SegmentedButton<int>(segments: const [ButtonSegment(value: 1, label: Text('Low')), ButtonSegment(value: 2, label: Text('Medium')), ButtonSegment(value: 3, label: Text('High'))], selected: {urgency}, onSelectionChanged: (s) => setState(() => urgency = s.first)),
        ]))),
        const SizedBox(height: 16),
        const Text('SUBJECTS', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1)),
        const SizedBox(height: 8),
        Row(children: [Expanded(child: TextField(controller: controller, onSubmitted: (_) => addSubject(), decoration: const InputDecoration(hintText: 'e.g. Physics', border: OutlineInputBorder()))), const SizedBox(width: 8), IconButton.filled(onPressed: addSubject, icon: const Icon(Icons.add))]),
        const SizedBox(height: 8),
        ...subjects.map((subject) => Card(child: ListTile(leading: const Icon(Icons.menu_book_rounded), title: Text(subject, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text('Priority: ${priorities[subject] == 3 ? 'High' : priorities[subject] == 2 ? 'Medium' : 'Low'}'), trailing: PopupMenuButton<int>(initialValue: priorities[subject], onSelected: (v) => setState(() => priorities[subject] = v), itemBuilder: (_) => const [PopupMenuItem(value: 3, child: Text('High priority')), PopupMenuItem(value: 2, child: Text('Medium priority')), PopupMenuItem(value: 1, child: Text('Low priority'))])))),
        const SizedBox(height: 24),
        FilledButton.icon(onPressed: subjects.isEmpty ? null : _showGeneratedPlan, icon: const Icon(Icons.auto_awesome_rounded), label: const Padding(padding: EdgeInsets.all(14), child: Text('Generate exam plan'))),
      ]),
    );
  }
}
