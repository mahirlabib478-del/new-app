import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/app_language.dart';
import '../services/local_store.dart';
import '../services/today_engine.dart';

class AttractiveHome extends StatelessWidget {
  const AttractiveHome({super.key, required this.store, required this.onOpenFocus, required this.onRegularStudy, required this.onExam, required this.language});
  final LocalStore store;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final void Function(bool) onExam;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final s = AppStrings(language);
    final snapshot = TodayEngine(store).build();
    final theme = Theme.of(context);
    final primary = theme.colorScheme.primary;
    final completed = snapshot.hasPlan ? snapshot.completedMinutes : 0;
    final remaining = snapshot.hasPlan ? snapshot.remainingMinutes : 0;
    final progress = snapshot.hasPlan ? snapshot.progress.clamp(0.0, 1.0) : 0.0;
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [theme.colorScheme.surface, Color.alphaBlend(primary.withOpacity(.08), theme.colorScheme.surface), theme.colorScheme.surface]),
        ),
        child: SafeArea(
          child: ListView(padding: const EdgeInsets.fromLTRB(20, 18, 20, 28), children: [
            Row(children: [
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(_greeting(s), style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
                const SizedBox(height: 3),
                Text(s.isBangla ? 'আজ একটু এগিয়ে যাই 🚀' : 'Let’s make progress today 🚀', style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
              ])),
              Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9), decoration: BoxDecoration(color: primary.withOpacity(.12), borderRadius: BorderRadius.circular(18)), child: Row(children: [Icon(Icons.local_fire_department_rounded, color: primary, size: 20), const SizedBox(width: 5), Text('${snapshot.streak}', style: const TextStyle(fontWeight: FontWeight.w900))])),
            ]),
            const SizedBox(height: 20),
            _HeroCard(snapshot: snapshot, progress: progress, primary: primary, language: language, onOpenFocus: onOpenFocus),
            const SizedBox(height: 14),
            Row(children: [
              Expanded(child: _StatCard(icon: Icons.schedule_rounded, value: '$completed', label: s.isBangla ? 'মিনিট সম্পন্ন' : 'min studied', color: primary)),
              const SizedBox(width: 10),
              Expanded(child: _StatCard(icon: Icons.stars_rounded, value: '${snapshot.xp}', label: 'XP', color: primary)),
              const SizedBox(width: 10),
              Expanded(child: _StatCard(icon: Icons.flag_rounded, value: '${snapshot.todayCompletedMinutes}/${snapshot.dailyGoalMinutes}', label: s.isBangla ? 'আজকের লক্ষ্য' : 'daily goal', color: primary)),
            ]),
            const SizedBox(height: 24),
            if (snapshot.nextItem != null) ...[
              Row(children: [Expanded(child: Text(s.isBangla ? 'এখন যা পড়বে' : 'Your next lesson', style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900))), const Icon(Icons.arrow_forward_rounded, size: 20)]),
              const SizedBox(height: 10),
              _NextLesson(snapshot: snapshot, primary: primary, language: language, onOpenFocus: onOpenFocus),
              const SizedBox(height: 24),
            ],
            Text(s.isBangla ? 'তোমার স্টাডি জার্নি' : 'Your study journey', style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 12),
            _JourneyTile(icon: Icons.menu_book_rounded, title: s.isBangla ? 'রেগুলার স্টাডি' : 'Regular Study', subtitle: s.isBangla ? 'নিজের মতো করে প্ল্যান বানাও' : 'Build your own study plan', onTap: onRegularStudy, primary: primary, active: true),
            _JourneyTile(icon: Icons.auto_awesome_rounded, title: s.isBangla ? 'পরীক্ষার প্রস্তুতি' : 'Exam Preparation', subtitle: s.isBangla ? 'অগ্রাধিকার দিয়ে রিভিশন' : 'Priority-based revision', onTap: () => onExam(false), primary: primary),
            _JourneyTile(icon: Icons.bolt_rounded, title: s.isBangla ? 'আগামীকালের পরীক্ষা' : 'Next Day Exam', subtitle: s.isBangla ? 'সবচেয়ে জরুরি বিষয় আগে' : 'Focus on what matters most', onTap: () => onExam(true), primary: primary),
            const SizedBox(height: 12),
            if (snapshot.hasPlan && remaining == 0) _CompleteBanner(language: language, primary: primary),
          ]),
        ),
      ),
    );
  }

  String _greeting(AppStrings s) {
    final hour = DateTime.now().hour;
    if (s.isBangla) return hour < 12 ? 'সুপ্রভাত' : hour < 18 ? 'শুভ অপরাহ্ণ' : 'শুভ সন্ধ্যা';
    return hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard({required this.snapshot, required this.progress, required this.primary, required this.language, required this.onOpenFocus});
  final TodaySnapshot snapshot;
  final double progress;
  final Color primary;
  final AppLanguage language;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  @override
  Widget build(BuildContext context) {
    final s = AppStrings(language);
    return Container(padding: const EdgeInsets.all(22), decoration: BoxDecoration(borderRadius: BorderRadius.circular(30), gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [primary, Color.alphaBlend(Colors.black.withOpacity(.18), primary)]), boxShadow: [BoxShadow(color: primary.withOpacity(.22), blurRadius: 24, offset: const Offset(0, 10))]), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [Expanded(child: Text(s.isBangla ? 'আজকের মিশন' : 'TODAY’S MISSION', style: TextStyle(color: Colors.white.withOpacity(.82), fontWeight: FontWeight.w900, letterSpacing: 1.3))), Icon(Icons.emoji_events_rounded, color: Colors.white.withOpacity(.9))]),
      const SizedBox(height: 14),
      Text(snapshot.hasPlan ? '${snapshot.remainingMinutes} ${s.minutes} ${s.isBangla ? 'বাকি' : 'left' }' : (s.isBangla ? 'আজকের প্ল্যান তৈরি করো' : 'Create today’s plan'), style: const TextStyle(color: Colors.white, fontSize: 31, fontWeight: FontWeight.w900)),
      const SizedBox(height: 7),
      Text(snapshot.hasPlan ? '${snapshot.completedMinutes} ${s.minutes} ${s.isBangla ? 'সম্পন্ন' : 'completed'}' : (s.isBangla ? 'একটি স্টাডি মোড বেছে নাও' : 'Choose a study mode to begin'), style: TextStyle(color: Colors.white.withOpacity(.82), fontWeight: FontWeight.w600)),
      const SizedBox(height: 18),
      ClipRRect(borderRadius: BorderRadius.circular(10), child: LinearProgressIndicator(value: progress, minHeight: 10, backgroundColor: Colors.white.withOpacity(.2), valueColor: const AlwaysStoppedAnimation<Color>(Colors.white))),
      const SizedBox(height: 15),
      if (snapshot.hasPlan && !snapshot.isComplete) FilledButton.icon(style: FilledButton.styleFrom(backgroundColor: Colors.white, foregroundColor: primary, padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13)), onPressed: () => onOpenFocus(), icon: const Icon(Icons.play_arrow_rounded), label: Text(s.isBangla ? 'চালিয়ে যাও' : 'Continue studying', style: const TextStyle(fontWeight: FontWeight.w900)))
      else if (!snapshot.hasPlan) FilledButton.icon(style: FilledButton.styleFrom(backgroundColor: Colors.white, foregroundColor: primary, padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13)), onPressed: () => onOpenFocus(), icon: const Icon(Icons.auto_awesome_rounded), label: Text(s.isBangla ? 'শুরু করি' : 'Let’s start', style: const TextStyle(fontWeight: FontWeight.w900)))
      else Row(children: [const Icon(Icons.check_circle_rounded, color: Colors.white), const SizedBox(width: 8), Text(s.isBangla ? 'মিশন সম্পন্ন! 🎉' : 'Mission complete! 🎉', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900))]),
    ]));
  }
}

class _NextLesson extends StatelessWidget {
  const _NextLesson({required this.snapshot, required this.primary, required this.language, required this.onOpenFocus});
  final TodaySnapshot snapshot; final Color primary; final AppLanguage language; final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  @override Widget build(BuildContext context) { final s = AppStrings(language); final item = snapshot.nextItem!; return Card(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)), child: InkWell(borderRadius: BorderRadius.circular(24), onTap: () => onOpenFocus(), child: Padding(padding: const EdgeInsets.all(16), child: Row(children: [Container(width: 52, height: 52, decoration: BoxDecoration(color: primary.withOpacity(.12), borderRadius: BorderRadius.circular(17)), child: Icon(Icons.play_arrow_rounded, color: primary)), const SizedBox(width: 14), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(item.title, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)), const SizedBox(height: 3), Text(item.topic.isEmpty ? '${item.minutes} ${s.minutes}' : item.topic, maxLines: 1, overflow: TextOverflow.ellipsis)])), const Icon(Icons.chevron_right_rounded)])))); }
}

class _StatCard extends StatelessWidget { const _StatCard({required this.icon, required this.value, required this.label, required this.color}); final IconData icon; final String value; final String label; final Color color; @override Widget build(BuildContext context) => Card(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)), child: Padding(padding: const EdgeInsets.fromLTRB(12, 14, 12, 13), child: Column(children: [Icon(icon, color: color, size: 21), const SizedBox(height: 8), Text(value, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)), const SizedBox(height: 3), Text(label, textAlign: TextAlign.center, maxLines: 2, style: Theme.of(context).textTheme.labelSmall)]))); }
}

class _JourneyTile extends StatelessWidget { const _JourneyTile({required this.icon, required this.title, required this.subtitle, required this.onTap, required this.primary, this.active = false}); final IconData icon; final String title; final String subtitle; final VoidCallback onTap; final Color primary; final bool active; @override Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 10), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)), child: InkWell(borderRadius: BorderRadius.circular(22), onTap: onTap, child: Padding(padding: const EdgeInsets.all(14), child: Row(children: [Container(width: 48, height: 48, decoration: BoxDecoration(color: active ? primary.withOpacity(.14) : Theme.of(context).colorScheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(16)), child: Icon(icon, color: active ? primary : null)), const SizedBox(width: 13), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontWeight: FontWeight.w900)), const SizedBox(height: 3), Text(subtitle)])), Icon(Icons.arrow_forward_ios_rounded, size: 16, color: Theme.of(context).colorScheme.onSurfaceVariant)])))); }
}

class _CompleteBanner extends StatelessWidget { const _CompleteBanner({required this.language, required this.primary}); final AppLanguage language; final Color primary; @override Widget build(BuildContext context) { final s = AppStrings(language); return Container(padding: const EdgeInsets.all(17), decoration: BoxDecoration(color: primary.withOpacity(.1), borderRadius: BorderRadius.circular(22)), child: Row(children: [Icon(Icons.celebration_rounded, color: primary), const SizedBox(width: 12), Expanded(child: Text(s.isBangla ? 'আজকের সব স্টাডি শেষ। দারুণ কাজ! 🎉' : 'All planned study is complete. Great work! 🎉', style: const TextStyle(fontWeight: FontWeight.w800)))])); } }
