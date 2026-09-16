import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/gamification.dart';
import '../services/local_store.dart';

class ProgressDashboard extends StatefulWidget {
  const ProgressDashboard({super.key, required this.store});
  final LocalStore store;
  @override State<ProgressDashboard> createState() => _ProgressDashboardState();
}

class _ProgressDashboardState extends State<ProgressDashboard> {
  Future<void> _changeGoal() async {
    final value = await showDialog<int>(context: context, builder: (_) => _DailyGoalDialog(initialGoal: widget.store.dailyGoalMinutes));
    if (value == null) return;
    await widget.store.setDailyGoalMinutes(value);
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final store = widget.store;
    final plan = store.loadPlan();
    final planned = plan?.allocatedMinutes ?? 0;
    final completedByItem = plan == null ? const <int, int>{} : store.itemCompletedMinutesMap;
    final itemCompletedTotal = plan == null ? 0 : plan.items.asMap().entries.fold<int>(0, (sum, entry) => sum + (completedByItem[entry.key] ?? 0).clamp(0, entry.value.minutes).toInt());
    final aggregateCompleted = store.planCompletedMinutes.clamp(0, planned).toInt();
    final completed = planned <= 0 ? 0 : completedByItem.isNotEmpty ? itemCompletedTotal.clamp(0, planned).toInt() : aggregateCompleted;
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0).toDouble();
    final goal = store.dailyGoalMinutes;
    final today = store.studyMinutesOn(DateTime.now());
    final goalProgress = (today / goal).clamp(0.0, 1.0).toDouble();
    final week = List.generate(7, (offset) {
      final date = DateTime.now().subtract(Duration(days: 6 - offset));
      return (date, store.studyMinutesOn(date));
    });
    final weekTotal = week.fold<int>(0, (sum, entry) => sum + entry.$2);
    final levelProgress = store.levelProgress / 250;
    final nextLevelXp = (store.level * 250) - store.xp;
    final achievements = Gamification(store).achievements();

    return Scaffold(
      appBar: AppBar(title: const Text('Progress')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text('Your momentum', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 18),
          Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [const Expanded(child: Text("Today's goal", style: TextStyle(fontWeight: FontWeight.w900))), Text('$today / $goal min', style: const TextStyle(fontWeight: FontWeight.w900)), IconButton(onPressed: _changeGoal, tooltip: 'Change goal', icon: const Icon(Icons.edit_rounded))]),
            const SizedBox(height: 10), LinearProgressIndicator(value: goalProgress, minHeight: 9), const SizedBox(height: 8),
            Text(goalProgress >= 1 ? 'Goal reached. Keep the momentum.' : '${goal - today > 0 ? goal - today : 0} min left to reach today\'s goal'),
          ]))),
          const SizedBox(height: 12),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Last 7 days', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 4), Text('$weekTotal focused minutes this week')])), const Icon(Icons.bar_chart_rounded)]),
            const SizedBox(height: 18), SizedBox(height: 150, child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [for (final entry in week) Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: _DayBar(date: entry.$1, minutes: entry.$2, goal: goal)))])),
          ]))),
          const SizedBox(height: 12),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('$completed / $planned min', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 12), LinearProgressIndicator(value: progress, minHeight: 9), const SizedBox(height: 10), Text('${(progress * 100).round()}% plan complete')]))),
          const SizedBox(height: 12),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(children: [
            Row(children: [CircleAvatar(radius: 24, child: Text('${store.level}', style: const TextStyle(fontWeight: FontWeight.w900))), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Level ${store.level}', style: const TextStyle(fontWeight: FontWeight.w900)), Text(nextLevelXp <= 0 ? 'Level complete' : '$nextLevelXp XP to next level')])), Text('${store.levelProgress}/250 XP', style: const TextStyle(fontWeight: FontWeight.w800))]),
            const SizedBox(height: 12), LinearProgressIndicator(value: levelProgress.clamp(0.0, 1.0).toDouble(), minHeight: 7),
          ]))),
          const SizedBox(height: 12),
          Row(children: [Expanded(child: _Stat(icon: Icons.local_fire_department_rounded, value: '${store.streak}', label: 'Streak')), const SizedBox(width: 10), Expanded(child: _Stat(icon: Icons.bolt_rounded, value: '${store.xp}', label: 'XP')), const SizedBox(width: 10), Expanded(child: _Stat(icon: Icons.timer_rounded, value: '${store.completedMinutes}', label: 'Minutes'))]),
          const SizedBox(height: 20), Text('Achievements', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 10),
          ...achievements.map((achievement) => _Achievement(icon: _achievementIcon(achievement.title), title: achievement.title, subtitle: achievement.description, unlocked: achievement.unlocked)),
          const SizedBox(height: 18),
          if (plan != null) ...plan.items.asMap().entries.map((entry) => _ProgressItem(item: entry.value, completed: store.itemCompletedMinutes(entry.key))),
        ],
      ),
    );
  }

  IconData _achievementIcon(String title) {
    switch (title) {
      case 'First Focus': return Icons.play_arrow_rounded;
      case '1 Hour': return Icons.timer_rounded;
      case '5 Sessions': return Icons.repeat_rounded;
      case '3 Day Streak': return Icons.local_fire_department_rounded;
      case '500 XP': return Icons.workspace_premium_rounded;
      default: return Icons.emoji_events_rounded;
    }
  }
}

class _DailyGoalDialog extends StatefulWidget {
  const _DailyGoalDialog({required this.initialGoal});
  final int initialGoal;
  @override State<_DailyGoalDialog> createState() => _DailyGoalDialogState();
}

class _DailyGoalDialogState extends State<_DailyGoalDialog> {
  late final TextEditingController controller;
  @override void initState() { super.initState(); controller = TextEditingController(text: '${widget.initialGoal}'); }
  @override void dispose() { controller.dispose(); super.dispose(); }
  @override Widget build(BuildContext context) => AlertDialog(title: const Text('Daily study goal'), content: TextField(controller: controller, autofocus: true, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Minutes', suffixText: 'min')), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')), FilledButton(onPressed: () => Navigator.pop(context, int.tryParse(controller.text.trim())), child: const Text('Save'))]);
}

class _DayBar extends StatelessWidget {
  const _DayBar({required this.date, required this.minutes, required this.goal});
  final DateTime date;
  final int minutes;
  final int goal;
  @override Widget build(BuildContext context) {
    final ratio = goal <= 0 ? 0.0 : (minutes / goal).clamp(0.0, 1.0).toDouble();
    final height = 16 + ratio * 92;
    final isToday = DateUtils.isSameDay(date, DateTime.now());
    return Column(mainAxisAlignment: MainAxisAlignment.end, children: [Text('$minutes', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800)), const SizedBox(height: 5), AnimatedContainer(duration: const Duration(milliseconds: 300), height: minutes == 0 ? 10 : height, decoration: BoxDecoration(color: isToday ? Theme.of(context).colorScheme.primary : Theme.of(context).colorScheme.primaryContainer, borderRadius: BorderRadius.circular(10))), const SizedBox(height: 6), Text(['M', 'T', 'W', 'T', 'F', 'S', 'S'][date.weekday - 1], style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800))]);
  }
}

class _Achievement extends StatelessWidget {
  const _Achievement({required this.icon, required this.title, required this.subtitle, required this.unlocked});
  final IconData icon;
  final String title;
  final String subtitle;
  final bool unlocked;
  @override Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(leading: CircleAvatar(child: Icon(icon)), title: Text(title, style: const TextStyle(fontWeight: FontWeight.w900)), subtitle: Text(subtitle), trailing: Icon(unlocked ? Icons.check_circle_rounded : Icons.lock_outline_rounded)));
}

class _ProgressItem extends StatelessWidget {
  const _ProgressItem({required this.item, required this.completed});
  final StudyItem item;
  final int completed;
  @override Widget build(BuildContext context) {
    final done = completed.clamp(0, item.minutes).toInt();
    final progress = item.minutes <= 0 ? 0.0 : (done / item.minutes).clamp(0.0, 1.0).toDouble();
    return Card(margin: const EdgeInsets.only(bottom: 10), child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Row(children: [Expanded(child: Text(item.title, style: const TextStyle(fontWeight: FontWeight.w900))), Text('$done/${item.minutes}m')]), if (item.topic.isNotEmpty) Text(item.topic), const SizedBox(height: 10), LinearProgressIndicator(value: progress)])));
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.icon, required this.value, required this.label});
  final IconData icon;
  final String value;
  final String label;
  @override Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(12), child: Column(children: [Icon(icon), const SizedBox(height: 6), Text(value, style: const TextStyle(fontWeight: FontWeight.w900)), Text(label)])));
}
