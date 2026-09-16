import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../models/study_models.dart';
import '../services/local_store.dart';

class FocusScreen extends StatefulWidget {
  const FocusScreen({super.key, required this.store, required this.plan, required this.index, required this.blockIndex, this.onFocusBlockCompleted});
  final LocalStore store;
  final StudyPlan plan;
  final int index;
  final int blockIndex;
  final Future<void> Function()? onFocusBlockCompleted;
  static const focusBlock = 25;
  @override State<FocusScreen> createState() => _FocusScreenState();
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
  int get completedTopicCount => widget.plan.items.asMap().entries.where((entry) {
    final minutes = entry.value.minutes;
    if (minutes <= 0) return false;
    return widget.store.itemCompletedMinutes(entry.key).clamp(0, minutes).toInt() >= minutes;
  }).length;
  int get totalTopicCount => widget.plan.items.where((item) => item.minutes > 0).length;

  int _firstRemainingIndex() {
    for (var i = 0; i < widget.plan.items.length; i++) {
      final minutes = widget.store.itemCompletedMinutes(i).clamp(0, widget.plan.items[i].minutes).toInt();
      if (minutes < widget.plan.items[i].minutes) return i;
    }
    return -1;
  }

  @override void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.plan.items.isEmpty) {
      activeIndex = 0; activeBlockIndex = 0; currentBlockMinutes = 1; seconds = 60; running = false;
      WidgetsBinding.instance.addPostFrameCallback((_) => _showCompletion()); return;
    }
    final savedIndex = widget.store.currentPlanIndex;
    final savedIsRemaining = savedIndex >= 0 && savedIndex < widget.plan.items.length && widget.store.itemCompletedMinutes(savedIndex).clamp(0, widget.plan.items[savedIndex].minutes).toInt() < widget.plan.items[savedIndex].minutes;
    final requestedIsRemaining = widget.index >= 0 && widget.index < widget.plan.items.length && widget.store.itemCompletedMinutes(widget.index).clamp(0, widget.plan.items[widget.index].minutes).toInt() < widget.plan.items[widget.index].minutes;
    if (savedIsRemaining) activeIndex = savedIndex; else if (requestedIsRemaining) activeIndex = widget.index; else activeIndex = _firstRemainingIndex();
    if (activeIndex < 0) {
      activeIndex = 0; activeBlockIndex = 0; currentBlockMinutes = 1; seconds = 60; running = false;
      WidgetsBinding.instance.addPostFrameCallback((_) => _showCompletion()); return;
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
        seconds = remainingSeconds.clamp(0, currentBlockMinutes * 60).toInt(); running = true; expiredOnResume = remainingSeconds <= 0;
      } else { seconds = saved.remainingSeconds.clamp(0, currentBlockMinutes * 60).toInt(); running = false; }
    }
    unawaited(widget.store.setPlanPosition(activeIndex, activeBlockIndex));
    if (expiredOnResume) WidgetsBinding.instance.addPostFrameCallback((_) => _openBreak(currentBlockMinutes));
    else { _persistTimerState(); if (running) _startTimer(); }
  }

  void _startTimer() {
    timer?.cancel();
    timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || !running || transitioning) return;
      if (seconds > 0) setState(() => seconds--);
      if (seconds == 0) { timer?.cancel(); unawaited(widget.store.clearFocusTimerState()); unawaited(_openBreak(currentBlockMinutes)); }
    });
  }
  void _persistTimerState() {
    if (transitioning) return;
    final deadline = running ? DateTime.now().add(Duration(seconds: seconds)).millisecondsSinceEpoch : null;
    unawaited(widget.store.saveFocusTimerState(FocusTimerState(index: activeIndex, blockIndex: activeBlockIndex, remainingSeconds: seconds, running: running, deadlineMillis: deadline)));
  }
  @override void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) _persistTimerState();
    else if (state == AppLifecycleState.resumed && mounted && running && !transitioning) {
      final saved = widget.store.focusTimerState;
      if (saved != null && saved.index == activeIndex && saved.blockIndex == activeBlockIndex && saved.deadlineMillis != null) {
        final nextSeconds = ((saved.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil();
        if (nextSeconds <= 0) { seconds = 0; timer?.cancel(); unawaited(_openBreak(currentBlockMinutes)); return; }
        setState(() => seconds = nextSeconds.clamp(0, currentBlockMinutes * 60).toInt());
      }
      _startTimer();
    }
  }
  void _toggleRunning() {
    if (transitioning || seconds <= 0) return;
    setState(() => running = !running); _persistTimerState(); if (running) _startTimer(); else timer?.cancel();
  }
  Future<void> _showCompletion() async {
    if (!mounted || transitioning) return;
    transitioning = true; timer?.cancel(); await widget.store.clearFocusTimerState(); await widget.store.clearPlanPosition();
    if (!mounted) return;
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => CompletionScreen(plan: widget.plan, store: widget.store)));
  }
  Future<void> _openBreak(int completed) async {
    if (transitioning || !mounted) return;
    transitioning = true; timer?.cancel();
    final safeCompleted = completed.clamp(0, currentBlockMinutes).toInt();
    if (safeCompleted > 0) {
      await widget.store.addItemCompletedMinutes(activeIndex, safeCompleted);
      if (widget.onFocusBlockCompleted != null) unawaited(widget.onFocusBlockCompleted!());
    }
    await widget.store.clearFocusTimerState();
    if (!mounted) return;
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => BreakScreen(store: widget.store, plan: widget.plan, index: activeIndex, blockIndex: activeBlockIndex, completed: safeCompleted, onFocusBlockCompleted: widget.onFocusBlockCompleted));
  }
  @override void dispose() { WidgetsBinding.instance.removeObserver(this); timer?.cancel(); super.dispose(); }

  @override Widget build(BuildContext context) {
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
        Card(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14), child: Row(children: [Icon(Icons.check_circle_outline_rounded, color: scheme.primary), const SizedBox(width: 10), Expanded(child: Text('Topics completed', style: const TextStyle(fontWeight: FontWeight.w800))), Text('$completedTopicCount / $totalTopicCount', style: TextStyle(fontWeight: FontWeight.w900, color: scheme.primary))]))),
        const SizedBox(height: 12),
        Row(children: [const Expanded(child: Text('Topic progress', style: TextStyle(fontWeight: FontWeight.w800))), Text('${completedForItem}/${item.minutes} min', style: const TextStyle(fontWeight: FontWeight.w800))]),
        const SizedBox(height: 8), LinearProgressIndicator(value: itemProgress, minHeight: 7),
        const SizedBox(height: 26),
        Container(width: 280, height: 280, decoration: BoxDecoration(shape: BoxShape.circle, color: scheme.surfaceContainerHighest), child: Stack(alignment: Alignment.center, children: [SizedBox(width: 258, height: 258, child: CircularProgressIndicator(value: progress, strokeWidth: 10, strokeCap: StrokeCap.round)), Column(mainAxisAlignment: MainAxisAlignment.center, children: [Text(clock, style: Theme.of(context).textTheme.displayLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 3), Text(running ? 'Stay with one task' : 'Timer paused')])])),
        const SizedBox(height: 24),
        Text('Block ${activeBlockIndex + 1} of $totalBlocks • $currentBlockMinutes min focus', style: const TextStyle(fontWeight: FontWeight.w800)),
        const SizedBox(height: 24),
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [FilledButton.icon(onPressed: transitioning ? null : _toggleRunning, icon: Icon(running ? Icons.pause_rounded : Icons.play_arrow_rounded), label: Text(running ? 'Pause' : 'Resume')), const SizedBox(width: 12), OutlinedButton.icon(onPressed: transitioning || elapsedMinutes < 1 ? null : () => unawaited(_openBreak(elapsedMinutes)), icon: const Icon(Icons.done_rounded), label: const Text('Finish early'))]),
        const SizedBox(height: 22),
        Card(child: Padding(padding: const EdgeInsets.all(16), child: Text('Your timer is saved locally. If the app closes, you can return and continue from this block.', style: Theme.of(context).textTheme.bodyMedium))),
      ]))))),
    );
  }
}

class BreakScreen extends StatefulWidget {
  const BreakScreen({super.key, required this.store, required this.plan, required this.index, required this.blockIndex, required this.completed, this.onFocusBlockCompleted});
  final LocalStore store;
  final StudyPlan plan;
  final int index;
  final int blockIndex;
  final int completed;
  final Future<void> Function()? onFocusBlockCompleted;
  @override State<BreakScreen> createState() => _BreakScreenState();
}

class _BreakScreenState extends State<BreakScreen> with WidgetsBindingObserver {
  late int seconds;
  bool running = true;
  bool advancing = false;
  Timer? timer;
  @override void initState() { super.initState(); WidgetsBinding.instance.addObserver(this); final saved = widget.store.breakTimerState; seconds = saved?.remainingSeconds ?? 5 * 60; running = saved?.running ?? true; if (saved?.deadlineMillis != null && running) { seconds = ((saved!.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil().clamp(0, 5 * 60).toInt(); } if (running) _startTimer(); _persist(); }
  void _startTimer() { timer?.cancel(); timer = Timer.periodic(const Duration(seconds: 1), (_) { if (!mounted || !running || advancing) return; if (seconds > 0) setState(() => seconds--); if (seconds == 0) { timer?.cancel(); unawaited(_continue()); } }); }
  void _persist() { if (advancing) return; final deadline = running ? DateTime.now().add(Duration(seconds: seconds)).millisecondsSinceEpoch : null; unawaited(widget.store.saveBreakTimerState(BreakTimerState(remainingSeconds: seconds, running: running, deadlineMillis: deadline))); }
  @override void didChangeAppLifecycleState(AppLifecycleState state) { if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) _persist(); else if (state == AppLifecycleState.resumed && mounted && running && !advancing) { final saved = widget.store.breakTimerState; if (saved?.deadlineMillis != null) { final next = ((saved!.deadlineMillis! - DateTime.now().millisecondsSinceEpoch) / 1000).ceil(); if (next <= 0) { seconds = 0; timer?.cancel(); unawaited(_continue()); return; } setState(() => seconds = next.clamp(0, 5 * 60).toInt()); } _startTimer(); } }
  Future<void> _continue() async {
    if (advancing || !mounted) return;
    advancing = true; timer?.cancel(); await widget.store.clearBreakTimerState();
    final nextIndex = _nextRemainingIndex();
    if (nextIndex < 0) { await widget.store.clearPlanPosition(); if (!mounted) return; Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => CompletionScreen(plan: widget.plan, store: widget.store))); return; }
    final completed = widget.store.itemCompletedMinutes(widget.index).clamp(0, widget.plan.items[widget.index].minutes).toInt();
    final totalBlocks = math.max(1, (widget.plan.items[nextIndex].minutes / FocusScreen.focusBlock).ceil());
    final nextBlock = nextIndex == widget.index ? math.min(widget.blockIndex + 1, totalBlocks - 1) : 0;
    await widget.store.setPlanPosition(nextIndex, nextBlock);
    if (!mounted) return;
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: widget.plan, index: nextIndex, blockIndex: nextBlock, onFocusBlockCompleted: widget.onFocusBlockCompleted)));
  }
  int _nextRemainingIndex() { for (var i = 0; i < widget.plan.items.length; i++) { final done = widget.store.itemCompletedMinutes(i).clamp(0, widget.plan.items[i].minutes).toInt(); if (done < widget.plan.items[i].minutes) return i; } return -1; }
  @override void dispose() { WidgetsBinding.instance.removeObserver(this); timer?.cancel(); super.dispose(); }
  @override Widget build(BuildContext context) { final clock = '${(seconds ~/ 60).toString().padLeft(2, '0')}:${(seconds % 60).toString().padLeft(2, '0')}'; return Scaffold(appBar: AppBar(title: const Text('Break time'), centerTitle: true), body: SafeArea(child: Center(child: Padding(padding: const EdgeInsets.all(24), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [const Icon(Icons.self_improvement_rounded, size: 72), const SizedBox(height: 18), Text('Take a real break', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 10), Text(clock, style: Theme.of(context).textTheme.displayLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 22), FilledButton.icon(onPressed: advancing ? null : () => unawaited(_continue()), icon: const Icon(Icons.play_arrow_rounded), label: const Text('Continue')), const SizedBox(height: 12), TextButton(onPressed: advancing ? null : () { setState(() => running = !running); _persist(); if (running) _startTimer(); else timer?.cancel(); }, child: Text(running ? 'Skip / Pause' : 'Resume'))])))); }
}

class CompletionScreen extends StatelessWidget {
  const CompletionScreen({super.key, required this.plan, required this.store});
  final StudyPlan plan;
  final LocalStore store;
  @override Widget build(BuildContext context) { final completed = store.planCompletedMinutes.clamp(0, plan.allocatedMinutes).toInt(); final total = plan.allocatedMinutes; final progress = total == 0 ? 0.0 : (completed / total).clamp(0.0, 1.0).toDouble(); return Scaffold(appBar: AppBar(title: const Text('Complete'), centerTitle: true), body: Center(child: Padding(padding: const EdgeInsets.all(24), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [const Icon(Icons.celebration_rounded, size: 84), const SizedBox(height: 18), Text('Study complete', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 12), Text('$completed / $total minutes completed', style: const TextStyle(fontWeight: FontWeight.w800)), const SizedBox(height: 16), SizedBox(width: 220, child: LinearProgressIndicator(value: progress, minHeight: 10)), const SizedBox(height: 28), FilledButton.icon(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.home_rounded), label: const Text('Back to home'))])))); }
}
