import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';
import 'focus_flow.dart';

class RegularStudyPlanner extends StatefulWidget {
  const RegularStudyPlanner({super.key, required this.store, required this.total, required this.subjects, this.onStartPlan});
  final LocalStore store;
  final int total;
  final List<String> subjects;
  final Future<void> Function(StudyPlan plan)? onStartPlan;
  @override State<RegularStudyPlanner> createState() => _RegularStudyPlannerState();
}

class _TopicEntry { _TopicEntry(this.name, this.minutes); String name; int minutes; }

class _RegularStudyPlannerState extends State<RegularStudyPlanner> {
  late final Map<String, List<_TopicEntry>> topics;
  late final Map<String, TextEditingController> controllers;
  late final Map<String, int> subjectMinutes;

  @override
  void initState() {
    super.initState();
    topics = {for (final s in widget.subjects) s: []};
    controllers = {for (final s in widget.subjects) s: TextEditingController()};
    final count = widget.subjects.length;
    final base = count == 0 ? 0 : widget.total ~/ count;
    final extra = count == 0 ? 0 : widget.total % count;
    subjectMinutes = {for (var i = 0; i < count; i++) widget.subjects[i]: base + (i < extra ? 1 : 0)};
  }

  @override
  void dispose() { for (final c in controllers.values) c.dispose(); super.dispose(); }

  int get allocated => subjectMinutes.values.fold(0, (a, b) => a + b);
  int get remaining => widget.total - allocated;

  void changeSubject(String subject, int value) {
    final others = allocated - subjectMinutes[subject]!;
    final max = widget.total - others;
    final next = value.clamp(0, max).toInt();
    final list = topics[subject]!;
    final topicTotal = list.fold(0, (a, t) => a + t.minutes);
    if (topicTotal > next && topicTotal > 0) {
      final ratio = next / topicTotal;
      var assigned = 0;
      for (var i = 0; i < list.length; i++) {
        final target = i == list.length - 1 ? next - assigned : (list[i].minutes * ratio).floor();
        list[i].minutes = target;
        assigned += target;
      }
    }
    setState(() => subjectMinutes[subject] = next);
  }

  void addTopic(String subject) {
    final c = controllers[subject]!;
    final name = c.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enter a chapter or topic name first.')));
      return;
    }
    if (topics[subject]!.any((t) => t.name.toLowerCase() == name.toLowerCase())) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('That topic is already added.')));
      return;
    }
    setState(() { topics[subject]!.add(_TopicEntry(name, 0)); c.clear(); });
  }

  void removeTopic(String subject, _TopicEntry topic) => setState(() => topics[subject]!.remove(topic));

  void changeTopic(String subject, _TopicEntry topic, int value) {
    final others = topics[subject]!.where((t) => !identical(t, topic)).fold(0, (a, t) => a + t.minutes);
    final max = subjectMinutes[subject]! - others;
    setState(() => topic.minutes = value.clamp(0, max).toInt());
  }

  void autoBalanceTopics(String subject) {
    final list = topics[subject]!;
    if (list.isEmpty) return;
    final total = subjectMinutes[subject]!;
    final base = total ~/ list.length;
    final extra = total % list.length;
    setState(() { for (var i = 0; i < list.length; i++) list[i].minutes = base + (i < extra ? 1 : 0); });
  }

  Future<void> save() async {
    final items = <StudyItem>[];
    var hasUnassigned = false;
    for (final subject in widget.subjects) {
      final subjectTotal = subjectMinutes[subject]!;
      final list = topics[subject]!;
      final topicAllocated = list.fold(0, (a, t) => a + t.minutes);
      final leftover = subjectTotal - topicAllocated;
      if (leftover < 0) return;
      if (leftover > 0) hasUnassigned = true;
      for (final topic in list) {
        if (topic.minutes > 0) items.add(StudyItem(title: subject, topic: topic.name, minutes: topic.minutes));
      }
      if (leftover > 0) items.add(StudyItem(title: subject, topic: list.isEmpty ? 'General study' : 'Other / review', minutes: leftover));
    }
    if (items.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Allocate at least 1 minute to a topic or subject.')));
      return;
    }
    if (hasUnassigned && mounted) {
      final proceed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Unassigned time'),
          content: const Text('Some subject time is not assigned to a named topic. It will be saved as General study or Other / review.'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Keep editing')),
            FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Save plan')),
          ],
        ),
      );
      if (proceed != true) return;
    }
    final plan = StudyPlan(totalMinutes: widget.total, items: items);
    await widget.store.savePlan(plan);
    if (!mounted) return;
    if (widget.onStartPlan != null) {
      await widget.onStartPlan!(plan);
      return;
    }
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: plan, index: 0, blockIndex: 0)));
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final totalDivisions = widget.total > 0 ? widget.total : null;
    return Scaffold(
      appBar: AppBar(title: const Text('Allocate by topic')),
      body: ListView(padding: const EdgeInsets.fromLTRB(20, 8, 20, 28), children: [
        Text('Build your focus map', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 6),
        const Text('Set each subject budget, then split that time across chapters or topics. The total can never exceed your original study time.'),
        const SizedBox(height: 16),
        Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text('Total budget', style: TextStyle(fontWeight: FontWeight.w900)), Text('${widget.total} min', style: const TextStyle(fontWeight: FontWeight.w900))]),
          const SizedBox(height: 10), LinearProgressIndicator(value: widget.total == 0 ? 0 : allocated / widget.total, minHeight: 9),
          const SizedBox(height: 8), Text(remaining == 0 ? 'Fully allocated — ready to focus.' : '$remaining minutes available to assign', style: TextStyle(fontWeight: FontWeight.w700, color: remaining == 0 ? scheme.primary : null)),
        ]))),
        const SizedBox(height: 14),
        ...widget.subjects.map((subject) => _subjectCard(subject, totalDivisions)),
        const SizedBox(height: 4),
        FilledButton.icon(onPressed: allocated == 0 ? null : save, icon: const Icon(Icons.play_arrow_rounded), label: Text(remaining == 0 ? 'Start focused study' : 'Start with $allocated min')),
      ],),
    );
  }

  Widget _subjectCard(String subject, int? totalDivisions) {
    final list = topics[subject]!;
    final assigned = list.fold(0, (a, t) => a + t.minutes);
    final leftover = subjectMinutes[subject]! - assigned;
    final subjectBudget = subjectMinutes[subject]!;
    final maxTopic = widget.total > 0 ? subjectBudget.clamp(1, widget.total).toInt() : 1;
    return Card(margin: const EdgeInsets.only(bottom: 12), child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [Expanded(child: Text(subject, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17))), Text('${subjectMinutes[subject]}m', style: const TextStyle(fontWeight: FontWeight.w900))]),
      Slider(value: subjectMinutes[subject]!.toDouble(), min: 0, max: widget.total.toDouble(), divisions: totalDivisions, onChanged: widget.total <= 0 ? null : (v) => changeSubject(subject, v.round())),
      Row(children: [Expanded(child: Text('Topics use $assigned min • $leftover min unassigned', style: Theme.of(context).textTheme.labelMedium)), if (list.isNotEmpty) TextButton.icon(onPressed: () => autoBalanceTopics(subject), icon: const Icon(Icons.balance_rounded, size: 18), label: const Text('Split evenly'))]),
      const SizedBox(height: 6),
      TextField(controller: controllers[subject], onSubmitted: (_) => addTopic(subject), textInputAction: TextInputAction.done, decoration: InputDecoration(labelText: 'Add chapter / topic', prefixIcon: const Icon(Icons.bookmark_outline_rounded), suffixIcon: IconButton(onPressed: () => addTopic(subject), tooltip: 'Add topic', icon: const Icon(Icons.add_rounded)))),
      if (list.isNotEmpty) ...[
        const SizedBox(height: 12),
        ...list.map((topic) => Padding(padding: const EdgeInsets.only(bottom: 8), child: Container(padding: const EdgeInsets.fromLTRB(12, 8, 8, 2), decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), border: Border.all(color: Theme.of(context).colorScheme.outlineVariant)), child: Column(children: [
          Row(children: [Expanded(child: Text(topic.name, style: const TextStyle(fontWeight: FontWeight.w700))), Text('${topic.minutes}m', style: const TextStyle(fontWeight: FontWeight.w800)), IconButton(onPressed: () => removeTopic(subject, topic), tooltip: 'Remove topic', icon: const Icon(Icons.close_rounded, size: 19))]),
          Slider(value: topic.minutes.clamp(0, maxTopic).toDouble(), min: 0, max: maxTopic.toDouble(), divisions: maxTopic, onChanged: (v) => changeTopic(subject, topic, v.round())),
        ])))),
      ],
    ])));
  }
}
