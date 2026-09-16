import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/study_models.dart';
import 'services/app_language.dart';
import 'services/local_store.dart';
import 'services/reminder_coordinator.dart';
import 'services/reminder_settings.dart';
import 'services/notification_service.dart';
import 'services/study_session_store.dart';
import 'services/today_engine.dart';
import 'screens/exam_planner_screen.dart';
import 'screens/focus_flow.dart';
import 'screens/progress_dashboard.dart';
import 'screens/profile_screen.dart';
import 'screens/saved_sessions_screen.dart';
import 'screens/setup_screen.dart';
import 'widgets/update_gate.dart';
import 'widgets/attractive_home.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(StudyOS(store: LocalStore(prefs), prefs: prefs));
}

const themes = <String, _AppTheme>{
  'midnight': _AppTheme('Midnight', Icons.nights_stay_rounded, Color(0xFF6C63FF), Brightness.dark),
  'ocean': _AppTheme('Ocean', Icons.water_rounded, Color(0xFF1479A8), Brightness.dark),
  'forest': _AppTheme('Forest', Icons.forest_rounded, Color(0xFF3F7D58), Brightness.dark),
  'sunrise': _AppTheme('Sunrise', Icons.wb_sunny_rounded, Color(0xFFE4774E), Brightness.light),
};

class _AppTheme {
  const _AppTheme(this.name, this.icon, this.seed, this.brightness);
  final String name;
  final IconData icon;
  final Color seed;
  final Brightness brightness;
}

class StudyOS extends StatefulWidget {
  const StudyOS({super.key, required this.store, required this.prefs, this.checkForUpdate});
  final LocalStore store;
  final SharedPreferences prefs;
  final Future<UpdateInfo?> Function()? checkForUpdate;
  @override State<StudyOS> createState() => _StudyOSState();
}

class _StudyOSState extends State<StudyOS> {
  final navigatorKey = GlobalKey<NavigatorState>();
  late final ReminderCoordinator reminderCoordinator;
  late String themeKey = themes.containsKey(widget.store.themePreset) ? widget.store.themePreset : 'midnight';
  late AppLanguage language = widget.store.appLanguage;
  int tab = 0;

  AppStrings get strings => AppStrings(language);

  @override
  void initState() {
    super.initState();
    reminderCoordinator = ReminderCoordinator(
      store: widget.store,
      settingsStore: ReminderSettingsStore(widget.prefs),
      scheduler: NotificationService(),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(reminderCoordinator.sync());
    });
  }

  Future<void> setTheme(String key) async {
    final theme = themes[key];
    if (theme == null) return;
    setState(() => themeKey = key);
    await widget.store.setThemePreset(key);
    await widget.store.setDarkMode(theme.brightness == Brightness.dark);
  }

  Future<void> setLanguage(AppLanguage value) async {
    await widget.store.setAppLanguage(value);
    if (mounted) setState(() => language = value);
  }

  Future<void> openFocus({StudyPlan? plan}) async {
    final snapshot = TodayEngine(widget.store).build();
    final activePlan = plan ?? snapshot.plan;
    if (activePlan == null || activePlan.items.isEmpty) return;
    if (plan == null && snapshot.isComplete) return;
    await navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => FocusScreen(
      store: widget.store,
      plan: activePlan,
      index: plan == null ? snapshot.currentIndex : 0,
      blockIndex: plan == null ? snapshot.currentBlockIndex : 0,
      onFocusBlockCompleted: reminderCoordinator.notifyFocusBlockCompleted,
    )));
    if (mounted) setState(() {});
  }

  void openRegularStudy() {
    navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => Setup(
      store: widget.store,
      onStartPlan: (plan) async {
        if (!mounted) return;
        navigatorKey.currentState?.pop();
        await openFocus(plan: plan);
      },
    )));
  }

  @override
  Widget build(BuildContext context) {
    final theme = themes[themeKey]!;
    return MaterialApp(
      navigatorKey: navigatorKey,
      debugShowCheckedModeBanner: false,
      title: 'Study OS',
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: theme.seed,
        brightness: theme.brightness,
        scaffoldBackgroundColor: theme.brightness == Brightness.dark ? const Color(0xFF0B0D13) : null,
        cardTheme: CardThemeData(margin: EdgeInsets.zero, elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22))),
        inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder()),
      ),
      home: UpdateGate(
        store: widget.store,
        checkForUpdate: widget.checkForUpdate,
        child: Scaffold(
          body: IndexedStack(index: tab, children: [
            Home(store: widget.store, onOpenFocus: openFocus, onRegularStudy: openRegularStudy, onExam: _openExam, language: language),
            StudyHub(store: widget.store, onStartPlan: (plan) => openFocus(plan: plan), onRegularStudy: openRegularStudy, language: language),
            ProgressDashboard(store: widget.store),
            ProfileScreen(store: widget.store, prefs: widget.prefs, themeKey: themeKey, onThemeChanged: setTheme, language: language, onLanguageChanged: setLanguage),
          ]),
          bottomNavigationBar: NavigationBar(
            selectedIndex: tab,
            onDestinationSelected: (value) => setState(() => tab = value),
            destinations: [
              NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home_rounded), label: strings.isBangla ? 'হোম' : 'Home'),
              NavigationDestination(icon: const Icon(Icons.menu_book_outlined), selectedIcon: const Icon(Icons.menu_book_rounded), label: strings.isBangla ? 'স্টাডি' : 'Study'),
              NavigationDestination(icon: const Icon(Icons.insights_outlined), selectedIcon: const Icon(Icons.insights_rounded), label: strings.isBangla ? 'অগ্রগতি' : 'Progress'),
              NavigationDestination(icon: const Icon(Icons.person_outline_rounded), selectedIcon: const Icon(Icons.person_rounded), label: strings.profile),
            ],
          ),
        ),
      ),
    );
  }

  void _openExam(bool nextDay) {
    navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => ExamPlannerScreen(
      nextDay: nextDay,
      store: widget.store,
      onStartPlan: (plan) async {
        if (!mounted) return;
        navigatorKey.currentState?.pop();
        await openFocus(plan: plan);
      },
    )));
  }
}

class Home extends StatelessWidget {
  const Home({super.key, required this.store, required this.onOpenFocus, required this.onRegularStudy, required this.onExam, required this.language});
  final LocalStore store;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final void Function(bool) onExam;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) => AttractiveHome(
    store: store,
    language: language,
    onOpenFocus: onOpenFocus,
    onRegularStudy: onRegularStudy,
    onExam: onExam,
  );
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
    return Scaffold(appBar: AppBar(title: Text(s.isBangla ? 'স্টাডি' : 'Study')), body: ListView(padding: const EdgeInsets.all(20), children: [
      Text(s.isBangla ? 'পরবর্তী কাজ বেছে নিন' : 'Choose your next move', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
      const SizedBox(height: 18),
      if (savedCount > 0) ...[
        Card(
          child: ListTile(
            contentPadding: const EdgeInsets.all(14),
            leading: CircleAvatar(child: const Icon(Icons.bookmark_rounded)),
            title: Text(s.isBangla ? 'সেভ করা সেশন' : 'Saved sessions', style: const TextStyle(fontWeight: FontWeight.w900)),
            subtitle: Text(s.isBangla ? '$savedCountটি অসম্পূর্ণ সেশন অপেক্ষা করছে' : '$savedCount unfinished session${savedCount == 1 ? '' : 's'} waiting'),
            trailing: const Icon(Icons.chevron_right_rounded),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => SavedSessionsScreen(
              store: store,
              onOpenFocus: () => onStartPlan(store.loadPlan()!),
            ))),
          ),
        ),
        const SizedBox(height: 14),
      ],
      _Mode(icon: Icons.menu_book_rounded, title: s.isBangla ? 'রেগুলার স্টাডি' : 'Regular Study', subtitle: s.isBangla ? 'বিষয়, অধ্যায় ও ফোকাস ব্লক পরিকল্পনা করুন।' : 'Plan subjects, chapters and focus blocks.', onTap: onRegularStudy),
      _Mode(icon: Icons.auto_awesome_rounded, title: s.isBangla ? 'পরীক্ষার প্রস্তুতি' : 'Exam Preparation', subtitle: s.isBangla ? 'অগ্রাধিকারভিত্তিক পরীক্ষার পরিকল্পনা।' : 'Priority-based exam planning.', onTap: () => _openExam(context, false)),
      _Mode(icon: Icons.bolt_rounded, title: s.isBangla ? 'আগামীকালের পরীক্ষা' : 'Next Day Exam', subtitle: s.isBangla ? 'গুরুত্বপূর্ণ রিভিশন।' : 'High-impact revision.', onTap: () => _openExam(context, true)),
    ]));
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
  @override Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(onTap: onTap, contentPadding: const EdgeInsets.all(12), leading: CircleAvatar(radius: 27, child: Icon(icon)), title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(subtitle), trailing: const Icon(Icons.chevron_right_rounded)));
}