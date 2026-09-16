import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/study_models.dart';
import 'services/local_store.dart';
import 'services/today_engine.dart';
import 'screens/exam_planner_screen.dart';
import 'screens/focus_flow.dart';
import 'screens/progress_dashboard.dart';
import 'screens/profile_screen.dart';
import 'screens/setup_screen.dart';
import 'widgets/update_gate.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(StudyOS(store: LocalStore(prefs)));
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
  const StudyOS({super.key, required this.store, this.checkForUpdate});
  final LocalStore store;
  final Future<UpdateInfo?> Function()? checkForUpdate;
  @override State<StudyOS> createState() => _StudyOSState();
}

class _StudyOSState extends State<StudyOS> {
  final navigatorKey = GlobalKey<NavigatorState>();
  late String themeKey = themes.containsKey(widget.store.themePreset) ? widget.store.themePreset : 'midnight';
  int tab = 0;

  Future<void> setTheme(String key) async {
    final theme = themes[key];
    if (theme == null) return;
    setState(() => themeKey = key);
    await widget.store.setThemePreset(key);
    await widget.store.setDarkMode(theme.brightness == Brightness.dark);
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
            Home(store: widget.store, onOpenFocus: openFocus, onRegularStudy: openRegularStudy, onExam: _openExam),
            StudyHub(store: widget.store, onStartPlan: (plan) => openFocus(plan: plan), onRegularStudy: openRegularStudy),
            ProgressDashboard(store: widget.store),
            ProfileScreen(store: widget.store, themeKey: themeKey, onThemeChanged: setTheme),
          ]),
          bottomNavigationBar: NavigationBar(
            selectedIndex: tab,
            onDestinationSelected: (value) => setState(() => tab = value),
            destinations: const [
              NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home_rounded), label: 'Home'),
              NavigationDestination(icon: Icon(Icons.menu_book_outlined), selectedIcon: Icon(Icons.menu_book_rounded), label: 'Study'),
              NavigationDestination(icon: Icon(Icons.insights_outlined), selectedIcon: Icon(Icons.insights_rounded), label: 'Progress'),
              NavigationDestination(icon: Icon(Icons.person_outline_rounded), selectedIcon: Icon(Icons.person_rounded), label: 'Profile'),
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
  const Home({super.key, required this.store, required this.onOpenFocus, required this.onRegularStudy, required this.onExam});
  final LocalStore store;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final void Function(bool) onExam;

  @override
  Widget build(BuildContext context) {
    final snapshot = TodayEngine(store).build();
    return Scaffold(
      body: SafeArea(child: ListView(padding: const EdgeInsets.all(20), children: [
        Text(_greeting(), style: Theme.of(context).textTheme.titleMedium),
        Text('Ready to focus?', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 20),
        Card(child: Padding(padding: const EdgeInsets.all(22), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('TODAY ENGINE', style: TextStyle(fontWeight: FontWeight.w900, color: Theme.of(context).colorScheme.primary, letterSpacing: 1.2)),
          const SizedBox(height: 8),
          Text(snapshot.hasPlan ? '${snapshot.remainingMinutes} min left' : 'Plan your day', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 14),
          LinearProgressIndicator(value: snapshot.progress, minHeight: 8),
          const SizedBox(height: 8),
          Text(snapshot.hasPlan ? '${snapshot.completedMinutes} min completed • ${snapshot.xp} XP' : 'Choose a study mode to begin.'),
        ]))),
        const SizedBox(height: 12),
        Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            const Expanded(child: Text("Today's goal", style: TextStyle(fontWeight: FontWeight.w900))),
            Text('${snapshot.todayCompletedMinutes} / ${snapshot.dailyGoalMinutes} min', style: const TextStyle(fontWeight: FontWeight.w900)),
          ]),
          const SizedBox(height: 10),
          LinearProgressIndicator(value: snapshot.goalProgress, minHeight: 7),
          const SizedBox(height: 8),
          Text(snapshot.dailyGoalReached ? 'Goal reached. Keep the momentum.' : '${snapshot.goalRemainingMinutes} min left today'),
        ]))),
        if (snapshot.nextItem != null) ...[
          const SizedBox(height: 16),
          Card(child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.play_arrow_rounded)),
            title: Text(snapshot.nextItem!.title, style: const TextStyle(fontWeight: FontWeight.w900)),
            subtitle: Text(snapshot.nextItem!.topic.isEmpty ? '${snapshot.nextItem!.minutes} min planned' : snapshot.nextItem!.topic),
            trailing: FilledButton(onPressed: () => onOpenFocus(), child: const Text('Start')),
          )),
        ],
        const SizedBox(height: 20),
        Text('Study modes', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 10),
        _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Subjects, topics and manual time allocation.', onTap: onRegularStudy),
        _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Priority-based focused revision.', onTap: () => onExam(false)),
        _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'High-impact revision for tomorrow.', onTap: () => onExam(true)),
        const SizedBox(height: 20),
        Card(child: ListTile(leading: const Icon(Icons.local_fire_department_rounded), title: Text('${snapshot.streak} day streak', style: const TextStyle(fontWeight: FontWeight.w900)), subtitle: Text('${snapshot.xp} XP • Level ${snapshot.level}'))),
      ])),
    );
  }

  String _greeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }
}

class _Mode extends StatelessWidget {
  const _Mode({required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(
    onTap: onTap,
    contentPadding: const EdgeInsets.all(12),
    leading: CircleAvatar(radius: 27, child: Icon(icon)),
    title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
    subtitle: Text(subtitle),
    trailing: const Icon(Icons.chevron_right_rounded),
  ));
}

class StudyHub extends StatelessWidget {
  const StudyHub({super.key, required this.store, required this.onStartPlan, required this.onRegularStudy});
  final LocalStore store;
  final Future<void> Function(StudyPlan plan) onStartPlan;
  final VoidCallback onRegularStudy;
  @override
  Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Study')), body: ListView(padding: const EdgeInsets.all(20), children: [
    Text('Choose your next move', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
    const SizedBox(height: 18),
    _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Plan subjects, chapters and focus blocks.', onTap: onRegularStudy),
    _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Priority-based exam planning.', onTap: () => _openExam(context, false)),
    _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'High-impact revision.', onTap: () => _openExam(context, true)),
  ]));

  void _openExam(BuildContext context, bool nextDay) {
    Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(
      nextDay: nextDay,
      store: store,
      onStartPlan: (plan) async {
        if (!context.mounted) return;
        Navigator.of(context).pop();
        await onStartPlan(plan);
      },
    )));
  }
}
