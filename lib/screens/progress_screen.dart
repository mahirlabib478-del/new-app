import 'package:flutter/material.dart';
import '../services/gamification.dart';
import '../services/local_store.dart';
import '../services/progress_analytics.dart';

class ProgressScreen extends StatelessWidget {
  const ProgressScreen({super.key, required this.store});
  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final xp = store.xp;
    final level = store.level;
    final progress = store.levelProgress / 250;
    final minutes = store.completedMinutes;
    final hours = minutes ~/ 60;
    final mins = minutes % 60;
    final streak = store.streak;
    final sessions = store.sessions;
    final analytics = ProgressAnalytics(store).build();
    final gamification = Gamification(store);
    final achievements = gamification.achievements();

    return Scaffold(
      appBar: AppBar(title: const Text('Progress')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text('Your momentum', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  CircleAvatar(radius: 30, child: Text('$level', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900))),
                  const SizedBox(width: 16),
                  Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('Level $level', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
                    Text('$xp XP total'),
                  ])),
                  Column(children: [const Icon(Icons.local_fire_department_rounded), Text('$streak days', style: const TextStyle(fontWeight: FontWeight.w800))]),
                ]),
                const SizedBox(height: 18),
                LinearProgressIndicator(value: progress, minHeight: 8, borderRadius: BorderRadius.circular(99)),
                const SizedBox(height: 8),
                Text('${gamification.xpToNextLevel()} XP to Level ${level + 1}'),
              ]),
            ),
          ),
          const SizedBox(height: 14),
          Row(children: [
            Expanded(child: _StatCard(icon: Icons.schedule_rounded, value: '${hours}h ${mins}m', label: 'Study time')),
            const SizedBox(width: 12),
            Expanded(child: _StatCard(icon: Icons.check_circle_rounded, value: '$sessions', label: 'Sessions')),
          ]),
          const SizedBox(height: 24),
          Text('Last 7 days', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 10),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Expanded(child: _Metric(value: '${analytics.totalMinutes}m', label: 'Total')),
                  Expanded(child: _Metric(value: '${analytics.averageMinutes.round()}m', label: 'Average')),
                  Expanded(child: _Metric(value: '${analytics.activeDays}/7', label: 'Active')),
                ]),
                const SizedBox(height: 20),
                SizedBox(
                  height: 100,
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: analytics.dailyMinutes.map((value) {
                      final maxMinutes = analytics.bestDayMinutes == 0 ? 1 : analytics.bestDayMinutes;
                      final height = 12 + (value / maxMinutes) * 72;
                      return Expanded(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 3),
                          child: Tooltip(
                            message: '$value min',
                            child: Align(
                              alignment: Alignment.bottomCenter,
                              child: Container(
                                height: height,
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(6),
                                  color: Theme.of(context).colorScheme.primary.withValues(alpha: value == 0 ? 0.12 : 0.8),
                                ),
                              ),
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
                const SizedBox(height: 10),
                Text('${analytics.goalHitDays} of 7 days reached your daily goal', style: Theme.of(context).textTheme.bodyMedium),
              ]),
            ),
          ),
          const SizedBox(height: 24),
          Text('Achievements', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 10),
          ...achievements.map((a) => Card(
            margin: const EdgeInsets.only(bottom: 10),
            child: ListTile(
              leading: CircleAvatar(child: Icon(a.unlocked ? Icons.emoji_events_rounded : Icons.lock_outline_rounded)),
              title: Text(a.title, style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text(a.description),
              trailing: Icon(a.unlocked ? Icons.check_circle_rounded : Icons.lock_outline_rounded),
            ),
          )),
          const SizedBox(height: 10),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Row(children: [
            const Icon(Icons.auto_awesome_rounded),
            const SizedBox(width: 12),
            Expanded(child: Text('Every focused minute earns 2 XP. Keep showing up — consistency beats intensity.', style: Theme.of(context).textTheme.bodyMedium)),
          ]))),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.icon, required this.value, required this.label});
  final IconData icon;
  final String value;
  final String label;
  @override
  Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Icon(icon), const SizedBox(height: 10), Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900)), Text(label)])));
}

class _Metric extends StatelessWidget {
  const _Metric({required this.value, required this.label});
  final String value;
  final String label;

  @override
  Widget build(BuildContext context) => Column(
        children: [
          Text(value, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
          const SizedBox(height: 3),
          Text(label),
        ],
      );
}
