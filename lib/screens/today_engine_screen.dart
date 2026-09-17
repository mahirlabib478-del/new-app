import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';
import '../services/today_engine.dart';
import 'focus_flow.dart';

class TodayEngineScreen extends StatelessWidget {
  const TodayEngineScreen({super.key, required this.store, this.onOpenFocus});
  final LocalStore store;
  final Future<void> Function({StudyPlan? plan})? onOpenFocus;

  Future<void> _openNext(BuildContext context, TodaySnapshot snapshot) async {
    if (onOpenFocus != null) {
      await onOpenFocus!();
      return;
    }
    final plan = snapshot.plan;
    if (plan == null || plan.items.isEmpty || snapshot.isComplete) return;
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => FocusScreen(
          store: store,
          plan: plan,
          index: snapshot.currentIndex,
          blockIndex: snapshot.currentBlockIndex,
        ),
      ),
    );
  }

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
          Text('What do I need to do today?', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [colorScheme.primaryContainer, colorScheme.secondaryContainer], begin: Alignment.topLeft, end: Alignment.bottomRight),
              borderRadius: BorderRadius.circular(28),
            ),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('LEVEL ${snapshot.level}', style: TextStyle(color: colorScheme.onPrimaryContainer.withValues(alpha: 0.78), fontWeight: FontWeight.w800, letterSpacing: 1.2)),
              const SizedBox(height: 8),
              Text('${snapshot.remainingMinutes} min left', style: TextStyle(color: colorScheme.onPrimaryContainer, fontSize: 34, fontWeight: FontWeight.w900)),
              const SizedBox(height: 14),
              ClipRRect(
                borderRadius: BorderRadius.circular(20),
                child: LinearProgressIndicator(value: snapshot.progress, minHeight: 9, backgroundColor: colorScheme.onPrimaryContainer.withValues(alpha: 0.14), color: colorScheme.onPrimaryContainer),
              ),
              const SizedBox(height: 8),
              Text('${snapshot.completedMinutes} min completed', style: TextStyle(color: colorScheme.onPrimaryContainer.withValues(alpha: 0.78))),
            ]),
          ),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(child: _MiniStat(icon: Icons.checklist_rounded, value: '${snapshot.remainingItemCount}', label: 'items left')),
            const SizedBox(width: 10),
            Expanded(child: _MiniStat(icon: Icons.timelapse_rounded, value: '${snapshot.estimatedFocusBlocksRemaining}', label: 'focus blocks')),
          ]),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  const Icon(Icons.flag_rounded),
                  const SizedBox(width: 10),
                  const Expanded(child: Text("TODAY'S GOAL", style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.1))),
                  Text('${snapshot.todayCompletedMinutes} / ${snapshot.dailyGoalMinutes}m', style: const TextStyle(fontWeight: FontWeight.w900)),
                ]),
                const SizedBox(height: 12),
                ClipRRect(borderRadius: BorderRadius.circular(20), child: LinearProgressIndicator(value: snapshot.goalProgress, minHeight: 8)),
                const SizedBox(height: 8),
                Text(snapshot.dailyGoalReached ? 'Daily goal reached. Keep the momentum going.' : '${snapshot.goalRemainingMinutes} min to reach today\'s goal', style: Theme.of(context).textTheme.bodyMedium),
              ]),
            ),
          ),
          const SizedBox(height: 12),
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
                        leading: const CircleAvatar(child: Icon(Icons.play_arrow_rounded)),
                        title: Text(snapshot.nextItem!.title, style: const TextStyle(fontWeight: FontWeight.w900)),
                        subtitle: Text(snapshot.nextItem!.topic.isEmpty ? 'Focus session' : snapshot.nextItem!.topic),
                        trailing: Text('${snapshot.nextItem!.minutes}m', style: const TextStyle(fontWeight: FontWeight.w900)),
                        onTap: () => _openNext(context, snapshot),
                      ),
                      const SizedBox(height: 8),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(color: colorScheme.primaryContainer, borderRadius: BorderRadius.circular(16)),
                        child: Row(children: [
                          Icon(Icons.auto_awesome_rounded, color: colorScheme.onPrimaryContainer),
                          const SizedBox(width: 10),
                          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text('RECOMMENDED FOCUS', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.0, color: colorScheme.onPrimaryContainer)),
                            const SizedBox(height: 4),
                            Text('${snapshot.recommendedFocusMinutes} min · ${snapshot.recommendationReason}', style: TextStyle(color: colorScheme.onPrimaryContainer)),
                          ])),
                        ]),
                      ),
                    ]),
            ),
          ),
          const SizedBox(height: 12),
          if (plan != null) ...[
            Text('TODAY\'S PLAN', style: Theme.of(context).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w800, letterSpacing: 1.1)),
            const SizedBox(height: 8),
            ...plan.items.take(8).map((item) => Card(child: ListTile(
              leading: const Icon(Icons.radio_button_unchecked_rounded),
              title: Text(item.title, style: const TextStyle(fontWeight: FontWeight.w700)),
              subtitle: Text(item.topic),
              trailing: Text('${item.minutes}m'),
            ))),
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
