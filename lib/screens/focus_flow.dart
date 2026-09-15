import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';

class FocusScreen extends StatefulWidget {
  const FocusScreen({super.key, required this.store, required this.plan, required this.index, required this.blockIndex});
  final LocalStore store;
  final StudyPlan plan;
  final int index;
  final int blockIndex;
  static const focusBlock = 25;

  @override
  State<FocusScreen> createState() => _FocusScreenState();
}

class _FocusScreenState extends State<FocusScreen> with WidgetsBindingObserver {
  late int activeIndex;
  late int activeBlockIndex;
  late int seconds;
  late int currentBlockMinutes;
  bool running = true;
  bool transitioning = false;
  Timer? timer;

  StudyItem get item => widget.plan.items[activeIndex];
  int get completedForItem => widget.store.itemCompletedMinutes(activeIndex).clamp(0, item.minutes).toInt();
  int get totalBlocks => math.max(1, (item.minutes / FocusScreen.focusBlock).ceil());

  int _firstRemainingIndex() {
    for (var i = 0; i < widget.plan.items.length; i++) {
      final minutes = widget.store.itemCompletedMinutes(i).clamp(0, widget.plan.items[i].minutes).toInt();
      if (minutes < widget.plan.items[i].minutes) return i;
    }
    return -1;
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);

    if (widget.plan.items.isEmpty) {
      activeIndex = 0;
      activeBlockIndex = 0;
      currentBlockMinutes = 1;
      seconds = 60;
      running = false;
      WidgetsBinding.instance.addPostFrameCallback((_) => _showCompletion());
      return;
    }

    final savedIndex = widget.store.currentPlanIndex;
    final savedIsRemaining = savedIndex >= 0 && savedIndex < widget.plan.items.length &&
        widget.store.itemCompletedMinutes(savedIndex).clamp(0, widget.plan.items[savedIndex].minutes).toInt() < widget.plan.items[savedIndex].minutes;
    final requestedIsRemaining = widget.index >= 0 && widget.index < widget.plan.items.length &&
        widget.store.itemCompletedMinutes(widget.index).clamp(0, widget.plan.items[widget.index].minutes).toInt() < widget.plan.items[widget.index].minutes;

    if (savedIsRemaining) {
      activeIndex = savedIndex;
    } else if (requestedIsRemaining) {
      activeIndex = widget.index;
    } else {
      activeIndex = _firstRemainingIndex();
    }

    if (activeIndex < 0) {
      activeIndex = 0;
      activeBlockIndex = 0;
      currentBlockMinutes = 1;
      seconds = 60;
      running = false;
      WidgetsBinding.instance.addPostFrameCallback((_) => _showCompletion());
      return;
    }

    final completed = completedForItem;
    activeBlockIndex = (completed ~/ FocusScreen.focusBlock).clamp(0, totalBlocks - 1).toInt();
    final remaining = (item.minutes - completed).clamp(0, item.minutes).toInt();
    currentBlockMinutes = remaining > FocusScreen.focusBlock ? FocusScreen.focusBlock : remaining;
    if (currentBlockMinutes <= 0) currentBlockMinutes = 1;
    seconds = currentBlockMinutes * 60;

    var expiredOnResume = false;
    final saved = widget.store.focusTimerState;
    if (saved != null && saved.index == activeIndex && saved.blockIndex == activeBlockIndex) {
      if (saved.running && saved.deadlineMillis != null) {
        final remainingSeconds = ((saved.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil();
        seconds = remainingSeconds.clamp(0, currentBlockMinutes * 60).toInt();
        running = true;
        expiredOnResume = remainingSeconds <= 0;
      } else {
        seconds = saved.remainingSeconds.clamp(0, currentBlockMinutes * 60).toInt();
        running = false;
      }
    }

    unawaited(widget.store.setPlanPosition(activeIndex, activeBlockIndex));
    if (expiredOnResume) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _openBreak(currentBlockMinutes));
    } else {
      _persistTimerState();
      if (running) _startTimer();
    }
  }

  void _startTimer() {
    timer?.cancel();
    timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || !running || transitioning) return;
      if (seconds > 0) setState(() => seconds--);
      if (seconds == 0) {
        timer?.cancel();
        unawaited(widget.store.clearFocusTimerState());
        _openBreak(currentBlockMinutes);
      }
    });
  }

  void _persistTimerState() {
    if (transitioning) return;
    final deadline = running ? DateTime.now().add(Duration(seconds: seconds)).millisecondsSinceEpoch : null;
    unawaited(widget.store.saveFocusTimerState(FocusTimerState(index: activeIndex, blockIndex: activeBlockIndex, remainingSeconds: seconds, running: running, deadlineMillis: deadline)));
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) {
      _persistTimerState();
    } else if (state == AppLifecycleState.resumed && mounted && running && !transitioning) {
      final saved = widget.store.focusTimerState;
      if (saved != null && saved.index == activeIndex && saved.blockIndex == activeBlockIndex && saved.deadlineMillis != null) {
        final nextSeconds = ((saved.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil();
        if (nextSeconds <= 0) {
          seconds = 0;
          timer?.cancel();
          _openBreak(currentBlockMinutes);
          return;
        }
        setState(() => seconds = nextSeconds.clamp(0, currentBlockMinutes * 60).toInt());
      }
      _startTimer();
    }
  }

  void _toggleRunning() {
    if (transitioning || seconds <= 0) return;
    setState(() => running = !running);
    _persistTimerState();
    if (running) _startTimer(); else timer?.cancel();
  }

  void _showCompletion() {
    if (!mounted || transitioning) return;
    transitioning = true;
    timer?.cancel();
    unawaited(widget.store.clearFocusTimerState());
    unawaited(widget.store.clearPlanPosition());
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => CompletionScreen(plan: widget.plan, store: widget.store)));
  }

  void _openBreak(int completed) {
    if (transitioning || !mounted) return;
    transitioning = true;
    timer?.cancel();
    final safeCompleted = completed.clamp(0, currentBlockMinutes).toInt();
    unawaited(widget.store.clearFocusTimerState());
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => BreakScreen(store: widget.store, plan: widget.plan, index: activeIndex, blockIndex: activeBlockIndex, completed: safeCompleted)));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final clock = '${(seconds ~/ 60).toString().padLeft(2, '0')}:${(seconds % 60).toString().padLeft(2, '0')}';
    final progress = (1 - seconds / (currentBlockMinutes * 60)).clamp(0.0, 1.0).toDouble();
    final itemProgress = item.minutes <= 0 ? 0.0 : (completedForItem / item.minutes).clamp(0.0, 1.0).toDouble();
    final elapsedMinutes = ((currentBlockMinutes * 60 - seconds) / 60).floor();
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Focus mode'), centerTitle: true),
      body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.fromLTRB(24, 20, 24, 30), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 560), child: Column(children: [
        Text('DEEP FOCUS', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.5, color: scheme.primary)),
        const SizedBox(height: 18),
        Text(item.title, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
        if (item.topic.isNotEmpty) ...[const SizedBox(height: 6), Text(item.topic, textAlign: TextAlign.center)],
        const SizedBox(height: 18),
        Row(children: [const Expanded(child: Text('Topic progress', style: TextStyle(fontWeight: FontWeight.w800))), Text('${completedForItem}/${item.minutes} min', style: const TextStyle(fontWeight: FontWeight.w800))]),
        const SizedBox(height: 8), LinearProgressIndicator(value: itemProgress, minHeight: 7),
        const SizedBox(height: 26),
        Container(width: 280, height: 280, decoration: BoxDecoration(shape: BoxShape.circle, color: scheme.surfaceContainerHighest), child: Stack(alignment: Alignment.center, children: [
          SizedBox(width: 258, height: 258, child: CircularProgressIndicator(value: progress, strokeWidth: 10, strokeCap: StrokeCap.round)),
          Column(mainAxisAlignment: MainAxisAlignment.center, children: [Text(clock, style: Theme.of(context).textTheme.displayLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 3), Text(running ? 'Stay with one task' : 'Timer paused')]),
        ])),
        const SizedBox(height: 24),
        Text('Block ${activeBlockIndex + 1} of $totalBlocks • $currentBlockMinutes min focus', style: const TextStyle(fontWeight: FontWeight.w800)),
        const SizedBox(height: 24),
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [FilledButton.icon(onPressed: transitioning ? null : _toggleRunning, icon: Icon(running ? Icons.pause_rounded : Icons.play_arrow_rounded), label: Text(running ? 'Pause' : 'Resume')), const SizedBox(width: 12), OutlinedButton.icon(onPressed: transitioning || elapsedMinutes < 1 ? null : () => _openBreak(elapsedMinutes), icon: const Icon(Icons.done_rounded), label: const Text('Finish early'))]),
        const SizedBox(height: 22),
        Card(child: Padding(padding: const EdgeInsets.all(16), child: Text('Your timer is saved locally. If the app closes, you can return and continue from this block.', style: Theme.of(context).textTheme.bodyMedium))),
      ]))))),
    );
  }
}

class BreakScreen extends StatefulWidget {
  const BreakScreen({super.key, required this.store, required this.plan, required this.index, required this.blockIndex, required this.completed});
  final LocalStore store;
  final StudyPlan plan;
  final int index;
  final int blockIndex;
  final int completed;

  @override
  State<BreakScreen> createState() => _BreakScreenState();
}

class _BreakScreenState extends State<BreakScreen> with WidgetsBindingObserver {
  int breakMinutes = 5;
  int seconds = 300;
  bool running = true;
  bool advancing = false;
  Timer? timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    var expiredOnResume = false;
    final saved = widget.store.breakTimerState;
    if (saved != null && saved.index == widget.index && saved.blockIndex == widget.blockIndex) {
      breakMinutes = saved.breakMinutes == 10 ? 10 : 5;
      if (saved.running && saved.deadlineMillis != null) {
        final remainingSeconds = ((saved.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil();
        seconds = remainingSeconds.clamp(0, breakMinutes * 60).toInt();
        running = true;
        expiredOnResume = remainingSeconds <= 0;
      } else {
        seconds = saved.remainingSeconds.clamp(0, breakMinutes * 60).toInt();
        running = false;
      }
    }
    if (expiredOnResume) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _next());
    } else {
      _persistBreakState();
      if (running) _startTimer();
    }
  }

  void _startTimer() {
    timer?.cancel();
    timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || !running || advancing) return;
      if (seconds > 0) setState(() => seconds--);
      if (seconds == 0) {
        timer?.cancel();
        unawaited(widget.store.clearBreakTimerState());
        _next();
      }
    });
  }

  void _persistBreakState() {
    if (advancing) return;
    final deadline = running ? DateTime.now().add(Duration(seconds: seconds)).millisecondsSinceEpoch : null;
    unawaited(widget.store.saveBreakTimerState(BreakTimerState(index: widget.index, blockIndex: widget.blockIndex, breakMinutes: breakMinutes, remainingSeconds: seconds, running: running, deadlineMillis: deadline)));
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) {
      _persistBreakState();
    } else if (state == AppLifecycleState.resumed && mounted && running && !advancing) {
      final saved = widget.store.breakTimerState;
      if (saved != null && saved.index == widget.index && saved.blockIndex == widget.blockIndex && saved.deadlineMillis != null) {
        final nextSeconds = ((saved.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil();
        if (nextSeconds <= 0) {
          seconds = 0;
          timer?.cancel();
          unawaited(widget.store.clearBreakTimerState());
          _next();
          return;
        }
        setState(() => seconds = nextSeconds.clamp(0, breakMinutes * 60).toInt());
      }
      _startTimer();
    }
  }

  void _setBreakMinutes(int minutes) {
    if (advancing || seconds < breakMinutes * 60 - 5) return;
    setState(() { breakMinutes = minutes; seconds = minutes * 60; });
    _persistBreakState();
  }

  void _toggleRunning() {
    if (advancing || seconds <= 0) return;
    setState(() => running = !running);
    _persistBreakState();
    if (running) _startTimer(); else timer?.cancel();
  }

  Future<void> _next() async {
    if (advancing) return;
    setState(() => advancing = true);
    timer?.cancel();
    await widget.store.clearBreakTimerState();
    final completed = widget.completed.clamp(0, FocusScreen.focusBlock).toInt();
    if (completed > 0) await widget.store.addItemCompletedMinutes(widget.index, completed);

    if (widget.index < 0 || widget.index >= widget.plan.items.length) {
      await widget.store.clearPlanPosition();
      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => CompletionScreen(plan: widget.plan, store: widget.store)));
      return;
    }

    final item = widget.plan.items[widget.index];
    final itemCompleted = widget.store.itemCompletedMinutes(widget.index).clamp(0, item.minutes).toInt();
    if (itemCompleted < item.minutes) {
      final nextBlock = (itemCompleted ~/ FocusScreen.focusBlock).clamp(0, 100000).toInt();
      await widget.store.setPlanPosition(widget.index, nextBlock);
      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: widget.plan, index: widget.index, blockIndex: nextBlock)));
      return;
    }

    var nextIndex = widget.index + 1;
    while (nextIndex < widget.plan.items.length) {
      final nextItem = widget.plan.items[nextIndex];
      final nextCompleted = widget.store.itemCompletedMinutes(nextIndex).clamp(0, nextItem.minutes).toInt();
      if (nextCompleted < nextItem.minutes) break;
      nextIndex++;
    }

    if (nextIndex < widget.plan.items.length) {
      await widget.store.setPlanPosition(nextIndex, 0);
      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: widget.plan, index: nextIndex, blockIndex: 0)));
      return;
    }

    await widget.store.clearPlanPosition();
    if (!mounted) return;
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => CompletionScreen(plan: widget.plan, store: widget.store)));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final clock = '${(seconds ~/ 60).toString().padLeft(2, '0')}:${(seconds % 60).toString().padLeft(2, '0')}';
    final progress = (1 - seconds / (breakMinutes * 60)).clamp(0.0, 1.0).toDouble();
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.all(28), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 520), child: Column(children: [
      Icon(Icons.spa_rounded, size: 64, color: scheme.primary),
      const SizedBox(height: 18), Text('Break time', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
      const SizedBox(height: 8), Text('You completed ${widget.completed} minute${widget.completed == 1 ? '' : 's'}. Reset before you return.', textAlign: TextAlign.center),
      const SizedBox(height: 20),
      Row(mainAxisAlignment: MainAxisAlignment.center, children: [const Text('Break length', style: TextStyle(fontWeight: FontWeight.w800)), const SizedBox(width: 10), ChoiceChip(label: const Text('5 min'), selected: breakMinutes == 5, onSelected: advancing ? null : (_) => _setBreakMinutes(5)), const SizedBox(width: 8), ChoiceChip(label: const Text('10 min'), selected: breakMinutes == 10, onSelected: advancing ? null : (_) => _setBreakMinutes(10))]),
      const SizedBox(height: 18), LinearProgressIndicator(value: progress, minHeight: 7), const SizedBox(height: 18),
      Text(clock, style: Theme.of(context).textTheme.displayMedium?.copyWith(fontWeight: FontWeight.w900)),
      const SizedBox(height: 24),
      const Card(child: Padding(padding: EdgeInsets.all(16), child: Column(children: [
        ListTile(leading: Icon(Icons.water_drop_rounded), title: Text('Drink some water'), subtitle: Text('Hydrate before you return.')),
        ListTile(leading: Icon(Icons.directions_walk_rounded), title: Text('Walk, stretch or move'), subtitle: Text('Give your body a reset.')),
        ListTile(leading: Icon(Icons.visibility_rounded), title: Text('Rest your eyes'), subtitle: Text('Look away from the screen.')),
        ListTile(leading: Icon(Icons.air_rounded), title: Text('Take a few slow breaths'), subtitle: Text('Relax your shoulders and jaw.')),
      ]))),
      const SizedBox(height: 20),
      Row(mainAxisAlignment: MainAxisAlignment.center, children: [OutlinedButton(onPressed: advancing ? null : _toggleRunning, child: Text(running ? 'Pause break' : 'Resume break')), const SizedBox(width: 10), FilledButton(onPressed: advancing ? null : _next, child: Text(advancing ? 'Saving…' : 'Continue'))]),
    ]))))));
  }
}

class CompletionScreen extends StatelessWidget {
  const CompletionScreen({super.key, required this.plan, required this.store});
  final StudyPlan plan;
  final LocalStore store;

  @override
  Widget build(BuildContext context) {
    final completed = plan.items.asMap().entries.fold<int>(0, (sum, entry) => sum + store.itemCompletedMinutes(entry.key).clamp(0, entry.value.minutes).toInt());
    final progress = plan.totalMinutes <= 0 ? 0.0 : (completed / plan.totalMinutes).clamp(0.0, 1.0).toDouble();
    final xp = completed * 2;
    return Scaffold(body: SafeArea(child: Center(child: Padding(padding: const EdgeInsets.all(28), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 520), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      Icon(Icons.emoji_events_rounded, size: 76, color: Theme.of(context).colorScheme.primary),
      const SizedBox(height: 20),
      Text('Study complete', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
      const SizedBox(height: 10),
      Text('$completed / ${plan.totalMinutes} minutes completed', style: const TextStyle(fontWeight: FontWeight.w700)),
      const SizedBox(height: 18),
      LinearProgressIndicator(value: progress, minHeight: 9),
      const SizedBox(height: 22),
      Card(child: Padding(padding: const EdgeInsets.all(18), child: Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
        _Stat(label: 'Focused', value: '$completed min'),
        _Stat(label: 'Progress', value: '${(progress * 100).round()}%'),
        _Stat(label: 'XP earned', value: '+$xp'),
      ]))),
      const SizedBox(height: 24),
      FilledButton.icon(onPressed: () => Navigator.popUntil(context, (route) => route.isFirst), icon: const Icon(Icons.home_rounded), label: const Text('Back to home')),
    ]))))));
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Column(children: [Text(value, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 3), Text(label)]);
}
