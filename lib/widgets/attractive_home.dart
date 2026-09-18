import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/app_language.dart';
import '../services/local_store.dart';
import '../services/sound_effects.dart';
import '../services/today_engine.dart';

/// Study-focused home surface.
///
/// Deliberately avoids blur, shadows, large animations and nested scrolling so
/// the main study flow stays responsive on entry-level Android devices.
class AttractiveHome extends StatelessWidget {
  const AttractiveHome({super.key, required this.store, required this.language, required this.onOpenFocus, required this.onRegularStudy, required this.onExam});

  final LocalStore store;
  final AppLanguage language;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final void Function(bool nextDay) onExam;

  void _tap(VoidCallback action) {
    SoundEffects(store).tap();
    action();
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings(language);
    final snapshot = TodayEngine(store).build();
    final scheme = Theme.of(context).colorScheme;
    final completedByItem = snapshot.plan == null ? const <int, int>{} : store.itemCompletedMinutesMap;

    return SafeArea(
      child: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(18, 16, 18, 0),
            sliver: SliverList(
              delegate: SliverChildListDelegate.fixed([
                Text(_greeting(strings), style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 3),
                Text(
                  strings.isBangla ? 'আজকের পড়াশোনা এক ধাপ করে এগিয়ে নিন' : 'Move through today one focused step at a time',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 16),
                _MissionCard(
                  snapshot: snapshot,
                  strings: strings,
                  onStart: snapshot.hasRemainingWork ? () => _tap(() => onOpenFocus()) : () => _tap(onRegularStudy),
                ),
                const SizedBox(height: 12),
                _GoalCard(snapshot: snapshot, strings: strings),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(child: _StatCard(icon: Icons.local_fire_department_rounded, value: '${snapshot.streak}', label: strings.isBangla ? 'দিন স্ট্রিক' : 'streak')),
                    const SizedBox(width: 8),
                    Expanded(child: _StatCard(icon: Icons.bolt_rounded, value: '${snapshot.xp}', label: 'XP')),
                    const SizedBox(width: 8),
                    Expanded(child: _StatCard(icon: Icons.workspace_premium_rounded, value: '${snapshot.level}', label: strings.isBangla ? 'লেভেল' : 'level')),
                  ],
                ),
                const SizedBox(height: 22),
                _SectionTitle(strings.isBangla ? 'তোমার স্টাডি জার্নি' : 'Your study journey'),
                const SizedBox(height: 10),
              ]),
            ),
          ),
          if (snapshot.plan == null)
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed([
                  _EmptyJourney(strings: strings, onTap: onRegularStudy),
                  const SizedBox(height: 14),
                ]),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    final item = snapshot.plan!.items[index];
                    final completed = completedByItem[index] ?? 0;
                    final progress = item.minutes <= 0 ? 1.0 : (completed / item.minutes).clamp(0.0, 1.0).toDouble();
                    final active = snapshot.currentIndex == index && snapshot.hasRemainingWork;
                    final complete = item.minutes > 0 && completed >= item.minutes;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: _JourneyItem(
                        item: item,
                        progress: progress,
                        active: active,
                        complete: complete,
                        minutesLabel: strings.minutes,
                        onTap: active ? () => _tap(() => onOpenFocus()) : null,
                      ),
                    );
                  },
                  childCount: snapshot.plan!.items.length,
                ),
              ),
            ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(18, 6, 18, 28),
            sliver: SliverList(
              delegate: SliverChildListDelegate.fixed([
                _SectionTitle(strings.isBangla ? 'স্টাডি মোড' : 'Study modes'),
                const SizedBox(height: 10),
                _ModeCard(
                  icon: Icons.menu_book_rounded,
                  title: strings.isBangla ? 'রেগুলার স্টাডি' : 'Regular Study',
                  subtitle: strings.isBangla ? 'বিষয়, অধ্যায় ও ফোকাস ব্লক পরিকল্পনা করুন।' : 'Build a focused plan for your subjects.',
                  onTap: () => _tap(onRegularStudy),
                ),
                _ModeCard(
                  icon: Icons.auto_awesome_rounded,
                  title: strings.isBangla ? 'পরীক্ষার প্রস্তুতি' : 'Exam Preparation',
                  subtitle: strings.isBangla ? 'অগ্রাধিকার অনুযায়ী রিভিশন করুন।' : 'Prioritize the work that matters most.',
                  onTap: () => _tap(() => onExam(false)),
                ),
                _ModeCard(
                  icon: Icons.bolt_rounded,
                  title: strings.isBangla ? 'আগামীকালের পরীক্ষা' : 'Next Day Exam',
                  subtitle: strings.isBangla ? 'জরুরি বিষয়গুলো আগে শেষ করুন।' : 'Focus on high-impact revision first.',
                  onTap: () => _tap(() => onExam(true)),
                ),
                const SizedBox(height: 6),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(14),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.lightbulb_outline_rounded, color: scheme.primary),
                        const SizedBox(width: 10),
                        Expanded(child: Text(snapshot.recommendationReason, style: const TextStyle(fontWeight: FontWeight.w700))),
                      ],
                    ),
                  ),
                ),
              ]),
            ),
          ),
        ],
      ),
    );
  String _greeting(AppStrings strings) {
    final hour = DateTime.now().hour;
    if (strings.isBangla) {
      if (hour < 12) return 'সুপ্রভাত';
      if (hour < 18) return 'শুভ অপরাহ্ণ';
      return 'শুভ সন্ধ্যা';
    }
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }
}

class _MissionCard extends StatelessWidget {
  const _MissionCard({required this.snapshot, required this.strings, required this.onStart});
  final TodaySnapshot snapshot;
  final AppStrings strings;
  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final progress = snapshot.progress.clamp(0.0, 1.0).toDouble();
    final title = snapshot.isComplete
        ? (strings.isBangla ? 'আজকের মিশন সম্পূর্ণ!' : 'Today’s mission complete!')
        : snapshot.nextItem?.title ?? (strings.isBangla ? 'আজকের প্ল্যান তৈরি করুন' : 'Create today’s plan');
    final subtitle = snapshot.isComplete
        ? (strings.isBangla ? 'চমৎকার কাজ। কাল আবার শুরু করুন।' : 'Great work. Keep the momentum.')
        : snapshot.nextItem == null
            ? (strings.isBangla ? 'একটি স্টাডি মোড বেছে নিয়ে শুরু করুন।' : 'Pick a study mode and start.')
            : snapshot.recommendationReason;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: scheme.primaryContainer,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: scheme.primary.withValues(alpha: 0.14)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(strings.isBangla ? 'আজকের মিশন' : 'TODAY’S MISSION', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w900, letterSpacing: 1.1, color: scheme.onPrimaryContainer))),
              Icon(snapshot.isComplete ? Icons.check_circle_rounded : Icons.flag_rounded, color: scheme.onPrimaryContainer),
            ],
          ),
          const SizedBox(height: 14),
          Text(title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900, color: scheme.onPrimaryContainer)),
          const SizedBox(height: 6),
          Text(subtitle, style: TextStyle(color: scheme.onPrimaryContainer.withValues(alpha: 0.78))),
          const SizedBox(height: 16),
          ClipRRect(borderRadius: BorderRadius.circular(10), child: LinearProgressIndicator(value: progress, minHeight: 9, backgroundColor: scheme.onPrimaryContainer.withValues(alpha: 0.12))),
          const SizedBox(height: 8),
          Text('${snapshot.remainingMinutes} ${strings.minutes} left', style: TextStyle(fontWeight: FontWeight.w800, color: scheme.onPrimaryContainer)),
          const SizedBox(height: 3),
          Text('${snapshot.completedMinutes} ${strings.minutes} completed • ${snapshot.xp} XP', style: TextStyle(fontWeight: FontWeight.w700, color: scheme.onPrimaryContainer.withValues(alpha: 0.78))),
          const SizedBox(height: 14),
          FilledButton.icon(key: const ValueKey<String>('home-mission-action'), onPressed: onStart, icon: Icon(snapshot.isComplete ? Icons.add_rounded : Icons.play_arrow_rounded), label: Text(snapshot.isComplete ? (strings.isBangla ? 'নতুন প্ল্যান' : 'New plan') : (strings.isBangla ? 'শুরু করুন' : 'Start'))),
        ],
      ),
    );
  }
}

class _GoalCard extends StatelessWidget {
  const _GoalCard({required this.snapshot, required this.strings});
  final TodaySnapshot snapshot;
  final AppStrings strings;

  @override
  Widget build(BuildContext context) {
    final progress = snapshot.goalProgress.clamp(0.0, 1.0).toDouble();
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(child: Text(strings.isBangla ? 'আজকের লক্ষ্য' : 'Today’s goal', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900))),
            Text('${snapshot.todayCompletedMinutes} / ${snapshot.dailyGoalMinutes} ${strings.minutes}', style: Theme.of(context).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w800)),
          ]),
          const SizedBox(height: 9),
          LinearProgressIndicator(value: progress, minHeight: 7),
          const SizedBox(height: 7),
          Text(snapshot.dailyGoalReached ? (strings.isBangla ? 'লক্ষ্য পূর্ণ। গতি ধরে রাখুন।' : 'Goal reached. Keep the momentum.') : '${snapshot.goalRemainingMinutes} ${strings.minutes} ${strings.isBangla ? 'বাকি আজ' : 'left today'}'),
        ]),
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
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 6),
        child: Column(children: [
          Icon(icon, size: 21, color: scheme.primary),
          const SizedBox(height: 4),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
          Text(label, style: Theme.of(context).textTheme.labelSmall),
        ]),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.title);
  final String title;
  @override
  Widget build(BuildContext context) => Text(title, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900));
}

class _JourneyItem extends StatelessWidget {
  const _JourneyItem({required this.item, required this.progress, required this.active, required this.complete, required this.minutesLabel, required this.onTap});
  final StudyItem item;
  final double progress;
  final bool active;
  final bool complete;
  final String minutesLabel;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final icon = complete ? Icons.check_rounded : active ? Icons.play_arrow_rounded : Icons.menu_book_rounded;
    return Card(
      child: ListTile(
        onTap: onTap,
        enableFeedback: true,
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
        leading: CircleAvatar(radius: 22, backgroundColor: complete ? scheme.primary : scheme.surfaceContainerHighest, foregroundColor: complete ? scheme.onPrimary : scheme.onSurfaceVariant, child: Icon(icon, size: 20)),
        title: Text(item.title, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [if (item.topic.isNotEmpty) Text(item.topic), const SizedBox(height: 5), LinearProgressIndicator(value: progress, minHeight: 5)]),
        trailing: active ? const Icon(Icons.chevron_right_rounded) : Text('$minutesLabel ${item.minutes}'),
      ),
    );
  }
}

class _EmptyJourney extends StatelessWidget {
  const _EmptyJourney({required this.strings, required this.onTap});
  final AppStrings strings;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
        child: ListTile(
          onTap: onTap,
          enableFeedback: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
          leading: const CircleAvatar(child: Icon(Icons.add_rounded)),
          title: Text(strings.isBangla ? 'আজকের প্ল্যান নেই' : 'No plan for today', style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text(strings.isBangla ? 'ট্যাপ করে শুরু করুন' : 'Tap to build your study plan'),
          trailing: const Icon(Icons.chevron_right_rounded),
        ),
      );
}

class _ModeCard extends StatelessWidget {
  const _ModeCard({required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          onTap: onTap,
          enableFeedback: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
          leading: CircleAvatar(radius: 24, child: Icon(icon)),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text(subtitle),
          trailing: const Icon(Icons.chevron_right_rounded),
        ),
      );
}
