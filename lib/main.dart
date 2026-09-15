import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/study_models.dart';
import 'services/local_store.dart';
import 'services/today_engine.dart';
import 'screens/exam_planner_screen.dart';
import 'screens/focus_flow.dart';
import 'screens/regular_study_planner.dart';

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
        cardTheme: CardThemeData(
          margin: EdgeInsets.zero,
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        ),
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

  @override
  State<StudyOS> createState() => _StudyOSState();
}

class _StudyOSState extends State<StudyOS> {
  late String themeKey = appThemes.containsKey(widget.store.themePreset) ? widget.store.themePreset : 'midnight';
  int tab = 0;

  AppTheme get selectedTheme => appThemes[themeKey]!;

  Future<void> setTheme(String key) async {
    if (!appThemes.containsKey(key)) return;
    setState(() => themeKey = key);
    await widget.store.setThemePreset(key);
    await widget.store.setDarkMode(appThemes[key]!.brightness == Brightness.dark);
  }

  Future<void> openFocus({StudyPlan? plan}) async {
    final activePlan = plan ?? TodayEngine(widget.store).build().plan;
    if (activePlan == null || activePlan.items.isEmpty) return;
    final snapshot = TodayEngine(widget.store).build();
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => FocusScreen(
          store: widget.store,
          plan: activePlan,
          index: snapshot.currentIndex,
          blockIndex: snapshot.currentBlockIndex,
        )));
    if (mounted) setState(() {});
  }

  void openRegularStudy() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => Setup(
          store: widget.store,
          onStartPlan: (plan) async {
            if (!mounted) return;
            Navigator.pop(context);
            await openFocus(plan: plan);
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Study OS',
      theme: selectedTheme.data,
      darkTheme: selectedTheme.data,
      themeMode: selectedTheme.brightness == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
      home: Scaffold(
        body: [
          Home(store: widget.store, onTheme: () => setTheme(themeKey == 'midnight' ? 'sunrise' : 'midnight'), onOpenFocus: openFocus, onRegularStudy: openRegularStudy),
          StudyHub(store: widget.store, onStartPlan: (plan) => openFocus(plan: plan), onRegularStudy: openRegularStudy),
          ProgressScreen(store: widget.store),
          ProfileScreen(store: widget.store, themeKey: themeKey, onThemeChanged: setTheme),
        ][tab],
        bottomNavigationBar: NavigationBar(
          selectedIndex: tab,
          onDestinationSelected: (i) => setState(() => tab = i),
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home_rounded), label: 'Home'),
            NavigationDestination(icon: Icon(Icons.menu_book_outlined), selectedIcon: Icon(Icons.menu_book_rounded), label: 'Study'),
            NavigationDestination(icon: Icon(Icons.insights_outlined), selectedIcon: Icon(Icons.insights_rounded), label: 'Progress'),
            NavigationDestination(icon: Icon(Icons.person_outline_rounded), selectedIcon: Icon(Icons.person_rounded), label: 'Profile'),
          ],
        ),
      ),
    );
  }
}

class Home extends StatelessWidget {
  const Home({super.key, required this.store, required this.onTheme, required this.onOpenFocus, required this.onRegularStudy});
  final LocalStore store;
  final VoidCallback onTheme;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;

  @override
  Widget build(BuildContext context) {
    final snapshot = TodayEngine(store).build();
    final plan = snapshot.plan;
    final canContinue = snapshot.hasRemainingWork;

    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Row(children: [
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(_greeting(), style: Theme.of(context).textTheme.titleMedium),
                Text('Ready to focus?', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
              ])),
              IconButton(onPressed: onTheme, icon: const Icon(Icons.brightness_6_rounded)),
              const CircleAvatar(child: Icon(Icons.person_rounded)),
            ]),
            const SizedBox(height: 22),
            _Hero(snapshot: snapshot),
            const SizedBox(height: 20),
            if (snapshot.nextItem != null) ...[
              _NextUp(snapshot: snapshot, onStart: () => onOpenFocus()),
              const SizedBox(height: 20),
            ],
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              Text('Study modes', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
              Text('${snapshot.streak} day streak', style: Theme.of(context).textTheme.labelLarge),
            ]),
            const SizedBox(height: 12),
            _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Build subjects, topics and focused sessions.', onTap: onRegularStudy),
            _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Build a focused exam plan around your priorities.', onTap: () => _openExam(context, false)),
            _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'Prioritize the highest-impact revision for tomorrow.', onTap: () => _openExam(context, true)),
            const SizedBox(height: 18),
            Text('Today', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 10),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: plan == null
                    ? const ListTile(leading: Icon(Icons.event_note_rounded, size: 30), title: Text('No plan yet'), subtitle: Text('Create a plan to get your day moving.'))
                    : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [
                          const Icon(Icons.event_note_rounded, size: 30),
                          const SizedBox(width: 12),
                          Expanded(child: Text('${plan.items.length} focus items planned', style: const TextStyle(fontWeight: FontWeight.w900))),
                          Text('${snapshot.completedMinutes}/${plan.allocatedMinutes}m', style: const TextStyle(fontWeight: FontWeight.w800)),
                        ]),
                        const SizedBox(height: 14),
                        LinearProgressIndicator(value: snapshot.progress, minHeight: 8),
                        const SizedBox(height: 10),
                        Text(snapshot.isComplete ? 'Plan complete — great work.' : '${snapshot.remainingMinutes} minutes remaining'),
                        const SizedBox(height: 12),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton.icon(
                            onPressed: canContinue ? () => onOpenFocus() : null,
                            icon: Icon(snapshot.completedMinutes == 0 ? Icons.play_arrow_rounded : Icons.play_circle_outline_rounded),
                            label: Text(canContinue ? (snapshot.completedMinutes == 0 ? "Start today's plan" : 'Resume plan') : 'Plan completed'),
                          ),
                        ),
                      ]),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _openExam(BuildContext context, bool nextDay) {
    Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(
          nextDay: nextDay,
          store: store,
          onStartPlan: (plan) async {
            if (!context.mounted) return;
            Navigator.pop(context);
            await onOpenFocus(plan: plan);
          },
        )));
  }

  String _greeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.snapshot});
  final TodaySnapshot snapshot;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: [scheme.primary, scheme.secondary]),
        borderRadius: BorderRadius.circular(28),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Text('TODAY ENGINE', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.w800, letterSpacing: 1.2)),
        const SizedBox(height: 7),
        Text(snapshot.hasPlan ? '${snapshot.remainingMinutes} min left' : 'Plan your day', style: const TextStyle(color: Colors.white, fontSize: 36, fontWeight: FontWeight.w900)),
        const SizedBox(height: 14),
        ClipRRect(borderRadius: BorderRadius.circular(99), child: LinearProgressIndicator(value: snapshot.progress, minHeight: 9, backgroundColor: Colors.white24)),
        const SizedBox(height: 9),
        Text(snapshot.hasPlan ? '${snapshot.completedMinutes} min completed • ${snapshot.xp} XP' : 'Choose a study mode to begin.', style: const TextStyle(color: Colors.white70)),
      ]),
    );
  }
}

class _NextUp extends StatelessWidget {
  const _NextUp({required this.snapshot, required this.onStart});
  final TodaySnapshot snapshot;
  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('NEXT UP', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1.1)),
            const SizedBox(height: 8),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const CircleAvatar(child: Icon(Icons.play_arrow_rounded)),
              title: Text(snapshot.nextItem?.title ?? 'Ready', style: const TextStyle(fontWeight: FontWeight.w900)),
              subtitle: Text(snapshot.nextItem?.topic.isNotEmpty == true ? snapshot.nextItem!.topic : '${snapshot.nextItem?.minutes ?? 0} minute focus block'),
              trailing: Text('${snapshot.nextItem?.minutes ?? 0}m', style: const TextStyle(fontWeight: FontWeight.w900)),
              onTap: onStart,
            ),
            SizedBox(width: double.infinity, child: FilledButton.icon(onPressed: onStart, icon: const Icon(Icons.play_arrow_rounded), label: const Text('Continue studying'))),
          ]),
        ),
      );
}

class _Mode extends StatelessWidget {
  const _Mode({required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon;
  final String title, subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
        margin: const EdgeInsets.only(bottom: 10),
        child: ListTile(
          onTap: onTap,
          contentPadding: const EdgeInsets.all(12),
          leading: CircleAvatar(radius: 27, child: Icon(icon)),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text(subtitle),
          trailing: const Icon(Icons.chevron_right_rounded),
        ),
      );
}

class StudyHub extends StatelessWidget {
  const StudyHub({super.key, required this.store, required this.onStartPlan, required this.onRegularStudy});
  final LocalStore store;
  final Future<void> Function(StudyPlan) onStartPlan;
  final VoidCallback onRegularStudy;

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Study')),
        body: ListView(padding: const EdgeInsets.all(20), children: [
          Text('Choose your next move', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 18),
          _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Plan subjects, chapters and focus blocks.', onTap: onRegularStudy),
          _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Multi-day exam planning.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: false, store: store, onStartPlan: onStartPlan)))),
          _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'High-impact revision for tomorrow.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: true, store: store, onStartPlan: onStartPlan)))),
        ]),
      );
}

class ProgressScreen extends StatelessWidget {
  const ProgressScreen({super.key, required this.store});
  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final plan = store.loadPlan();
    final planned = plan?.allocatedMinutes ?? 0;
    final completed = plan == null ? 0 : store.planCompletedMinutes.clamp(0, planned).toInt();
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0).toDouble();
    final levelProgress = (store.levelProgress / 250).clamp(0.0, 1.0).toDouble();

    return Scaffold(
      appBar: AppBar(title: const Text('Progress')),
      body: ListView(padding: const EdgeInsets.fromLTRB(20, 8, 20, 28), children: [
        Text('Your momentum', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 6),
        const Text('See what you finished today and how your consistency is growing.'),
        const SizedBox(height: 18),
        _ProgressHero(completed: completed, planned: planned, progress: progress),
        const SizedBox(height: 14),
        Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [CircleAvatar(child: Text('${store.level}', style: const TextStyle(fontWeight: FontWeight.w900))), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Level ${store.level}', style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 18)), Text('${store.xp} XP total')]))]),
          const SizedBox(height: 16),
          LinearProgressIndicator(value: levelProgress, minHeight: 9),
          const SizedBox(height: 8),
          Text('${store.levelProgress} / 250 XP to next level'),
        ]))),
        const SizedBox(height: 14),
        Row(children: [
          Expanded(child: _Stat(icon: Icons.local_fire_department_rounded, value: '${store.streak}', label: 'Day streak')),
          const SizedBox(width: 10),
          Expanded(child: _Stat(icon: Icons.today_rounded, value: '$completed', label: 'Today min')),
          const SizedBox(width: 10),
          Expanded(child: _Stat(icon: Icons.timer_rounded, value: '${store.completedMinutes}', label: 'Total min')),
        ]),
        const SizedBox(height: 10),
        Card(child: ListTile(leading: const Icon(Icons.check_circle_rounded), title: const Text('Focus blocks completed', style: TextStyle(fontWeight: FontWeight.w800)), trailing: Text('${store.sessions}', style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 18)))),
        const SizedBox(height: 24),
        Text("Today's breakdown", style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 8),
        if (plan == null || plan.items.isEmpty)
          const Card(child: Padding(padding: EdgeInsets.all(18), child: Row(children: [Icon(Icons.event_note_rounded), SizedBox(width: 12), Expanded(child: Text('Create a study plan to see subject and topic progress here.'))])))
        else
          ...plan.items.asMap().entries.map((entry) => _ProgressItem(item: entry.value, completed: store.itemCompletedMinutes(entry.key))),
        const SizedBox(height: 24),
        Text('Milestones', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 8),
        _Milestone(title: 'First Focus', subtitle: 'Complete your first focus block', unlocked: store.sessions >= 1),
        _Milestone(title: '60 Minutes', subtitle: 'Complete 60 minutes of focus', unlocked: store.completedMinutes >= 60),
        _Milestone(title: '250 XP', subtitle: 'Earn 250 XP', unlocked: store.xp >= 250),
        _Milestone(title: '3 Day Streak', subtitle: 'Study on 3 consecutive days', unlocked: store.streak >= 3),
        _Milestone(title: '10 Focus Blocks', subtitle: 'Complete 10 focus blocks', unlocked: store.sessions >= 10),
        _Milestone(title: 'Level 5', subtitle: 'Reach level 5', unlocked: store.level >= 5),
      ]),
    );
  }
}

class _ProgressHero extends StatelessWidget {
  const _ProgressHero({required this.completed, required this.planned, required this.progress});
  final int completed, planned;
  final double progress;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(gradient: LinearGradient(colors: [scheme.primary, scheme.secondary]), borderRadius: BorderRadius.circular(26)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Text('TODAY', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.w800, letterSpacing: 1.2)),
        const SizedBox(height: 6),
        Text(planned == 0 ? 'No plan yet' : '$completed / $planned min', style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w900)),
        const SizedBox(height: 12),
        ClipRRect(borderRadius: BorderRadius.circular(99), child: LinearProgressIndicator(value: progress, minHeight: 9, backgroundColor: Colors.white24)),
        const SizedBox(height: 8),
        Text(planned == 0 ? 'Build a plan to start tracking your day.' : '${(progress * 100).round()}% complete', style: const TextStyle(color: Colors.white70)),
      ]),
    );
  }
}

class _ProgressItem extends StatelessWidget {
  const _ProgressItem({required this.item, required this.completed});
  final StudyItem item;
  final int completed;

  @override
  Widget build(BuildContext context) {
    final planned = item.minutes;
    final done = completed.clamp(0, planned).toInt();
    final progress = planned <= 0 ? 0.0 : (done / planned).clamp(0.0, 1.0).toDouble();
    final complete = done >= planned && planned > 0;
    final topic = item.topic.trim();
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          CircleAvatar(radius: 20, child: Icon(complete ? Icons.check_rounded : Icons.menu_book_rounded, size: 20)),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(item.title, style: const TextStyle(fontWeight: FontWeight.w900)), if (topic.isNotEmpty) Text(topic, maxLines: 1, overflow: TextOverflow.ellipsis, style: Theme.of(context).textTheme.bodySmall)])),
          Text('$done/$planned m', style: const TextStyle(fontWeight: FontWeight.w900)),
        ]),
        const SizedBox(height: 12),
        LinearProgressIndicator(value: progress, minHeight: 7),
        const SizedBox(height: 7),
        Text(complete ? 'Complete' : '${(progress * 100).round()}% complete • ${planned - done} min left', style: Theme.of(context).textTheme.bodySmall),
      ])),
    );
  }
}

class _Milestone extends StatelessWidget {
  const _Milestone({required this.title, required this.subtitle, required this.unlocked});
  final String title, subtitle;
  final bool unlocked;

  @override
  Widget build(BuildContext context) => Card(
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          leading: CircleAvatar(child: Icon(unlocked ? Icons.emoji_events_rounded : Icons.lock_outline_rounded)),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text(subtitle),
          trailing: Icon(unlocked ? Icons.check_circle_rounded : Icons.chevron_right_rounded),
        ),
      );
}

class _Stat extends StatelessWidget {
  const _Stat({required this.icon, required this.value, required this.label});
  final IconData icon;
  final String value, label;

  @override
  Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 8), child: Column(children: [Icon(icon), const SizedBox(height: 6), Text(value, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), Text(label, style: Theme.of(context).textTheme.labelSmall)]));
}

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key, required this.store, required this.themeKey, required this.onThemeChanged});
  final LocalStore store;
  final String themeKey;
  final Future<void> Function(String) onThemeChanged;

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Profile')),
        body: ListView(padding: const EdgeInsets.all(20), children: [
          const CircleAvatar(radius: 42, child: Icon(Icons.person_rounded, size: 44)),
          const SizedBox(height: 14),
          const Center(child: Text('Study OS learner', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900))),
          const SizedBox(height: 28),
          Text('App theme', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 10),
          ...appThemes.entries.map((entry) {
            final theme = entry.value;
            final selected = entry.key == themeKey;
            return Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(onTap: () => onThemeChanged(entry.key), leading: CircleAvatar(child: Icon(theme.icon)), title: Text(theme.name, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(theme.description), trailing: Icon(selected ? Icons.check_circle_rounded : Icons.chevron_right_rounded)));
          }),
          const SizedBox(height: 18),
          const Card(child: ListTile(leading: Icon(Icons.offline_bolt_rounded), title: Text('Offline-first'), subtitle: Text('Your study data stays on this device.'))),
          const Card(child: ListTile(leading: Icon(Icons.verified_rounded), title: Text('Zero-cost foundation'), subtitle: Text('No backend or paid API required for the core app.'))),
        ],),
      );
}

class Setup extends StatefulWidget {
  const Setup({super.key, required this.store, this.onStartPlan});
  final LocalStore store;
  final Future<void> Function(StudyPlan plan)? onStartPlan;

  @override
  State<Setup> createState() => _SetupState();
}

class _SetupState extends State<Setup> {
  int total = 120;
  final subjects = <String>[];
  final input = TextEditingController();

  void add() {
    final subject = input.text.trim();
    if (subject.isEmpty || subjects.contains(subject)) return;
    setState(() {
      subjects.add(subject);
      input.clear();
    });
  }

  @override
  void dispose() {
    input.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Regular Study')),
        body: ListView(padding: const EdgeInsets.all(20), children: [
          Text('Plan your focus time', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 6),
          const Text('Set your total time first. You can never allocate more than this.'),
          const SizedBox(height: 22),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text('Total study time', style: TextStyle(fontWeight: FontWeight.w800)), Text('$total min', style: const TextStyle(fontWeight: FontWeight.w900))]),
            Slider(value: total.toDouble(), min: 25, max: 480, divisions: 91, label: '$total min', onChanged: (value) => setState(() => total = value.round())),
            Text('${total ~/ 60}h ${total % 60}m available'),
          ]))),
          const SizedBox(height: 20),
          Text('Subjects', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 10),
          Row(children: [Expanded(child: TextField(controller: input, onSubmitted: (_) => add(), decoration: const InputDecoration(hintText: 'e.g. Mathematics'))), const SizedBox(width: 10), FilledButton(onPressed: add, child: const Icon(Icons.add_rounded))]),
          const SizedBox(height: 12),
          if (subjects.isNotEmpty) Wrap(spacing: 8, runSpacing: 8, children: subjects.map((subject) => InputChip(label: Text(subject), onDeleted: () => setState(() => subjects.remove(subject))).toList())),
          if (subjects.isEmpty) const Card(child: Padding(padding: EdgeInsets.all(16), child: Text('Add at least one subject to continue.'))),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: subjects.isEmpty
                  ? null
                  : () => Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => RegularStudyPlanner(store: widget.store, total: total, subjects: List<String>.from(subjects), onStartPlan: widget.onStartPlan)),
                      ),
              icon: const Icon(Icons.arrow_forward_rounded),
              label: const Text('Continue to topic allocation'),
            ),
          ),
        ]),
      );
}
