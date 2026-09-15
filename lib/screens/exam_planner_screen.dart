import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';

List<StudyItem> generateExamPlan({
  required bool nextDay,
  required int studyHours,
  required int urgency,
  required List<String> subjects,
  required Map<String, int> priorities,
}) {
  if (subjects.isEmpty || studyHours <= 0) return const [];

  final totalMinutes = studyHours * 60;
  final ranked = [...subjects]
    ..sort((a, b) => (priorities[b] ?? 2).compareTo(priorities[a] ?? 2));
  final safeUrgency = urgency.clamp(1, 3).toInt();
  final weights = subjects
      .map((subject) => (priorities[subject] ?? 2).clamp(1, 3).toInt() + safeUrgency - 1)
      .toList();
  final totalWeight = weights.fold<int>(0, (sum, weight) => sum + weight);
  final blockCount = totalMinutes ~/ 25;
  final counts = List<int>.filled(subjects.length, 0);
  final fractions = List<double>.filled(subjects.length, 0);

  if (blockCount > 0 && totalWeight > 0) {
    var assignedBlocks = 0;
    for (var i = 0; i < subjects.length; i++) {
      final exact = blockCount * weights[i] / totalWeight;
      counts[i] = exact.floor();
      fractions[i] = exact - counts[i];
      assignedBlocks += counts[i];
    }

    var remainingBlocks = blockCount - assignedBlocks;
    final order = List<int>.generate(subjects.length, (index) => index)
      ..sort((a, b) {
        final fractionCompare = fractions[b].compareTo(fractions[a]);
        return fractionCompare != 0 ? fractionCompare : a.compareTo(b);
      });
    for (var i = 0; i < remainingBlocks; i++) {
      counts[order[i % order.length]]++;
    }
  }

  final items = <StudyItem>[];
  for (var i = 0; i < subjects.length; i++) {
    for (var b = 0; b < counts[i]; b++) {
      items.add(StudyItem(
        title: subjects[i],
        minutes: 25,
        topic: nextDay ? 'High-impact revision' : 'Exam preparation',
      ));
    }
  }

  final remainder = totalMinutes % 25;
  if (remainder > 0) {
    items.add(StudyItem(
      title: ranked.first,
      minutes: remainder,
      topic: nextDay ? 'Final review' : 'Flexible review',
    ));
  }
  return items;
}

class ExamPlannerScreen extends StatefulWidget {
  const ExamPlannerScreen({super.key, required this.nextDay, required this.store, this.onStartPlan});

  final bool nextDay;
  final LocalStore store;
  final Future<void> Function(StudyPlan plan)? onStartPlan;

  @override
  State<ExamPlannerScreen> createState() => _ExamPlannerScreenState();
}

class _ExamPlannerScreenState extends State<ExamPlannerScreen> {
  int studyHours = 4;
  int urgency = 2;
  final subjects = <String>[];
  final controller = TextEditingController();
  final priorities = <String, int>{};

  void addSubject() {
    final value = controller.text.trim();
    if (value.isEmpty || subjects.any((subject) => subject.toLowerCase() == value.toLowerCase())) return;
    setState(() {
      subjects.add(value);
      priorities[value] = widget.nextDay ? 3 : 2;
      controller.clear();
    });
  }

  void removeSubject(String subject) {
    setState(() {
      subjects.remove(subject);
      priorities.remove(subject);
    });
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  List<StudyItem> _generatePlan() => generateExamPlan(
        nextDay: widget.nextDay,
        studyHours: studyHours,
        urgency: urgency,
        subjects: subjects,
        priorities: priorities,
      );

  Future<void> _showGeneratedPlan() async {
    final items = _generatePlan();
    if (items.isEmpty) return;
    final plan = StudyPlan(totalMinutes: studyHours * 60, items: items);
    if (!mounted) return;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.nextDay ? 'Tomorrow is ready' : 'Your exam plan is ready',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 6),
                Text('${studyHours}h planned • ${items.length} focus blocks • 25-minute cycles'),
                const SizedBox(height: 16),
                ...subjects.map((subject) {
                  final minutes = items.where((item) => item.title == subject).fold<int>(0, (sum, item) => sum + item.minutes);
                  final priority = priorities[subject] ?? 2;
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
                      leading: CircleAvatar(child: Text('$minutes')),
                      title: Text(subject, style: const TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: Text('${_priorityLabel(priority)} priority • ${_blockCount(minutes)} focus blocks'),
                      trailing: const Text('min', style: TextStyle(fontWeight: FontWeight.w700)),
                    ),
                  );
                }),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Row(
                    children: [
                      Icon(widget.nextDay ? Icons.bolt_rounded : Icons.auto_awesome_rounded),
                      const SizedBox(width: 10),
                      Expanded(child: Text(widget.nextDay ? 'High-priority subjects get more focus blocks.' : 'Priority and exam urgency shape the distribution.')),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                FilledButton.icon(
                  onPressed: () async {
                    await widget.store.savePlan(plan);
                    if (!sheetContext.mounted) return;
                    Navigator.pop(sheetContext);
                    if (widget.onStartPlan != null) {
                      await widget.onStartPlan!(plan);
                    }
                  },
                  icon: const Icon(Icons.play_arrow_rounded),
                  label: const SizedBox(width: double.infinity, child: Center(child: Text('Start exam plan'))),
                ),
                TextButton(
                  onPressed: () async {
                    await widget.store.savePlan(plan);
                    if (!sheetContext.mounted) return;
                    Navigator.pop(sheetContext);
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Plan saved. You can resume it from Home.')));
                    }
                  },
                  child: const SizedBox(width: double.infinity, child: Center(child: Text('Save & continue later'))),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _priorityLabel(int value) => value == 3 ? 'High' : value == 2 ? 'Medium' : 'Low';

  int _blockCount(int minutes) => (minutes / 25).ceil();

  @override
  Widget build(BuildContext context) {
    final title = widget.nextDay ? 'Next Day Exam' : 'Exam Preparation';
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(widget.nextDay ? 'FINAL SPRINT' : 'EXAM MODE', style: Theme.of(context).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w900, letterSpacing: 1.4)),
          const SizedBox(height: 6),
          Text(widget.nextDay ? 'Focus on the highest-impact work for tomorrow.' : 'Build a priority-weighted plan without overthinking the schedule.', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 22),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Available study time', style: TextStyle(fontWeight: FontWeight.w800)),
                  const SizedBox(height: 8),
                  Text('$studyHours hour${studyHours == 1 ? '' : 's'}', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
                  Slider(value: studyHours.toDouble(), min: 1, max: 12, divisions: 11, label: '$studyHours h', onChanged: (value) => setState(() => studyHours = value.round())),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Urgency', style: TextStyle(fontWeight: FontWeight.w800)),
                  const SizedBox(height: 8),
                  SegmentedButton<int>(
                    segments: const [
                      ButtonSegment(value: 1, label: Text('Low')),
                      ButtonSegment(value: 2, label: Text('Medium')),
                      ButtonSegment(value: 3, label: Text('High')),
                    ],
                    selected: {urgency},
                    onSelectionChanged: (value) => setState(() => urgency = value.first),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Text('Subjects', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(child: TextField(controller: controller, onSubmitted: (_) => addSubject(), decoration: const InputDecoration(hintText: 'e.g. Physics', prefixIcon: Icon(Icons.menu_book_rounded)))),
              const SizedBox(width: 8),
              IconButton.filled(onPressed: addSubject, icon: const Icon(Icons.add_rounded)),
            ],
          ),
          const SizedBox(height: 10),
          ...subjects.map((subject) => Card(
                margin: const EdgeInsets.only(bottom: 8),
                child: ListTile(
                  title: Text(subject, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: DropdownButton<int>(
                    value: priorities[subject] ?? 2,
                    isExpanded: true,
                    underline: const SizedBox.shrink(),
                    items: const [
                      DropdownMenuItem(value: 1, child: Text('Low priority')),
                      DropdownMenuItem(value: 2, child: Text('Medium priority')),
                      DropdownMenuItem(value: 3, child: Text('High priority')),
                    ],
                    onChanged: (value) => setState(() => priorities[subject] = value ?? 2),
                  ),
                  trailing: IconButton(onPressed: () => removeSubject(subject), icon: const Icon(Icons.close_rounded)),
                ),
              )),
          const SizedBox(height: 18),
          FilledButton.icon(onPressed: subjects.isEmpty ? null : _showGeneratedPlan, icon: const Icon(Icons.auto_awesome_rounded), label: const Text('Generate plan')),
        ],
      ),
    );
  }
}
