import 'package:flutter/material.dart';

import '../services/local_store.dart';

class StudyHistoryScreen extends StatelessWidget {
  const StudyHistoryScreen({super.key, required this.store});
  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final days = List.generate(30, (index) => DateTime(now.year, now.month, now.day).subtract(Duration(days: index)));
    final entries = days.map((date) => (date: date, minutes: store.studyMinutesOn(date))).toList();
    final total = entries.fold<int>(0, (sum, entry) => sum + entry.minutes);
    final activeDays = entries.where((entry) => entry.minutes > 0).length;
    final best = entries.fold<int>(0, (best, entry) => entry.minutes > best ? entry.minutes : best);
    final goal = store.dailyGoalMinutes;

    return Scaffold(
      appBar: AppBar(title: const Text('Study history')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Row(
                children: [
                  Expanded(child: _Summary(value: '${total}m', label: 'Last 30 days')),
                  Expanded(child: _Summary(value: '$activeDays', label: 'Active days')),
                  Expanded(child: _Summary(value: '${best}m', label: 'Best day')),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Text('Daily record', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 10),
          Card(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Column(
                children: entries.map((entry) {
                  final reached = entry.minutes >= goal;
                  final ratio = goal <= 0 ? 0.0 : (entry.minutes / goal).clamp(0.0, 1.0).toDouble();
                  return ListTile(
                    leading: CircleAvatar(
                      child: Text('${entry.date.day}', style: const TextStyle(fontWeight: FontWeight.w800)),
                    ),
                    title: Text(_formatDate(entry.date), style: const TextStyle(fontWeight: FontWeight.w800)),
                    subtitle: Padding(
                      padding: const EdgeInsets.only(top: 7),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(99),
                        child: LinearProgressIndicator(value: ratio, minHeight: 6),
                      ),
                    ),
                    trailing: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text('${entry.minutes} min', style: const TextStyle(fontWeight: FontWeight.w900)),
                        if (reached) const Icon(Icons.check_circle_rounded, size: 17),
                      ],
                    ),
                  );
                }).toList(),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text('Goal reference: $goal min/day', style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }

  String _formatDate(DateTime date) {
    final today = DateTime.now();
    if (DateUtils.isSameDay(date, today)) return 'Today';
    if (DateUtils.isSameDay(date, today.subtract(const Duration(days: 1)))) return 'Yesterday';
    const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    return '${weekdays[date.weekday - 1]}, ${date.day}/${date.month}';
  }
}

class _Summary extends StatelessWidget {
  const _Summary({required this.value, required this.label});
  final String value;
  final String label;

  @override
  Widget build(BuildContext context) => Column(
        children: [
          Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
          const SizedBox(height: 4),
          Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12)),
        ],
      );
}
