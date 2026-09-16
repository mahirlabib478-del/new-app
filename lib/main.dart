import 'dart:async';
import 'package:flutter/material.dart';
import 'models/app_language.dart';
import 'models/study_plan.dart';
import 'screens/exam_planner_screen.dart';
import 'screens/focus_flow.dart';
import 'screens/profile_screen.dart';
import 'screens/progress_dashboard.dart';
import 'screens/regular_study_planner.dart';
import 'screens/saved_sessions_screen.dart';
import 'services/app_strings.dart';
import 'services/local_store.dart';
import 'services/reminder_coordinator.dart';
import 'services/study_session_store.dart';
import 'services/today_engine.dart';
import 'widgets/attractive_home.dart';

class StudyOS extends StatefulWidget {
  const StudyOS({super.key});
  @override State<StudyOS> createState() => _StudyOSState();
}

class _StudyOSState extends State<StudyOS> {
  late final LocalStore store;
  late final ReminderCoordinator reminderCoordinator;
  AppLanguage language = AppLanguage.english;
  int tab = 0;

  @override
  void initState() {
    super.initState();
    store = LocalStore();
    reminderCoordinator = ReminderCoordinator(store);
    WidgetsBinding.instance.addPostFrameCallback((_) => unawaited(reminderCoordinator.sync()));
  }

  @override
  void dispose() {
    reminderCoordinator.dispose();
    super.dispose();
  }

  Future<void> openFocus({StudyPlan? plan}) async {
    final snapshot = TodayEngine(store).build();
    final activePlan = plan ?? snapshot.plan;
    if (activePlan == null || activePlan.items.isEmpty) return;
    if (plan == null && snapshot.isComplete) return;
    final storedPlan = store.loadPlan();
    final isStoredPlan = storedPlan != null && storedPlan.toJson().toString() == activePlan.toJson().toString();
    if (!mounted) return;
    await Navigator.push(context, MaterialPageRoute(builder: (_) => FocusScreen(plan: activePlan, store: store, index: isStoredPlan ? snapshot.currentIndex : 0, blockIndex: isStoredPlan ? snapshot.currentBlockIndex : 0)));
    if (mounted) setState(() {});
  }

  void openRegularStudy() {
    Navigator.push(context, MaterialPageRoute(builder: (_) => RegularStudyPlanner(store: store, onStartPlan: (plan) async {
      if (!mounted) return;
      Navigator.pop(context);
      await openFocus(plan: plan);
    })));
  }

  void openExam({required bool nextDay}) {
    Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: nextDay, store: store, onStartPlan: (plan) async {
      if (!mounted) return;
      Navigator.pop(context);
      await openFocus(plan: plan);
    })));
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings(language);
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Study OS',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.indigo, brightness: Brightness.light),
      home: Scaffold(
        body: IndexedStack(index: tab, children: [
          AttractiveHome(store: store, language: language, onOpenFocus: () => openFocus(), onRegularStudy: openRegularStudy, onExam: () => openExam(nextDay: false)),
          StudyHub(store: store, onStartPlan: openFocus, onRegularStudy: openRegularStudy, language: language),
          ProgressDashboard(store: store, language: language),
          ProfileScreen(store: store, language: language, onLanguageChanged: (value) => setState(() => language = value)),
        ]),
        bottomNavigationBar: NavigationBar(selectedIndex: tab, onDestinationSelected: (value) => setState(() => tab = value), destinations: [
          NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home), label: strings.isBangla ? 'হোম' : 'Home'),
          NavigationDestination(icon: const Icon(Icons.menu_book_outlined), selectedIcon: const Icon(Icons.menu_book), label: strings.isBangla ? 'স্টাডি' : 'Study'),
          NavigationDestination(icon: const Icon(Icons.insights_outlined), selectedIcon: const Icon(Icons.insights), label: strings.isBangla ? 'প্রগ্রেস' : 'Progress'),
          NavigationDestination(icon: const Icon(Icons.person_outline), selectedIcon: const Icon(Icons.person), label: strings.isBangla ? 'প্রোফাইল' : 'Profile'),
        ]),
      ),
    );
  }
}

class StudyHub extends StatelessWidget {
  const StudyHub({super.key, required this.store, required this.onStartPlan, required this.onRegularStudy, required this.language});
  final LocalStore store;
  final Future<void> Function(StudyPlan plan) onStartPlan;
  final VoidCallback onRegularStudy;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final s = AppStrings(language);
    final savedCount = StudySessionStore(store).sessions.length;
    return Scaffold(
      appBar: AppBar(title: Text(s.isBangla ? 'স্টাডি' : 'Study')),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        Text(s.isBangla ? 'পরবর্তী কাজ বেছে নিন' : 'Choose your next move', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 18),
        if (savedCount > 0) ...[
          Card(child: ListTile(
            contentPadding: const EdgeInsets.all(14),
            leading: const CircleAvatar(child: Icon(Icons.bookmark_rounded)),
            title: Text(s.isBangla ? 'সেভ করা সেশন' : 'Saved sessions', style: const TextStyle(fontWeight: FontWeight.w900)),
            subtitle: Text(s.isBangla ? '$savedCountটি অসম্পূর্ণ সেশন অপেক্ষা করছে' : '$savedCount unfinished session${savedCount == 1 ? '' : 's'} waiting'),
            trailing: const Icon(Icons.chevron_right_rounded),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => SavedSessionsScreen(store: store, onOpenFocus: () => onStartPlan(store.loadPlan()!))),
          )),
          const SizedBox(height: 14),
        ],
        _Mode(icon: Icons.menu_book_rounded, title: s.isBangla ? 'রেগুলার স্টাডি' : 'Regular Study', subtitle: s.isBangla ? 'বিষয়, অধ্যায় ও ফোকাস ব্লক পরিকল্পনা করুন।' : 'Plan subjects, chapters and focus blocks.', onTap: onRegularStudy),
        _Mode(icon: Icons.auto_awesome_rounded, title: s.isBangla ? 'পরীক্ষার প্রস্তুতি' : 'Exam Preparation', subtitle: s.isBangla ? 'অগ্রাধিকারভিত্তিক পরীক্ষার পরিকল্পনা।' : 'Priority-based exam planning.', onTap: () => _openExam(context, false)),
        _Mode(icon: Icons.bolt_rounded, title: s.isBangla ? 'আগামীকালের পরীক্ষা' : 'Next Day Exam', subtitle: s.isBangla ? 'গুরুত্বপূর্ণ রিভিশন।' : 'High-impact revision.', onTap: () => _openExam(context, true)),
      ]),
    );
  }

  void _openExam(BuildContext context, bool nextDay) {
    Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: nextDay, store: store, onStartPlan: (plan) async {
      if (!context.mounted) return;
      Navigator.of(context).pop();
      await onStartPlan(plan);
    })));
  }
}

class _Mode extends StatelessWidget {
  const _Mode({required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(child: ListTile(contentPadding: const EdgeInsets.all(16), leading: CircleAvatar(child: Icon(icon)), title: Text(title, style: const TextStyle(fontWeight: FontWeight.w900)), subtitle: Text(subtitle), trailing: const Icon(Icons.chevron_right_rounded), onTap: onTap));
}
