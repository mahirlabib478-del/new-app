import 'package:flutter/material.dart';

import '../services/local_store.dart';

class StudyHistoryScreen extends StatelessWidget {
  const StudyHistoryScreen({super.key, required this.store});

  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    final todayStart = DateTime(today.year, today.month, today.day);
    final days = List.generate(
      30,
      (index) => todayStart.subtract(Duration(days: index)),
    );
    final entries = days
        .map((date) => (date: date, minutes: store.studyMinutesOn(date)))
        .toList();
    final goal = store.dailyGoalMinutes;
    final total = entries.fold<int>(0, (sum, entry) => sum + entry.minutes);
    final activeDays = entries.where((entry) => entry.minutes > 0).length;
    final goalDays = entries.where((entry) => goal > 0 && entry.minutes >= goal).length;
    final best = entries.fold<int>(0, (best, entry) => entry.minutes > best ? entry.minutes : best);
    final recent = entries.take(7).toList().reversed.toList();
    final recentTotal = recent.fold<int>(0, (sum, entry) => sum + entry.minutes);
    final average = activeDays == 0 ? 0 : (total / activeDays).round();
    final streak = _currentStreak(entries);
    final hasStudy = activeDays > 0;

    return Scaffold(
      appBar: AppBar(title: const Text('Study history')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
        children: [
          Text(
            'Your study timeline',
            style: Theme.of(context)
                .textTheme
                .headlineSmall
                ?.copyWith(fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 6),
          Text(
            'See your focus rhythm, daily progress, and recent milestones in one place.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 18),
          _OverviewCard(
            total: total,
            activeDays: activeDays,
            streak: streak,
            best: best,
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          'Last 7 days',
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.w900),
                        ),
                      ),
                      Text(
                        '$recentTotal min',
                        style: Theme.of(context)
                            .textTheme
                            .labelLarge
                            ?.copyWith(fontWeight: FontWeight.w800),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    goal > 0 ? 'Daily goal: $goal min' : 'Set a daily goal to track targets',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 18),
                  SizedBox(
                    height: 132,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        for (final entry in recent)
                          Expanded(
                            child: Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 3),
                              child: _HistoryBar(entry: entry, goal: goal),
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Wrap(
                spacing: 12,
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
          const SizedBox(height: 22),
          Row(
            children: [
              Expanded(
                child: Text(
                  'Daily timeline',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.w900),
                ),
              ),
              if (goal > 0)
                Text('$goal min goal', style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
          const SizedBox(height: 10),
          if (!hasStudy)
            _EmptyHistoryCard(),
          if (hasStudy)
            Card(
              clipBehavior: Clip.antiAlias,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
                child: Column(
                  children: [
                    for (var index = 0; index < entries.length; index++)
                      _TimelineRow(
                        entry: entries[index],
                        goal: goal,
                        isLast: index == entries.length - 1,
                      ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: 14),
          Text(
            'History is based on completed study minutes stored on this device.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }

  int _currentStreak(List<({DateTime date, int minutes})> entries) {
    var streak = 0;
    for (final entry in entries) {
      if (entry.minutes <= 0) break;
      streak++;
    }
    return streak;
  }
}

class _OverviewCard extends StatelessWidget {
  const _OverviewCard({
    required this.total,
    required this.activeDays,
    required this.streak,
    required this.best,
  });

  final int total;
  final int activeDays;
  final int streak;
  final int best;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 24,
                  backgroundColor: scheme.primaryContainer,
                  child: Icon(Icons.insights_rounded, color: scheme.onPrimaryContainer),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Your rhythm',
                        style: Theme.of(context)
                            .textTheme
                            .titleMedium
                            ?.copyWith(fontWeight: FontWeight.w900),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        streak == 0
                            ? 'Start a focus block today to begin a streak.'
                            : '$streak day${streak == 1 ? '' : 's'} of study in a row.',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(child: _OverviewMetric(value: '${total}m', label: '30 days')),
                Expanded(child: _OverviewMetric(value: '$activeDays', label: 'Active days')),
                Expanded(child: _OverviewMetric(value: '$bestm', label: 'Best day')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _OverviewMetric extends StatelessWidget {
  const _OverviewMetric({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
          const SizedBox(height: 3),
          Text(label, style: Theme.of(context).textTheme.bodySmall),
        ],
      );
}

class _HistoryBar extends StatelessWidget {
  const _HistoryBar({required this.entry, required this.goal});

  final ({DateTime date, int minutes}) entry;
  final int goal;

  @override
  Widget build(BuildContext context) {
    final ratio = goal <= 0 ? 0.0 : (entry.minutes / goal).clamp(0.0, 1.0).toDouble();
    final height = entry.minutes == 0 ? 8.0 : 14 + ratio * 76;
    final isToday = DateUtils.isSameDay(entry.date, DateTime.now());
    final scheme = Theme.of(context).colorScheme;
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text('${entry.minutes}', style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w800)),
        const SizedBox(height: 4),
        AnimatedContainer(
          duration: const Duration(milliseconds: 250),
          height: height,
          decoration: BoxDecoration(
            color: isToday ? scheme.primary : scheme.primaryContainer,
            borderRadius: BorderRadius.circular(8),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          ['M', 'T', 'W', 'T', 'F', 'S', 'S'][entry.date.weekday - 1],
          style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w800),
        ),
      ],
    );
  }
}

class _TimelineRow extends StatelessWidget {
  const _TimelineRow({required this.entry, required this.goal, required this.isLast});

  final ({DateTime date, int minutes}) entry;
  final int goal;
  final bool isLast;

  @override
  Widget build(BuildContext context) {
    final reached = goal > 0 && entry.minutes >= goal;
    final ratio = goal <= 0 ? 0.0 : (entry.minutes / goal).clamp(0.0, 1.0).toDouble();
    final isToday = DateUtils.isSameDay(entry.date, DateTime.now());
    final scheme = Theme.of(context).colorScheme;

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            width: 34,
            child: Column(
              children: [
                Container(
                  width: 28,
                  height: 28,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: reached || isToday ? scheme.primaryContainer : scheme.surfaceContainerHighest,
                    shape: BoxShape.circle,
                  ),
                  child: reached
                      ? Icon(Icons.check_rounded, size: 17, color: scheme.onPrimaryContainer)
                      : Text('${entry.date.day}', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w900)),
                ),
                if (!isLast)
                  Expanded(
                    child: Container(width: 2, margin: const EdgeInsets.symmetric(vertical: 4), color: scheme.outlineVariant),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: EdgeInsets.only(bottom: isLast ? 4 : 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          _formatDate(entry.date),
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                      ),
                      Text(
                        '${entry.minutes} min',
                        style: const TextStyle(fontWeight: FontWeight.w900),
                      ),
                    ],
                  ),
                  const SizedBox(height: 7),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(99),
                    child: LinearProgressIndicator(value: ratio, minHeight: 6),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    entry.minutes == 0
                        ? 'No completed focus time'
                        : reached
                            ? 'Daily goal reached'
                            : goal > 0
                                ? '${goal - entry.minutes} min to daily goal'
                                : 'Focus time logged',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ),
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
  Widget build(BuildContext context) => SizedBox(
        width: 88,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(value, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
            const SizedBox(height: 4),
            Text(label, style: const TextStyle(fontSize: 11)),
          ],
        ),
      );
}

class _EmptyHistoryCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 34),
          child: Column(
            children: [
              const Icon(Icons.auto_graph_rounded, size: 42),
              const SizedBox(height: 12),
              const Text('No study time logged yet', style: TextStyle(fontWeight: FontWeight.w900)),
              const SizedBox(height: 6),
              Text(
                'Complete a focus block and your daily timeline will appear here.',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
      );
}
