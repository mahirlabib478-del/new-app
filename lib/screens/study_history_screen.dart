import 'package:flutter/material.dart';

import '../services/local_store.dart';

class StudyHistoryScreen extends StatelessWidget {
  const StudyHistoryScreen({super.key, required this.store});
  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    final todayStart = DateTime(today.year, today.month, today.day);
    final days = List.generate(30, (index) => todayStart.subtract(Duration(days: index)));
    final entries = days.map((date) => (date: date, minutes: store.studyMinutesOn(date))).toList();
    final total = entries.fold<int>(0, (sum, entry) => sum + entry.minutes);
    final activeDays = entries.where((entry) => entry.minutes > 0).length;
    final goal = store.dailyGoalMinutes;
    final goalDays = entries.where((entry) => entry.minutes >= goal).length;
    final best = entries.fold<int>(0, (best, entry) => entry.minutes > best ? entry.minutes : best);
    final recent = entries.take(7).toList();
    final recentTotal = recent.fold<int>(0, (sum, entry) => sum + entry.minutes);
    final average = activeDays == 0 ? 0 : (total / activeDays).round();

    return Scaffold(
      appBar: AppBar(title: const Text('Study history')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          Text(
            'Your study timeline',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 6),
          Text('A clear view of the work you have logged recently.'),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Last 7 days', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 4),
                  Text('$recentTotal focused minutes'),
                  const SizedBox(height: 16),
                  SizedBox(
                    height: 110,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        for (final entry in recent.reversed)
                          Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 3), child: _HistoryBar(entry: entry, goal: goal))),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Wrap(
                alignment: WrapAlignment.spaceBetween,
                runSpacing: 18,
                children: [
                  _Summary(value: '${total}m', label: '30-day total'),
                  _Summary(value: '$activeDays', label: 'Active days'),
                  _Summary(value: '$goalDays', label: 'Goals reached'),
                  _Summary(value: '${average}m', label: 'Active-day avg'),
                  _Summary(value: '${best}m', label: 'Best day'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(child: Text('Daily record', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900))),
              Text('$goal min goal', style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
          const SizedBox(height: 10),
          if (activeDays == 0)
            Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 34),
                child: Column(
                  children: [
                    const Icon(Icons.auto_graph_rounded, size: 42),
                    const SizedBox(height: 12),
                    const Text('No study time logged yet', style: TextStyle(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 6),
                    Text('Complete a focus block and your daily record will appear here.', textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
                  ],
                ),
              ),
            )
          else
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
          Text('History is based on completed study minutes stored on this device.', style: Theme.of(context).textTheme.bodySmall),
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

class _HistoryBar extends StatelessWidget {
  const _HistoryBar({required this.entry, required this.goal});
  final ({DateTime date, int minutes}) entry;
  final int goal;

  @override
  Widget build(BuildContext context) {
    final ratio = goal <= 0 ? 0.0 : (entry.minutes / goal).clamp(0.0, 1.0).toDouble();
    final height = entry.minutes == 0 ? 8.0 : 14 + ratio * 70;
    final isToday = DateUtils.isSameDay(entry.date, DateTime.now());
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text('${entry.minutes}', style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w800)),
        const SizedBox(height: 4),
        AnimatedContainer(
          duration: const Duration(milliseconds: 250),
          height: height,
          decoration: BoxDecoration(
            color: isToday ? Theme.of(context).colorScheme.primary : Theme.of(context).colorScheme.primaryContainer,
            borderRadius: BorderRadius.circular(8),
          ),
        ),
        const SizedBox(height: 5),
        Text(['M', 'T', 'W', 'T', 'F', 'S', 'S'][entry.date.weekday - 1], style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w800)),
      ],
    );
  }
}

class _Summary extends StatelessWidget {
  const _Summary({required this.value, required this.label});
  final String value;
  final String label;

  @override
  Widget build(BuildContext context) => SizedBox(
        width: 82,
        child: Column(
          children: [
            Text(value, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
            const SizedBox(height: 4),
            Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 11)),
          ],
        ),
      );
}
