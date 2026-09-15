import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';
import '../services/today_engine.dart';

class TodayEngineScreen extends StatelessWidget {
  const TodayEngineScreen({super.key, required this.store});
  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final snapshot = TodayEngine(store).build();
    final plan = snapshot.plan;
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Today Engine')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(
            'What do I need to do today?',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [colorScheme.primary, colorScheme.secondary]),
              borderRadius: BorderRadius.circular(28),
            ),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('LEVEL ${snapshot.level}', style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.w800, letterSpacing: 1.2)),
              const SizedBox(height: 8),
              Text('${snapshot.remainingMinutes} min left', style: const TextStyle(color: Colors.white, fontSize: 34, fontWeight: FontWeight.w900)),
              const SizedBox(height: 14),
              ClipRRect(borderRadius: BorderRadius.circular(20), child: LinearProgressIndicator(value: snapshot.progress, minHeight: 9, backgroundColor: Colors.white24)),
              const SizedBox(height: 8),
              Text('${snapshot.completedMinutes} min completed', style: const TextStyle(color: Colors.white70)),
            ]),
          ),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(child: _MiniStat(icon: Icons.local_fire_department_rounded, value: '${snapshot.streak}', label: 'streak')),
            const SizedBox(width: 10),
            Expanded(child: _MiniStat(icon: Icons.bolt_rounded, value: '${snapshot.xp}', label: 'XP')),
          ]),
          const SizedBox(height: 20),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: snapshot.nextItem == null
                  ? const _EmptyToday()
                  : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Text('NEXT UP', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1.1)),
                      const SizedBox(height: 12),
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: CircleAvatar(child: const Icon(Icons.play_arrow_rounded)),
                        title: Text(snapshot.nextItem!.title, style: const TextStyle(fontWeight: FontWeight.w900)),
                        subtitle: Text(snapshot.nextItem!.topic.isEmpty ? 'Focus session' : snapshot.nextItem!.topic),
                        trailing: Text('${snapshot.nextItem!.minutes}m', style: const TextStyle(fontWeight: FontWeight.w900)),
                      ),
                    ]),
            ),
          ),
          const SizedBox(height: 12),
          if (plan != null) ...[
            Text('TODAY\'S PLAN', style: Theme.of(context).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w800, letterSpacing: 1.1)),
            const SizedBox(height: 8),
            ...plan.items.take(8).map((item) => Card(
              child: ListTile(
                leading: const Icon(Icons.radio_button_unchecked_rounded),
                title: Text(item.title, style: const TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(item.topic),
                trailing: Text('${item.minutes}m'),
              ),
            )),
          ],
        ],
      ),
    );
  }
}

class _MiniStat extends StatelessWidget {
  const _MiniStat({required this.icon, required this.value, required this.label});
  final IconData icon;
  final String value;
  final String label;

  @override
  Widget build(BuildContext context) => Card(child: Padding(
    padding: const EdgeInsets.all(16),
    child: Row(children: [Icon(icon), const SizedBox(width: 10), Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(value, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 20)), Text(label)])]),
  ));
}

class _EmptyToday extends StatelessWidget {
  const _EmptyToday();
  @override
  Widget build(BuildContext context) => const Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Text('No plan yet', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 20)),
    SizedBox(height: 6),
    Text('Create a Regular Study or Exam plan and Today Engine will turn it into your next action.'),
  ]);
}
