import 'dart:async';
import 'package:flutter/foundation.dart';
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
import 'screens/today_engine_screen.dart';
import 'widgets/update_gate.dart';
import 'widgets/attractive_home.dart';
import 'widgets/study_os_bootstrap.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    StudyOSBootstrap(
      builder: (prefs) => StudyOS(store: LocalStore(prefs), prefs: prefs),
    ),
  );
}

const themes = <String, _AppTheme>{
  'midnight': _AppTheme('Midnight', Icons.nights_stay_rounded, Color(0xFF6C63FF), Brightness.dark),
  'ocean': _AppTheme('Ocean Dark', Icons.water_rounded, Color(0xFF1479A8), Brightness.dark),
  'forest': _AppTheme('Forest', Icons.forest_rounded, Color(0xFF3F7D58), Brightness.dark),
  'sunrise': _AppTheme('Sunrise', Icons.wb_sunny_rounded, Color(0xFFE4774E), Brightness.light),
  'ocean_light': _AppTheme('Ocean', Icons.water_drop_rounded, Color(0xFF168AAD), Brightness.light),
  'mint': _AppTheme('Mint', Icons.spa_rounded, Color(0xFF2A9D8F), Brightness.light),
  'rose': _AppTheme('Rose', Icons.local_florist_rounded, Color(0xFFC85572), Brightness.light),
  'peach': _AppTheme('Peach', Icons.wb_sunny_outlined, Color(0xFFE07A5F), Brightness.light),
  'lavender': _AppTheme('Lavender', Icons.auto_awesome_rounded, Color(0xFF7B61A8), Brightness.light),
  'sky': _AppTheme('Sky', Icons.cloud_rounded, Color(0xFF3D7EA6), Brightness.light),
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

class _StudyOSState extends State<StudyOS> with WidgetsBindingObserver {
  final navigatorKey = GlobalKey<NavigatorState>();
  late final ReminderCoordinator reminderCoordinator;
  late String themeKey = themes.containsKey(widget.store.themePreset) ? widget.store.themePreset : 'midnight';
  late AppLanguage language = widget.store.appLanguage;
  int tab = 0;
  late final List<Widget?> _tabs = List<Widget?>.filled(4, null);
  AppStrings get strings => AppStrings(language);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    reminderCoordinator = ReminderCoordinator(store: widget.store, settingsStore: ReminderSettingsStore(widget.prefs), scheduler: NotificationService());
    _tabs[0] = _buildHomeTab();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (kReleaseMode) {
        unawaited(Future<void>.delayed(const Duration(milliseconds: 800), _syncRemindersSafely));
      } else {
        unawaited(_syncRemindersSafely());
      }
    });
  }

  Future<void> _syncRemindersSafely() async {
    try {
      await reminderCoordinator.sync();
    } catch (_) {
      // Reminders are non-critical and must never terminate the app at startup.
    }
  }

  Widget _buildHomeTab() => Home(store: widget.store, onOpenFocus: openFocus, onRegularStudy: openRegularStudy, onExam: _openExam, language: language);
  Widget _buildStudyTab() => StudyHub(store: widget.store, onStartPlan: (plan) => openFocus(plan: plan), onOpenFocus: openFocus, onRegularStudy: openRegularStudy, language: language);
  Widget _buildProgressTab() => ProgressDashboard(store: widget.store);
  Widget _buildProfileTab() => ProfileScreen(store: widget.store, prefs: widget.prefs, themeKey: themeKey, onThemeChanged: setTheme, language: language, onLanguageChanged: setLanguage);

  void _ensureTab(int index) {
    if (_tabs[index] != null) return;
    _tabs[index] = switch (index) {
      0 => _buildHomeTab(),
      1 => _buildStudyTab(),
      2 => _buildProgressTab(),
      3 => _buildProfileTab(),
      _ => const SizedBox.shrink(),
    };
  }

  void _selectTab(int value) {
    _ensureTab(value);
    if (tab != value) setState(() => tab = value);
  }

  void _refreshDependentTabs() {
    _tabs[0] = _buildHomeTab();
    _tabs[1] = _buildStudyTab();
    _tabs[3] = _buildProfileTab();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(Future<void>.delayed(const Duration(milliseconds: 500), _syncRemindersSafely));
    }
  }

  Future<void> setTheme(String key) async {
    final theme = themes[key];
    if (theme == null) return;
    setState(() {
      themeKey = key;
      _tabs[3] = _buildProfileTab();
    });
    await widget.store.setThemePreset(key);
    await widget.store.setDarkMode(theme.brightness == Brightness.dark);
  }

  Future<void> setLanguage(AppLanguage value) async {
    await widget.store.setAppLanguage(value);
    if (!mounted) return;
    setState(() {
      language = value;
      _refreshDependentTabs();
    });
  }

  Future<void> openFocus({StudyPlan? plan}) async {
    final snapshot = TodayEngine(widget.store).build();
    final activePlan = plan ?? snapshot.plan;
    if (activePlan == null || activePlan.items.isEmpty) return;
    if (plan == null && snapshot.isComplete) return;
    final storedPlan = widget.store.loadPlan();
    final sameAsStored = storedPlan != null && StudySessionStore(widget.store).samePlan(storedPlan, activePlan);
    final index = sameAsStored ? snapshot.currentIndex : 0;
    final blockIndex = sameAsStored ? snapshot.currentBlockIndex : 0;
    await navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: activePlan, index: index, blockIndex: blockIndex, onFocusBlockCompleted: reminderCoordinator.notifyFocusBlockCompleted)));
    if (mounted) {
      _tabs[0] = _buildHomeTab();
      _tabs[1] = _buildStudyTab();
      _tabs[2] = _buildProgressTab();
      setState(() {});
    }
  }

  void openRegularStudy() {
    navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => Setup(store: widget.store, onStartPlan: (plan) async {
      if (!mounted) return;
      navigatorKey.currentState?.pop();
      await openFocus(plan: plan);
    })));
  }

  @override
  Widget build(BuildContext context) {
    final theme = themes[themeKey]!;
    final scheme = ColorScheme.fromSeed(seedColor: theme.seed, brightness: theme.brightness);
    _ensureTab(tab);
    return MaterialApp(
      navigatorKey: navigatorKey,
      debugShowCheckedModeBanner: false,
      title: 'Study OS',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        brightness: theme.brightness,
        scaffoldBackgroundColor: theme.brightness == Brightness.dark ? const Color(0xFF0B0D13) : null,
        appBarTheme: AppBarTheme(
          centerTitle: false,
          elevation: 0,
          scrolledUnderElevation: 0,
          titleTextStyle: TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: scheme.onSurface),
        ),
        cardTheme: CardThemeData(
          margin: EdgeInsets.zero,
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        ),
        listTileTheme: ListTileThemeData(
          enableFeedback: true,
          minVerticalPadding: 8,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            minimumSize: const Size(0, 50),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            textStyle: const TextStyle(fontWeight: FontWeight.w800),
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(0, 50),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            textStyle: const TextStyle(fontWeight: FontWeight.w800),
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            minimumSize: const Size(0, 46),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            textStyle: const TextStyle(fontWeight: FontWeight.w800),
          ),
        ),
        chipTheme: ChipThemeData(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          labelStyle: const TextStyle(fontWeight: FontWeight.w700),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
        ),
        inputDecorationTheme: InputDecorationTheme(
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
          enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
          focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: scheme.primary, width: 2)),
        ),
        iconButtonTheme: IconButtonThemeData(
          style: ButtonStyle(
            overlayColor: WidgetStateProperty.resolveWith((states) {
              if (states.contains(WidgetState.pressed)) {
                return scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.30 : 0.18);
              }
              if (states.contains(WidgetState.hovered)) {
                return scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.10 : 0.06);
              }
              return null;
            }),
          ),
        ),
        navigationBarTheme: NavigationBarThemeData(
          overlayColor: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.pressed)) {
              return scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.26 : 0.16);
            }
            if (states.contains(WidgetState.hovered)) {
              return scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.10 : 0.06);
            }
            return null;
          }),
        ),
        splashFactory: InkRipple.splashFactory,
        splashColor: scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.30 : 0.18),
        highlightColor: scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.12 : 0.08),
        hoverColor: scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.10 : 0.06),
        focusColor: scheme.primary.withValues(alpha: theme.brightness == Brightness.dark ? 0.12 : 0.08),
      ),
      home: UpdateGate(
        store: widget.store,
        checkForUpdate: widget.checkForUpdate,
        child: Scaffold(
          body: IndexedStack(index: tab, children: [
            _tabs[0] ?? const SizedBox.shrink(),
            _tabs[1] ?? const SizedBox.shrink(),
            _tabs[2] ?? const SizedBox.shrink(),
            _tabs[3] ?? const SizedBox.shrink(),
          ]),
          bottomNavigationBar: NavigationBar(selectedIndex: tab, onDestinationSelected: _selectTab, destinations: [
            NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home_rounded), label: strings.isBangla ? 'হোম' : 'Home'),
            NavigationDestination(icon: const Icon(Icons.menu_book_outlined), selectedIcon: const Icon(Icons.menu_book_rounded), label: strings.isBangla ? 'স্টাডি' : 'Study'),
            NavigationDestination(icon: const Icon(Icons.insights_outlined), selectedIcon: const Icon(Icons.insights_rounded), label: strings.isBangla ? 'অগ্রগতি' : 'Progress'),
            NavigationDestination(icon: const Icon(Icons.person_outline_rounded), selectedIcon: const Icon(Icons.person_rounded), label: strings.profile),
          ]),
        ),
      ),
    );
  }

  void _openExam(bool nextDay) {
    navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => ExamPlannerScreen(nextDay: nextDay, store: widget.store, onStartPlan: (plan) async {
      if (!mounted) return;
      navigatorKey.currentState?.pop();
      await openFocus(plan: plan);
    })));
  }
}

class Home extends StatelessWidget {
  const Home({super.key, required this.store, required this.onOpenFocus, required this.onRegularStudy, required this.onExam, required this.language});
  final LocalStore store;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final void Function(bool) onExam;
  final AppLanguage language;
  @override Widget build(BuildContext context) => AttractiveHome(store: store, language: language, onOpenFocus: onOpenFocus, onRegularStudy: onRegularStudy, onExam: onExam);
}

class StudyHub extends StatelessWidget {
  const StudyHub({super.key, required this.store, required this.onStartPlan, required this.onOpenFocus, required this.onRegularStudy, required this.language});
  final LocalStore store;
  final Future<void> Function(StudyPlan plan) onStartPlan;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final s = AppStrings(language);
    final theme = Theme.of(context);
    final savedCount = StudySessionStore(store).sessions.length;

    return Scaffold(
      appBar: AppBar(
        title: Text(s.isBangla ? 'স্টাডি' : 'Study', style: const TextStyle(fontWeight: FontWeight.w900)),
        centerTitle: false,
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final maxWidth = constraints.maxWidth > 700 ? 680.0 : double.infinity;
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
            children: [
              Center(
                child: ConstrainedBox(
                  constraints: BoxConstraints(maxWidth: maxWidth),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        s.isBangla ? 'আজকের পড়াশোনা' : 'Your study space',
                        style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        s.isBangla ? 'একটি কাজ বেছে নিয়ে সরাসরি শুরু করুন।' : 'Pick a path and get straight into studying.',
                        style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                      const SizedBox(height: 18),
                      _StudyHero(
                        icon: Icons.auto_awesome_rounded,
                        title: s.isBangla ? 'আজকের পরিকল্পনা' : 'Today Engine',
                        subtitle: s.isBangla
                            ? 'আজ কী পড়বেন এবং পরের ফোকাস কী—এক নজরে দেখুন।'
                            : 'See what is left today and jump to your next focus.',
                        actionLabel: s.isBangla ? 'দেখুন' : 'Open',
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => TodayEngineScreen(store: store, onOpenFocus: onOpenFocus),
                          ),
                        ),
                      ),
                      if (savedCount > 0) ...[
                        const SizedBox(height: 12),
                        Card(
                          child: ListTile(
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                            leading: CircleAvatar(
                              backgroundColor: theme.colorScheme.secondaryContainer,
                              foregroundColor: theme.colorScheme.onSecondaryContainer,
                              child: const Icon(Icons.bookmark_rounded),
                            ),
                            title: Text(
                              s.isBangla ? 'সেভ করা সেশন' : 'Saved sessions',
                              style: const TextStyle(fontWeight: FontWeight.w900),
                            ),
                            subtitle: Text(
                              s.isBangla
                                  ? '$savedCountটি অসম্পূর্ণ সেশন অপেক্ষা করছে'
                                  : '$savedCount unfinished session${savedCount == 1 ? '' : 's'} waiting',
                            ),
                            trailing: const Icon(Icons.chevron_right_rounded),
                            onTap: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => SavedSessionsScreen(
                                  store: store,
                                  onOpenFocus: () async {
                                    final restoredPlan = store.loadPlan();
                                    if (restoredPlan == null) return;
                                    await onStartPlan(restoredPlan);
                                  },
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                      const SizedBox(height: 22),
                      Text(
                        s.isBangla ? 'স্টাডি মোড' : 'Study modes',
                        style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
                      ),
                      const SizedBox(height: 10),
                      _Mode(
                        icon: Icons.menu_book_rounded,
                        title: s.isBangla ? 'রেগুলার স্টাডি' : 'Regular Study',
                        subtitle: s.isBangla ? 'বিষয়, অধ্যায় ও ফোকাস ব্লক পরিকল্পনা করুন।' : 'Plan subjects, chapters and focus blocks.',
                        onTap: onRegularStudy,
                      ),
                      _Mode(
                        icon: Icons.auto_awesome_rounded,
                        title: s.isBangla ? 'পরীক্ষার প্রস্তুতি' : 'Exam Preparation',
                        subtitle: s.isBangla ? 'অগ্রাধিকারভিত্তিক পরীক্ষার পরিকল্পনা।' : 'Build a focused exam plan.',
                        onTap: () => _openExam(context, false),
                      ),
                      _Mode(
                        icon: Icons.bolt_rounded,
                        title: s.isBangla ? 'আগামীকালের পরীক্ষা' : 'Next Day Exam',
                        subtitle: s.isBangla ? 'গুরুত্বপূর্ণ রিভিশনকে আগে আনুন।' : 'Bring high-priority revision forward.',
                        onTap: () => _openExam(context, true),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
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

class _StudyHero extends StatelessWidget {
  const _StudyHero({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.actionLabel,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String actionLabel;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      color: scheme.primaryContainer,
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: scheme.surface,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: scheme.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: TextStyle(fontWeight: FontWeight.w900, fontSize: 17, color: scheme.onPrimaryContainer)),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(color: scheme.onPrimaryContainer.withValues(alpha: 0.78)),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              FilledButton.tonal(
                onPressed: onTap,
                child: Text(actionLabel),
              ),
            ],
          ),
        ),
      ),
    );
  }
}


class _Mode extends StatelessWidget {
  const _Mode({required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 14, 12, 14),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: scheme.secondaryContainer,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: scheme.onSecondaryContainer),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
                    const SizedBox(height: 3),
                    Text(
                      subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(color: scheme.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(Icons.arrow_forward_rounded, color: scheme.primary),
            ],
          ),
        ),
      ),
    );
  }
}
