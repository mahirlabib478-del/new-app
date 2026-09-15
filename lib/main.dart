import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/study_models.dart';
import 'services/local_store.dart';
import 'services/today_engine.dart';
import 'screens/exam_planner_screen.dart';
import 'screens/focus_flow.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(StudyOS(store: LocalStore(prefs)));
}

class AppTheme {
  const AppTheme({required this.name, required this.description, required this.icon, required this.seed, required this.brightness});
  final String name, description;
  final IconData icon;
  final Color seed;
  final Brightness brightness;
  ThemeData get data => ThemeData(
    useMaterial3: true,
    colorSchemeSeed: seed,
    brightness: brightness,
    scaffoldBackgroundColor: brightness == Brightness.dark ? const Color(0xFF0B0D13) : null,
    cardTheme: CardThemeData(margin: EdgeInsets.zero, elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22))),
    inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder()),
  );
}

const appThemes = <String, AppTheme>{
  'midnight': AppTheme(name: 'Midnight', description: 'Deep focus with a calm dark canvas.', icon: Icons.nights_stay_rounded, seed: Color(0xFF6C63FF), brightness: Brightness.dark),
  'ocean': AppTheme(name: 'Ocean', description: 'Cool, clear and distraction-free.', icon: Icons.water_rounded, seed: Color(0xFF1479A8), brightness: Brightness.dark),
  'forest': AppTheme(name: 'Forest', description: 'Grounded green tones for long sessions.', icon: Icons.forest_rounded, seed: Color(0xFF3F7D58), brightness: Brightness.dark),
  'sunrise': AppTheme(name: 'Sunrise', description: 'Warm, bright and energetic.', icon: Icons.wb_sunny_rounded, seed: Color(0xFFE4774E), brightness: Brightness.light),
};

class StudyOS extends StatefulWidget {
  const StudyOS({super.key, required this.store});
  final LocalStore store;
  @override State<StudyOS> createState() => _StudyOSState();
}
class _StudyOSState extends State<StudyOS> {
  late String themeKey = appThemes.containsKey(widget.store.themePreset) ? widget.store.themePreset : 'midnight';
  int tab = 0;
  AppTheme get selectedTheme => appThemes[themeKey]!;
  void setTheme(String key) async { if (!appThemes.containsKey(key)) return; setState(() => themeKey = key); await widget.store.setThemePreset(key); await widget.store.setDarkMode(appThemes[key]!.brightness == Brightness.dark); }
  void startPlan(StudyPlan plan) { final s = TodayEngine(widget.store).build(); Navigator.of(context).push(MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: plan, index: s.currentIndex, blockIndex: s.currentBlockIndex))); }
  @override Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false, title: 'Study OS', theme: selectedTheme.data, darkTheme: selectedTheme.data,
    themeMode: selectedTheme.brightness == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
    home: Scaffold(
      body: [Home(store: widget.store, onTheme: () => setTheme(themeKey == 'midnight' ? 'sunrise' : 'midnight')), StudyHub(store: widget.store, onStartPlan: startPlan), ProgressScreen(store: widget.store), ProfileScreen(store: widget.store, themeKey: themeKey, onThemeChanged: setTheme)][tab],
      bottomNavigationBar: NavigationBar(selectedIndex: tab, onDestinationSelected: (i) => setState(() => tab = i), destinations: const [NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home_rounded), label: 'Home'), NavigationDestination(icon: Icon(Icons.menu_book_outlined), selectedIcon: Icon(Icons.menu_book_rounded), label: 'Study'), NavigationDestination(icon: Icon(Icons.insights_outlined), selectedIcon: Icon(Icons.insights_rounded), label: 'Progress'), NavigationDestination(icon: Icon(Icons.person_outline_rounded), selectedIcon: Icon(Icons.person_rounded), label: 'Profile')]),
    ),
  );
}

class Home extends StatelessWidget {
  const Home({super.key, required this.store, required this.onTheme});
  final LocalStore store; final VoidCallback onTheme;
  @override Widget build(BuildContext context) {
    final s = TodayEngine(store).build(); final plan = s.plan;
    return Scaffold(body: SafeArea(child: ListView(padding: const EdgeInsets.all(20), children: [
      Row(children: [Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(_greeting(), style: Theme.of(context).textTheme.titleMedium), Text('Ready to focus?', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900))])), IconButton(onPressed: onTheme, icon: const Icon(Icons.brightness_6_rounded)), const CircleAvatar(child: Icon(Icons.person_rounded))]),
      const SizedBox(height: 22), _Hero(snapshot: s),
      const SizedBox(height: 20),
      if (s.hasPlan) _NextUp(snapshot: s, onStart: () => Navigator.push(context, MaterialPageRoute(builder: (_) => FocusScreen(store: store, plan: plan!, index: s.currentIndex, blockIndex: s.currentBlockIndex)))),
      if (s.hasPlan) const SizedBox(height: 20),
      Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text('Study modes', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), Text('${s.streak} day streak', style: Theme.of(context).textTheme.labelLarge)]), const SizedBox(height: 12),
      _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Build subjects, topics and focused sessions.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => Setup(store: store)))),
      _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Build a focused exam plan around your priorities.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: false, store: store, onStartPlan: (p) => Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: store, plan: p, index: 0, blockIndex: 0)))))),
      _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'Prioritize the highest-impact revision for tomorrow.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: true, store: store, onStartPlan: (p) => Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: store, plan: p, index: 0, blockIndex: 0)))))),
      const SizedBox(height: 18), Text('Today', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 10),
      Card(child: Padding(padding: const EdgeInsets.all(16), child: plan == null ? const ListTile(leading: Icon(Icons.event_note_rounded, size: 30), title: Text('No plan yet'), subtitle: Text('Create a plan to get your day moving.')) : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Row(children: [const Icon(Icons.event_note_rounded, size: 30), const SizedBox(width: 12), Expanded(child: Text('${plan.items.length} focus blocks planned', style: const TextStyle(fontWeight: FontWeight.w900))), Text('${s.completedMinutes}/${plan.allocatedMinutes}m', style: const TextStyle(fontWeight: FontWeight.w800))]), const SizedBox(height: 14), LinearProgressIndicator(value: s.progress, minHeight: 8), const SizedBox(height: 10), Text(s.remainingMinutes == 0 ? 'Plan complete — great work.' : '${s.remainingMinutes} minutes remaining'), const SizedBox(height: 12), SizedBox(width: double.infinity, child: OutlinedButton.icon(onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => FocusScreen(store: store, plan: plan, index: s.currentIndex, blockIndex: s.currentBlockIndex))), icon: Icon(s.completedMinutes == 0 ? Icons.play_arrow_rounded : Icons.play_circle_outline_rounded), label: Text(s.completedMinutes == 0 ? 'Start today\'s plan' : 'Resume plan')))]))),
    ])));
  }
  String _greeting() { final h = DateTime.now().hour; if (h < 12) return 'Good morning'; if (h < 18) return 'Good afternoon'; return 'Good evening'; }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.snapshot}); final TodaySnapshot snapshot;
  @override Widget build(BuildContext context) { final c = Theme.of(context).colorScheme; return TweenAnimationBuilder<double>(tween: Tween(begin: 0, end: snapshot.progress), duration: const Duration(milliseconds: 650), curve: Curves.easeOutCubic, builder: (_, value, __) => Container(padding: const EdgeInsets.all(22), decoration: BoxDecoration(gradient: LinearGradient(colors: [c.primary, c.secondary]), borderRadius: BorderRadius.circular(28)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('TODAY ENGINE', style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.w800, letterSpacing: 1.2)), const SizedBox(height: 7), Text(snapshot.hasPlan ? '${snapshot.remainingMinutes} min left' : 'Plan your day', style: const TextStyle(color: Colors.white, fontSize: 36, fontWeight: FontWeight.w900)), const SizedBox(height: 14), ClipRRect(borderRadius: BorderRadius.circular(99), child: LinearProgressIndicator(value: value, minHeight: 9, backgroundColor: Colors.white24)), const SizedBox(height: 9), Text(snapshot.hasPlan ? '${snapshot.completedMinutes} min completed • ${snapshot.xp} XP' : 'Choose a study mode to begin.', style: const TextStyle(color: Colors.white70))]))); }
}

class _NextUp extends StatelessWidget { const _NextUp({required this.snapshot, required this.onStart}); final TodaySnapshot snapshot; final VoidCallback onStart; @override Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('NEXT UP', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1.1)), const SizedBox(height: 8), ListTile(contentPadding: EdgeInsets.zero, leading: const CircleAvatar(child: Icon(Icons.play_arrow_rounded)), title: Text(snapshot.nextItem?.title ?? 'Ready', style: const TextStyle(fontWeight: FontWeight.w900)), subtitle: Text(snapshot.nextItem?.topic?.isNotEmpty == true ? snapshot.nextItem!.topic : '${snapshot.nextItem?.minutes ?? 0} minute focus block'), trailing: Text('${snapshot.nextItem?.minutes ?? 0}m', style: const TextStyle(fontWeight: FontWeight.w900)), onTap: onStart), SizedBox(width: double.infinity, child: FilledButton.icon(onPressed: onStart, icon: const Icon(Icons.play_arrow_rounded), label: const Text('Continue studying'))]))); }
class _Mode extends StatelessWidget { const _Mode({required this.icon, required this.title, required this.subtitle, required this.onTap}); final IconData icon; final String title, subtitle; final VoidCallback onTap; @override Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(onTap: onTap, contentPadding: const EdgeInsets.all(12), leading: CircleAvatar(radius: 27, child: Icon(icon)), title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(subtitle), trailing: const Icon(Icons.chevron_right_rounded))); }

class StudyHub extends StatelessWidget { const StudyHub({super.key, required this.store, required this.onStartPlan}); final LocalStore store; final void Function(StudyPlan) onStartPlan; @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Study')), body: ListView(padding: const EdgeInsets.all(20), children: [Text('Choose your next move', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 18), _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Plan subjects, chapters and focus blocks.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => Setup(store: store)))), _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Multi-day exam planning.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: false, store: store, onStartPlan: onStartPlan)))), _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'High-impact revision for tomorrow.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: true, store: store, onStartPlan: onStartPlan))))])); }

class ProgressScreen extends StatelessWidget { const ProgressScreen({super.key, required this.store}); final LocalStore store; @override Widget build(BuildContext context) { final xp = store.xp; return Scaffold(appBar: AppBar(title: const Text('Progress')), body: ListView(padding: const EdgeInsets.all(20), children: [Text('Your momentum', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 16), Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Level ${store.level}', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), Text('$xp XP total'), const SizedBox(height: 14), LinearProgressIndicator(value: store.levelProgress / 250, minHeight: 9), const SizedBox(height: 8), Text('${store.levelProgress} / 250 XP to next level')])), const SizedBox(height: 14), Row(children: [Expanded(child: _Stat(icon: Icons.local_fire_department_rounded, value: '${store.streak}', label: 'Day streak')), const SizedBox(width: 10), Expanded(child: _Stat(icon: Icons.timer_rounded, value: '${store.completedMinutes}', label: 'Minutes')), const SizedBox(width: 10), Expanded(child: _Stat(icon: Icons.check_circle_rounded, value: '${store.sessions}', label: 'Sessions'))]), const SizedBox(height: 24), Text('Achievements', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), ...[('First Focus', store.sessions >= 1), ('60 Minutes', store.completedMinutes >= 60), ('250 XP', xp >= 250), ('3 Day Streak', store.streak >= 3), ('10 Sessions', store.sessions >= 10)].map((a) => Card(child: ListTile(leading: CircleAvatar(child: Icon(a.$2 ? Icons.emoji_events_rounded : Icons.lock_outline_rounded)), title: Text(a.$1), trailing: Icon(a.$2 ? Icons.check_circle_rounded : Icons.chevron_right_rounded))))])); } }
class _Stat extends StatelessWidget { const _Stat({required this.icon, required this.value, required this.label}); final IconData icon; final String value, label; @override Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 8), child: Column(children: [Icon(icon), const SizedBox(height: 6), Text(value, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), Text(label, style: Theme.of(context).textTheme.labelSmall)]))); }

class ProfileScreen extends StatelessWidget { const ProfileScreen({super.key, required this.store, required this.themeKey, required this.onThemeChanged}); final LocalStore store; final String themeKey; final void Function(String) onThemeChanged; @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Profile')), body: ListView(padding: const EdgeInsets.all(20), children: [const CircleAvatar(radius: 42, child: Icon(Icons.person_rounded, size: 44)), const SizedBox(height: 14), const Center(child: Text('Study OS learner', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900))), const SizedBox(height: 28), Text('App theme', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 10), ...appThemes.entries.map((entry) { final theme = entry.value; final selected = entry.key == themeKey; return Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(onTap: () => onThemeChanged(entry.key), leading: CircleAvatar(child: Icon(theme.icon)), title: Text(theme.name, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(theme.description), trailing: Icon(selected ? Icons.check_circle_rounded : Icons.chevron_right_rounded)); }), const SizedBox(height: 18), const Card(child: ListTile(leading: Icon(Icons.offline_bolt_rounded), title: Text('Offline-first'), subtitle: Text('Your study data stays on this device.'))), const Card(child: ListTile(leading: Icon(Icons.verified_rounded), title: Text('Zero-cost foundation'), subtitle: Text('No backend or paid API required for the core app.'))])); }

class Setup extends StatefulWidget { const Setup({super.key, required this.store}); final LocalStore store; @override State<Setup> createState() => _SetupState(); }
class _SetupState extends State<Setup> { int total = 120; final subjects = <String>[]; final input = TextEditingController(); void add() { final s = input.text.trim(); if (s.isNotEmpty && !subjects.contains(s)) setState(() { subjects.add(s); input.clear(); }); } @override void dispose() { input.dispose(); super.dispose(); } @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Regular Study')), body: const SizedBox()); }
