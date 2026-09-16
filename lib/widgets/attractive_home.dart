import 'package:flutter/material.dart';

import '../models/study_models.dart';
import '../services/app_language.dart';
import '../services/local_store.dart';
import '../services/today_engine.dart';

class AttractiveHome extends StatelessWidget {
  const AttractiveHome({
    super.key,
    required this.store,
    required this.language,
    required this.onOpenFocus,
    required this.onRegularStudy,
    required this.onExam,
  });

  final LocalStore store;
  final AppLanguage language;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final void Function(bool nextDay) onExam;

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings(language);
    final snapshot = TodayEngine(store).build();
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_greeting(strings), style: theme.textTheme.titleMedium),
                    const SizedBox(height: 4),
                    Text(
                      strings.isBangla ? 'আজকের যাত্রা শুরু করি' : 'Let’s make today count',
                      style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
                    ),
                  ],
                ),
              ),
              CircleAvatar(
                radius: 24,
                backgroundColor: scheme.primaryContainer,
                child: Icon(Icons.auto_awesome_rounded, color: scheme.onPrimaryContainer),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _MissionCard(
            snapshot: snapshot,
            strings: strings,
            onStart: snapshot.hasRemainingWork
                ? () => onOpenFocus()
                : onRegularStudy,
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(child: _StatCard(icon: Icons.local_fire_department_rounded, value: '${snapshot.streak}', label: strings.isBangla ? 'দিন স্ট্রিক' : 'day streak')),
              const SizedBox(width: 10),
              Expanded(child: _StatCard(icon: Icons.bolt_rounded, value: '${snapshot.xp}', label: 'XP')),
              const SizedBox(width: 10),
              Expanded(child: _StatCard(icon: Icons.workspace_premium_rounded, value: '${snapshot.level}', label: strings.isBangla ? 'লেভেল' : 'level')),
            ],
          ),
          const SizedBox(height: 24),
          _SectionTitle(strings.isBangla ? 'তোমার স্টাডি জার্নি' : 'Your study journey'),
          const SizedBox(height: 12),
          if (snapshot.plan == null)
            _EmptyJourney(strings: strings, onTap: onRegularStudy)
          else
            ...snapshot.plan!.items.asMap().entries.map((entry) {
              final item = entry.value;
              final completed = store.itemCompletedMinutesMap[entry.key] ?? 0;
              final progress = item.minutes <= 0
                  ? 1.0
                  : (completed / item.minutes).clamp(0.0, 1.0).toDouble();
              final active = snapshot.currentIndex == entry.key && snapshot.hasRemainingWork;
              final complete = item.minutes > 0 && completed >= item.minutes;
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: _JourneyItem(
                  item: item,
                  progress: progress,
                  active: active,
                  complete: complete,
                  minutesLabel: strings.minutes,
                  onTap: active ? () => onOpenFocus() : null,
                ),
              );
            }),
          const SizedBox(height: 22),
          _SectionTitle(strings.isBangla ? 'স্টাডি মোড' : 'Study modes'),
          const SizedBox(height: 12),
          _ModeCard(
            icon: Icons.menu_book_rounded,
            title: strings.isBangla ? 'রেগুলার স্টাডি' : 'Regular Study',
            subtitle: strings.isBangla ? 'নিজের মতো করে আজকের প্ল্যান তৈরি করুন।' : 'Build your own plan for today.',
            onTap: onRegularStudy,
          ),
          _ModeCard(
            icon: Icons.auto_awesome_rounded,
            title: strings.isBangla ? 'পরীক্ষার প্রস্তুতি' : 'Exam Preparation',
            subtitle: strings.isBangla ? 'অগ্রাধিকার অনুযায়ী রিভিশন করুন।' : 'Revise by priority.',
            onTap: () => onExam(false),
          ),
          _ModeCard(
            icon: Icons.bolt_rounded,
            title: strings.isBangla ? 'আগামীকালের পরীক্ষা' : 'Next Day Exam',
            subtitle: strings.isBangla ? 'জরুরি বিষয়গুলো আগে শেষ করুন।' : 'Focus on the most important topics first.',
            onTap: () => onExam(true),
          ),
        ],
      ),
    );
  }

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
        : snapshot.nextItem?.title ??
            (strings.isBangla ? 'আজকের প্ল্যান তৈরি করুন' : 'Create today’s plan');
    final subtitle = snapshot.isComplete
        ? (strings.isBangla ? 'চমৎকার কাজ। কাল আবার শুরু করুন।' : 'Great work. Keep the streak alive.')
        : snapshot.nextItem == null
            ? (strings.isBangla ? 'একটি স্টাডি মোড বেছে নিয়ে শুরু করুন।' : 'Pick a study mode and start your journey.')
            : snapshot.recommendationReason;

    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(28),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [scheme.primaryContainer, scheme.secondaryContainer],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  strings.isBangla ? 'আজকের মিশন' : 'TODAY’S MISSION',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w900, letterSpacing: 1.2, color: scheme.onPrimaryContainer),
                ),
              ),
              Icon(Icons.flag_rounded, color: scheme.onPrimaryContainer),
            ],
          ),
          const SizedBox(height: 18),
          Text(
            title,
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900, color: scheme.onPrimaryContainer),
          ),
          const SizedBox(height: 7),
          Text(subtitle, style: TextStyle(color: scheme.onPrimaryContainer.withValues(alpha: 0.78))),
          const SizedBox(height: 18),
          ClipRRect(
            borderRadius: BorderRadius.circular(20),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 10,
              backgroundColor: scheme.onPrimaryContainer.withValues(alpha: 0.12),
            ),
          ),
          const SizedBox(height: 9),
          Row(
            children: [
              Expanded(
                child: Text(
                  '${snapshot.completedMinutes} / ${snapshot.plan?.allocatedMinutes ?? 0} ${strings.minutes}',
                  style: TextStyle(fontWeight: FontWeight.w800, color: scheme.onPrimaryContainer),
                ),
              ),
              if (snapshot.hasRemainingWork)
                Text(
                  '${snapshot.estimatedFocusBlocksRemaining} ${strings.isBangla ? 'ব্লক বাকি' : 'blocks left'}',
                  style: TextStyle(fontWeight: FontWeight.w700, color: scheme.onPrimaryContainer),
                ),
            ],
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: onStart,
            icon: Icon(snapshot.isComplete ? Icons.check_rounded : Icons.play_arrow_rounded),
            label: Text(
              snapshot.isComplete
                  ? (strings.isBangla ? 'নতুন প্ল্যান' : 'New plan')
                  : (strings.isBangla ? 'চালিয়ে যান' : 'Continue'),
            ),
          ),
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
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
        child: Column(
          children: [
            Icon(icon, size: 22),
            const SizedBox(height: 5),
            Text(value, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
            Text(label, style: Theme.of(context).textTheme.labelSmall),
          ],
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.title);

  final String title;

  @override
  Widget build(BuildContext context) {
    return Text(title, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900));
  }
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
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: CircleAvatar(
          backgroundColor: complete ? scheme.primary : scheme.surfaceContainerHighest,
          foregroundColor: complete ? scheme.onPrimary : scheme.onSurfaceVariant,
          child: Icon(icon),
        ),
        title: Text(item.title, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (item.topic.isNotEmpty) Text(item.topic),
            const SizedBox(height: 6),
            LinearProgressIndicator(value: progress),
          ],
        ),
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
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            children: [
              const CircleAvatar(child: Icon(Icons.add_rounded)),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(strings.isBangla ? 'আজকের প্ল্যান নেই' : 'No plan for today', style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text(strings.isBangla ? 'ট্যাপ করে শুরু করুন' : 'Tap to build your study plan'),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ModeCard extends StatelessWidget {
  const _ModeCard({required this.icon, required this.title, required this.subtitle, required this.onTap});

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        onTap: onTap,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
        leading: CircleAvatar(radius: 25, child: Icon(icon)),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.chevron_right_rounded),
      ),
    );
  }
}
