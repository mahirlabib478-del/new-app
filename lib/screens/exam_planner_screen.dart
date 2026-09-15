import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';

class ExamPlannerScreen extends StatefulWidget {
  const ExamPlannerScreen({super.key, required this.nextDay, required this.store, this.onStartPlan});

  final bool nextDay;
  final LocalStore store;
  final void Function(StudyPlan plan)? onStartPlan;

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

  List<StudyItem> _generatePlan() {
    final totalMinutes = studyHours * 60;
    if (subjects.isEmpty) return [];

    final urgencyBoost = urgency == 3 ? 0.35 : urgency == 2 ? 0.15 : 0.0;
    final weights = subjects.map((subject) {
      final priority = priorities[subject] ?? 2;
      final nextDayBoost = widget.nextDay && priority == 3 ? 0.25 : 0.0;
      return priority.toDouble() + urgencyBoost * priority + nextDayBoost;
    }).toList();
    final weightTotal = weights.fold<double>(0, (sum, value) => sum + value);
    final blocks = totalMinutes ~/ 25;
    final remainder = totalMinutes % 25;
    final counts = List<int>.filled(subjects.length, 0);

    for (var i = 0; i < subjects.length; i++) {
      counts[i] = (blocks * weights[i] / weightTotal).floor();
    }

    var assigned = counts.fold(0, (sum, value) => sum + value);
    final ranked = List<int>.generate(subjects.length, (i) => i)
      ..sort((a, b) => weights[b].compareTo(weights[a]));
    var cursor = 0;
    while (assigned < blocks) {
      counts[ranked[cursor % ranked.length]]++;
      assigned++;
      cursor++;
    }

    final items = <StudyItem>[];
    for (var i = 0; i < subjects.length; i++) {
      for (var b = 0; b < counts[i]; b++) {
        items.add(StudyItem(
          title: subjects[i],
          minutes: 25,
          topic: widget.nextDay ? 'High-impact revision' : 'Exam preparation',
        ));
      }
    }
    if (remainder > 0) {
      items.add(StudyItem(
        title: subjects[ranked.first],
        minutes: remainder,
        topic: widget.nextDay ? 'Final review' : 'Flexible review',
      ));
    }
    return items;
  }

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
                    widget.onStartPlan?.call(plan);
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
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.nextDay ? 'Next Day Exam' : 'Exam Preparation';
    final description = widget.nextDay
        ? 'Turn tonight into a high-impact revision sprint for tomorrow.'
        : 'Build a focused plan using available time, subject priority and exam urgency.';

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [Theme.of(context).colorScheme.primaryContainer, Theme.of(context).colorScheme.secondaryContainer],
              ),
              borderRadius: BorderRadius.circular(24),
            ),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                CircleAvatar(child: Icon(widget.nextDay ? Icons.bolt_rounded : Icons.auto_awesome_rounded)),
                const SizedBox(width: 12),
                Text(widget.nextDay ? 'FINAL SPRINT' : 'EXAM MODE', style: const TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1)),
              ]),
              const SizedBox(height: 14),
              Text(widget.nextDay ? 'Win tomorrow.' : 'Study with a plan.', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 6),
              Text(description),
            ]),
          ),
          const SizedBox(height: 18),
          _sectionCard(
            context,
            title: 'AVAILABLE STUDY TIME',
            child: Column(children: [
              Text('$studyHours hours', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w900)),
              Slider(value: studyHours.toDouble(), min: 1, max: 12, divisions: 11, label: '$studyHours h', onChanged: (value) => setState(() => studyHours = value.round())),
              const Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text('1h'), Text('6h'), Text('12h')]),
            ]),
          ),
          const SizedBox(height: 14),
          _sectionCard(
            context,
            title: 'EXAM URGENCY',
            child: SegmentedButton<int>(
              expandedInsets: EdgeInsets.zero,
              segments: const [
                ButtonSegment(value: 1, label: Text('Low'), icon: Icon(Icons.schedule_rounded)),
                ButtonSegment(value: 2, label: Text('Medium'), icon: Icon(Icons.timelapse_rounded)),
                ButtonSegment(value: 3, label: Text('High'), icon: Icon(Icons.priority_high_rounded)),
              ],
              selected: {urgency},
              onSelectionChanged: (selection) => setState(() => urgency = selection.first),
            ),
          ),
          const SizedBox(height: 18),
          Text('SUBJECTS', style: Theme.of(context).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w900, letterSpacing: 1.1)),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: TextField(controller: controller, onSubmitted: (_) => addSubject(), decoration: const InputDecoration(hintText: 'Add a subject, e.g. Physics', prefixIcon: Icon(Icons.menu_book_rounded)))),
            const SizedBox(width: 8),
            IconButton.filled(onPressed: addSubject, icon: const Icon(Icons.add_rounded)),
          ]),
          const SizedBox(height: 10),
          if (subjects.isEmpty)
            const Card(child: Padding(padding: EdgeInsets.all(18), child: Row(children: [Icon(Icons.lightbulb_outline_rounded), SizedBox(width: 12), Expanded(child: Text('Add at least one subject. You can change its priority after adding it.'))])))
          else
            ...subjects.map((subject) => Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: CircleAvatar(child: Text('${priorities[subject] ?? 2}')),
                    title: Text(subject, style: const TextStyle(fontWeight: FontWeight.w800)),
                    subtitle: Text('${_priorityLabel(priorities[subject] ?? 2)} priority'),
                    trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                      PopupMenuButton<int>(
                        initialValue: priorities[subject],
                        tooltip: 'Set priority',
                        onSelected: (value) => setState(() => priorities[subject] = value),
                        itemBuilder: (_) => const [
                          PopupMenuItem(value: 3, child: Text('High priority')),
                          PopupMenuItem(value: 2, child: Text('Medium priority')),
                          PopupMenuItem(value: 1, child: Text('Low priority')),
                        ],
                      ),
                      IconButton(onPressed: () => removeSubject(subject), icon: const Icon(Icons.delete_outline_rounded)),
                    ]),
                  ),
                )),
          const SizedBox(height: 16),
          if (subjects.isNotEmpty)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(children: [
                  const Icon(Icons.tune_rounded),
                  const SizedBox(width: 12),
                  Expanded(child: Text(widget.nextDay ? 'Tip: mark only truly important subjects as High so the final sprint stays focused.' : 'Tip: use High for subjects that need the most revision time.')),
                ]),
              ),
            ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: subjects.isEmpty ? null : _showGeneratedPlan,
            icon: const Icon(Icons.auto_awesome_rounded),
            label: const Padding(padding: EdgeInsets.all(14), child: Text('Generate exam plan')),
          ),
        ],
      ),
    );
  }

  Widget _sectionCard(BuildContext context, {required String title, required Widget child}) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.0)),
          const SizedBox(height: 10),
          child,
        ]),
      ),
    );
  }
}
