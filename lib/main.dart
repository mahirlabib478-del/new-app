import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/study_models.dart';
import 'services/ambient_audio_service.dart';
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
  'midnight': _AppTheme('Midnight', Icons.nights_stay_rounded, Color(0xFF6366F1), Brightness.dark, scaffoldBackground: Color(0xFF0C0E17)),
  'pitch_black': _AppTheme('Pitch Black', Icons.dark_mode_rounded, Color(0xFF00E5FF), Brightness.dark, scaffoldBackground: Color(0xFF000000)),
  'espresso': _AppTheme('Espresso', Icons.coffee_rounded, Color(0xFFD4A373), Brightness.dark, scaffoldBackground: Color(0xFF14100D)),
  'ocean': _AppTheme('Ocean Dark', Icons.water_rounded, Color(0xFF0284C7), Brightness.dark, scaffoldBackground: Color(0xFF08121E)),
  'forest': _AppTheme('Forest', Icons.forest_rounded, Color(0xFF10B981), Brightness.dark, scaffoldBackground: Color(0xFF07140B)),
  'paper': _AppTheme('Paper Sepia', Icons.menu_book_rounded, Color(0xFF8B5A2B), Brightness.light, scaffoldBackground: Color(0xFFF7F4EB)),
  'mint': _AppTheme('Mint', Icons.spa_rounded, Color(0xFF0D9488), Brightness.light, scaffoldBackground: Color(0xFFF0FDF4)),
  'matcha': _AppTheme('Matcha', Icons.eco_rounded, Color(0xFF4D7C0F), Brightness.light, scaffoldBackground: Color(0xFFF4F8EE)),
  'sunrise': _AppTheme('Sunrise', Icons.wb_sunny_rounded, Color(0xFFEA580C), Brightness.light, scaffoldBackground: Color(0xFFFFF7ED)),
  'slate': _AppTheme('Nordic Slate', Icons.filter_drama_rounded, Color(0xFF475569), Brightness.light, scaffoldBackground: Color(0xFFF1F5F9)),
};

class _AppTheme {
  const _AppTheme(this.name, this.icon, this.seed, this.brightness, {this.scaffoldBackground});
  final String name;
  final IconData icon;
  final Color seed;
  final Brightness brightness;
  final Color? scaffoldBackground;
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
  late double textScale = widget.store.textScale;
  int tab = 0;
  late final List<Widget?> _tabs = List<Widget?>.filled(4, null);
  AppStrings get strings => AppStrings(language);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    reminderCoordinator = ReminderCoordinator(store: widget.store, settingsStore: ReminderSettingsStore(widget.prefs), scheduler: NotificationService());
    _tabs[0] = _buildHomeTab();
    unawaited(AmbientAudioService.instance.init(widget.prefs));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_prepareRemindersSafely());
    });
  }

  Future<void> _prepareRemindersSafely() async {
    try {
      final settings = ReminderSettingsStore(widget.prefs).settings;
      final anyEnabled = settings.studyEnabled || settings.breakEnabled || settings.planEnabled;
      if (!anyEnabled) return;

      // Explain why the system permission is needed before opening the
      // Android dialog, so the first prompt has clear context.
      if (!await reminderCoordinator.areNotificationsEnabled()) {
        final allow = await showDialog<bool>(
          context: navigatorKey.currentState!.overlay!.context,
          builder: (context) => AlertDialog(
            title: Text(strings.notificationPermissionTitle),
            content: Text(strings.notificationPermissionBody),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: Text(strings.notificationLater),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: Text(strings.allowNotifications),
              ),
            ],
          ),
        );
        if (allow != true) return;
      }
      final granted = await reminderCoordinator.requestPermissions();
      if (!granted) return;

      await reminderCoordinator.sync(settingsOverride: settings);
    } on Exception {
      // Reminders are non-critical and must never terminate the app at startup.
    }
  }

  Widget _buildHomeTab() => Home(store: widget.store, onOpenFocus: openFocus, onRegularStudy: openRegularStudy, onExam: _openExam, language: language);
  Widget _buildStudyTab() => StudyHub(store: widget.store, onStartPlan: _startPlanAndSyncReminders, onOpenFocus: openFocus, onRegularStudy: openRegularStudy, language: language, onPlanSaved: (_) => _syncRemindersSafely());
  Widget _buildProgressTab() => ProgressDashboard(store: widget.store);
  Widget _buildProfileTab() => ProfileScreen(
        store: widget.store,
        prefs: widget.prefs,
        themeKey: themeKey,
        onThemeChanged: setTheme,
        language: language,
        onLanguageChanged: setLanguage,
        textScale: textScale,
        onTextScaleChanged: setTextScale,
        reminderCoordinator: reminderCoordinator,
        onDataChanged: _refreshAllTabs,
      );

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

  void _refreshAllTabs() {
    _refreshDependentTabs();
    _tabs[2] = _buildProgressTab();
    if (mounted) setState(() {});
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
      _refreshAllTabs();
      unawaited(Future<void>.delayed(const Duration(milliseconds: 500), _syncRemindersSafely));
    }
  }

  Future<void> _syncRemindersSafely() async {
    try {
      await reminderCoordinator.sync();
    } catch (_) {
      // Reminders are non-critical and must never terminate the app on resume.
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

  Future<void> setTextScale(double value) async {
    await widget.store.setTextScale(value);
    if (!mounted) return;
    setState(() {
      textScale = value;
      _tabs[3] = _buildProfileTab();
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
    await navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: activePlan, index: index, blockIndex: blockIndex, onFocusBlockCompleted: reminderCoordinator.notifyFocusBlockCompleted,
      onFocusBlockScheduled: reminderCoordinator.scheduleFocusBlockCompletion,
      onFocusBlockScheduleCancelled: reminderCoordinator.cancelFocusBlockCompletion)));
    if (mounted) {
      _tabs[0] = _buildHomeTab();
      _tabs[1] = _buildStudyTab();
      _tabs[2] = _buildProgressTab();
      setState(() {});
    }
  }

  Future<void> _startPlanAndSyncReminders(StudyPlan plan) async {
    // A newly-created plan changes both Study Reminder and Plan Reminder
    // eligibility. Sync immediately instead of waiting for an app resume.
    await _syncRemindersSafely();
    if (!mounted) return;
    await openFocus(plan: plan);
  }

  void openRegularStudy() {
    navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => Setup(store: widget.store, onStartPlan: (plan) async {
      if (!mounted) return;
      navigatorKey.currentState?.pop();
      await _startPlanAndSyncReminders(plan);
    })));
  }

  @override
  Widget build(BuildContext context) {
    final theme = themes[themeKey]!;
    final isDark = theme.brightness == Brightness.dark;
    final scheme = ColorScheme.fromSeed(seedColor: theme.seed, brightness: theme.brightness);
    _ensureTab(tab);
    return MaterialApp(
      navigatorKey: navigatorKey,
      debugShowCheckedModeBanner: false,
      title: 'Study OS',
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: TextScaler.linear(textScale),
          ),
          child: child ?? const SizedBox.shrink(),
        );
      },
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        brightness: theme.brightness,
        scaffoldBackgroundColor: theme.scaffoldBackground ?? (isDark ? const Color(0xFF0C0E14) : null),
        appBarTheme: AppBarTheme(
          centerTitle: false,
          elevation: 0,
          scrolledUnderElevation: 0,
          titleTextStyle: TextStyle(
            fontSize: 21,
            fontWeight: FontWeight.w800,
            letterSpacing: -0.2,
            color: scheme.onSurface,
          ),
        ),
        cardTheme: CardThemeData(
          margin: EdgeInsets.zero,
          elevation: isDark ? 0 : 0.8,
          color: isDark ? scheme.surfaceContainerLow : scheme.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
            side: BorderSide(
              color: isDark
                  ? scheme.outlineVariant.withValues(alpha: 0.38)
                  : scheme.outlineVariant.withValues(alpha: 0.30),
              width: 1.0,
            ),
          ),
        ),
        listTileTheme: ListTileThemeData(
          enableFeedback: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          minVerticalPadding: 6,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            minimumSize: const Size(0, 52),
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15, letterSpacing: 0.2),
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(0, 52),
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            side: BorderSide(color: scheme.outlineVariant.withValues(alpha: 0.7), width: 1.2),
            textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15, letterSpacing: 0.2),
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            minimumSize: const Size(0, 46),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14),
          ),
        ),
        textTheme: TextTheme(
          titleLarge: TextStyle(fontWeight: FontWeight.w800, fontSize: 20, color: scheme.onSurface),
          titleMedium: TextStyle(fontWeight: FontWeight.w700, fontSize: 16, color: scheme.onSurface),
          titleSmall: TextStyle(fontWeight: FontWeight.w600, fontSize: 14, color: scheme.onSurface),
          bodyLarge: TextStyle(fontSize: 15.5, color: scheme.onSurface, height: 1.4),
          bodyMedium: TextStyle(fontSize: 14, color: scheme.onSurface, height: 1.35),
          bodySmall: TextStyle(fontSize: 13, color: scheme.onSurfaceVariant, height: 1.3),
          labelLarge: TextStyle(fontWeight: FontWeight.w700, fontSize: 14, color: scheme.onSurface),
        ),
        chipTheme: ChipThemeData(
          backgroundColor: scheme.surfaceContainerLow,
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
      await _startPlanAndSyncReminders(plan);
    }, onPlanSaved: (_) => _syncRemindersSafely())));
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
  const StudyHub({super.key, required this.store, required this.onStartPlan, required this.onOpenFocus, required this.onRegularStudy, required this.language, required this.onPlanSaved});
  final LocalStore store;
  final Future<void> Function(StudyPlan plan) onStartPlan;
  final Future<void> Function({StudyPlan? plan}) onOpenFocus;
  final VoidCallback onRegularStudy;
  final AppLanguage language;
  final Future<void> Function(StudyPlan plan) onPlanSaved;

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
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
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
                      const SizedBox(height: 10),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: theme.colorScheme.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(18),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.today_rounded, size: 20, color: theme.colorScheme.primary),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                s.isBangla ? 'আজকের ফোকাস' : 'Today’s focus',
                                style: theme.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w800),
                              ),
                            ),
                            Text(
                              s.isBangla ? 'সরাসরি শুরু করুন' : 'Start focused',
                              style: theme.textTheme.labelMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 10),
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
                        const SizedBox(height: 10),
                        Card(
                          color: theme.colorScheme.surfaceContainerLow,
                          child: InkWell(
                            borderRadius: BorderRadius.circular(22),
                            splashColor: theme.colorScheme.primary.withValues(alpha: 0.18),
                            highlightColor: theme.colorScheme.primary.withValues(alpha: 0.08),
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
                            child: Padding(
                              padding: const EdgeInsets.fromLTRB(14, 11, 12, 11),
                              child: Row(
                                children: [
                                  Container(
                                    width: 48,
                                    height: 48,
                                    decoration: BoxDecoration(
                                      color: theme.colorScheme.secondaryContainer,
                                      borderRadius: BorderRadius.circular(16),
                                    ),
                                    child: Icon(
                                      Icons.bookmark_rounded,
                                      color: theme.colorScheme.onSecondaryContainer,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          s.isBangla ? 'সেভ করা সেশন' : 'Saved sessions',
                                          style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16),
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          s.isBangla
                                              ? '$savedCountটি অসম্পূর্ণ সেশন অপেক্ষা করছে'
                                              : '$savedCount unfinished session${savedCount == 1 ? '' : 's'} waiting',
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(color: theme.colorScheme.onSurfaceVariant),
                                        ),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Icon(Icons.arrow_forward_rounded, color: theme.colorScheme.primary),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                      const SizedBox(height: 18),
                      Row(
                        children: [
                          Icon(Icons.tune_rounded, size: 22, color: theme.colorScheme.primary),
                          const SizedBox(width: 8),
                          Text(
                            s.isBangla ? 'স্টাডি মোড' : 'Study modes',
                            style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        s.isBangla ? 'আপনার পড়ার লক্ষ্য অনুযায়ী একটি মোড বেছে নিন।' : 'Choose a mode that matches what you want to study.',
                        style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
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
    }, onPlanSaved: onPlanSaved)));
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
          padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
          child: LayoutBuilder(
            builder: (context, constraints) {
              final compact = constraints.maxWidth < 520;
              final text = Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: TextStyle(fontWeight: FontWeight.w900, fontSize: 17, color: scheme.onPrimaryContainer)),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(color: scheme.onPrimaryContainer.withValues(alpha: 0.78)),
                    ),
                  ],
                ),
              );
              final iconBox = Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: scheme.surface,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: scheme.primary),
              );
              final button = FilledButton(
                onPressed: onTap,
                style: FilledButton.styleFrom(
                  minimumSize: const Size(0, 50),
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                ),
                child: Text(actionLabel),
              );

              if (compact) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(children: [iconBox, const SizedBox(width: 8), text]),
                    const SizedBox(height: 10),
                    button,
                  ],
                );
              }

              return Row(
                children: [
                  iconBox,
                  const SizedBox(width: 8),
                  text,
                  const SizedBox(width: 6),
                  Flexible(child: button),
                ],
              );
            },
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
      color: scheme.surfaceContainerLow,
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        splashColor: scheme.primary.withValues(alpha: 0.18),
        highlightColor: scheme.primary.withValues(alpha: 0.08),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
          child: Row(
            children: [
              Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: scheme.secondaryContainer,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: scheme.onSecondaryContainer),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(color: scheme.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: scheme.primary.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.arrow_forward_rounded, size: 20, color: scheme.primary),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
