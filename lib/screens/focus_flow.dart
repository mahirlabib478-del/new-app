import 'dart:async';
import 'dart:ui';
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
  @override State<FocusScreen> createState() => _FocusScreenState();
}

class _FocusScreenState extends State<FocusScreen> {
  late int activeIndex;
  late int activeBlockIndex;
  late int seconds;
  late int currentBlockMinutes;
  bool running = true;
  Timer? timer;
  StudyItem get item => widget.plan.items[activeIndex];

  int get totalBlocks => (item.minutes / FocusScreen.focusBlock).ceil();

  @override void initState() {
    super.initState();
    final savedIndex = widget.store.currentPlanIndex;
    final savedBlock = widget.store.currentBlockIndex;
    activeIndex = savedIndex >= 0 && savedIndex < widget.plan.items.length ? savedIndex : widget.index;
    activeBlockIndex = activeIndex == widget.index ? savedBlock : 0;
    final remaining = item.minutes - activeBlockIndex * FocusScreen.focusBlock;
    currentBlockMinutes = remaining > FocusScreen.focusBlock ? FocusScreen.focusBlock : remaining;
    if (currentBlockMinutes <= 0) {
      activeBlockIndex = 0;
      currentBlockMinutes = item.minutes.clamp(1, FocusScreen.focusBlock).toInt();
    }
    seconds = currentBlockMinutes * 60;
    unawaited(widget.store.setPlanPosition(activeIndex, activeBlockIndex));
    timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || !running) return;
      if (seconds > 0) setState(() => seconds--);
      if (seconds == 0) {
        timer?.cancel();
        _openBreak(currentBlockMinutes);
      }
    });
  }

  @override void dispose() { timer?.cancel(); super.dispose(); }

  void _openBreak(int completed) {
    timer?.cancel();
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => BreakScreen(store: widget.store, plan: widget.plan, index: activeIndex, blockIndex: activeBlockIndex, completed: completed)));
  }

  @override Widget build(BuildContext context) {
    final clock = '${(seconds ~/ 60).toString().padLeft(2, '0')}:${(seconds % 60).toString().padLeft(2, '0')}';
    final progress = currentBlockMinutes == 0 ? 0.0 : (1 - seconds / (currentBlockMinutes * 60)).clamp(0.0, 1.0);
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Focus mode'), centerTitle: true),
      body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.fromLTRB(24, 20, 24, 30), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 560), child: Column(children: [
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [
          Icon(Icons.auto_awesome_rounded, size: 16, color: scheme.primary), const SizedBox(width: 7),
          Text('DEEP FOCUS', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.5, color: scheme.primary)),
        ]),
        const SizedBox(height: 18),
        Text(item.title, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
        if (item.topic.isNotEmpty) ...[const SizedBox(height: 6), Text(item.topic, textAlign: TextAlign.center)],
        const SizedBox(height: 30),
        Container(
          width: 280, height: 280,
          decoration: BoxDecoration(shape: BoxShape.circle, color: scheme.surfaceContainerHighest, boxShadow: [BoxShadow(color: scheme.primary.withOpacity(.14), blurRadius: 40, spreadRadius: 2)]),
          child: Stack(alignment: Alignment.center, children: [
            SizedBox(width: 258, height: 258, child: CircularProgressIndicator(value: progress, strokeWidth: 10, strokeCap: StrokeCap.round, backgroundColor: scheme.outlineVariant)),
            Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Text(clock, style: Theme.of(context).textTheme.displayLarge?.copyWith(fontWeight: FontWeight.w900, fontFeatures: [const FontFeature.tabularFigures()])),
              const SizedBox(height: 3), Text(running ? 'Stay with one task' : 'Timer paused', style: Theme.of(context).textTheme.bodyMedium),
            ]),
          ]),
        ),
        const SizedBox(height: 24),
        Container(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10), decoration: BoxDecoration(color: scheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(99)), child: Text('Block ${activeBlockIndex + 1} of $totalBlocks  •  ${currentBlockMinutes} min focus', style: const TextStyle(fontWeight: FontWeight.w800))),
        const SizedBox(height: 26),
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [
          FilledButton.icon(onPressed: () => setState(() => running = !running), icon: Icon(running ? Icons.pause_rounded : Icons.play_arrow_rounded), label: Text(running ? 'Pause' : 'Resume')),
          const SizedBox(width: 12),
          OutlinedButton.icon(onPressed: () => _openBreak(((currentBlockMinutes * 60 - seconds) / 60).floor()), icon: const Icon(Icons.done_rounded), label: const Text('Finish early')),
        ]),
        const SizedBox(height: 22),
        Card(child: Padding(padding: const EdgeInsets.all(16), child: Row(children: [
          Icon(Icons.lightbulb_outline_rounded, color: scheme.primary), const SizedBox(width: 12),
          Expanded(child: Text('One block at a time. Your break is waiting when this block is done.', style: Theme.of(context).textTheme.bodyMedium)),
        ]))),
      ]))))),
    );
  }
}

class BreakScreen extends StatefulWidget {
  const BreakScreen({super.key, required this.store, required this.plan, required this.index, required this.blockIndex, required this.completed});
  final LocalStore store; final StudyPlan plan; final int index; final int blockIndex; final int completed;
  @override State<BreakScreen> createState() => _BreakScreenState();
}

class _BreakScreenState extends State<BreakScreen> {
  int seconds = 5 * 60; bool running = true; bool advancing = false; Timer? timer;

  @override void initState() {
    super.initState();
    timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || !running || advancing) return;
      if (seconds > 0) setState(() => seconds--);
      if (seconds == 0) { timer?.cancel(); _next(); }
    });
  }

  @override void dispose() { timer?.cancel(); super.dispose(); }

  Future<void> _next() async {
    if (advancing) return;
    setState(() => advancing = true);
    timer?.cancel();
    final completed = widget.completed.clamp(0, FocusScreen.focusBlock).toInt();
    if (completed > 0) await widget.store.addCompletedMinutes(completed);

    final item = widget.plan.items[widget.index];
    final nextBlock = widget.blockIndex + 1;
    if (nextBlock * FocusScreen.focusBlock < item.minutes) {
      await widget.store.setPlanPosition(widget.index, nextBlock);
      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => FocusScreen(store: widget.store, plan: widget.plan, index: widget.index, blockIndex: nextBlock)));
      return;
    }

    final nextIndex = widget.index + 1;
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

  @override Widget build(BuildContext context) {
    final clock = '${(seconds ~/ 60).toString().padLeft(2, '0')}:${(seconds % 60).toString().padLeft(2, '0')}';
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.all(28), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 520), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      Container(width: 88, height: 88, decoration: BoxDecoration(shape: BoxShape.circle, color: scheme.primaryContainer), child: Icon(Icons.spa_rounded, size: 46, color: scheme.onPrimaryContainer)),
      const SizedBox(height: 18), Text('Break time', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 8),
      Text('You completed ${widget.completed} minute${widget.completed == 1 ? '' : 's'}. Reset before the next focus block.', textAlign: TextAlign.center), const SizedBox(height: 28),
      Text(clock, style: Theme.of(context).textTheme.displayMedium?.copyWith(fontWeight: FontWeight.w900, fontFeatures: [const FontFeature.tabularFigures()])), const SizedBox(height: 24),
      Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(children: const [
        ListTile(leading: Icon(Icons.water_drop_rounded), title: Text('Drink some water'), subtitle: Text('Hydrate before you return.'), dense: true),
        ListTile(leading: Icon(Icons.directions_walk_rounded), title: Text('Walk, stretch or move'), subtitle: Text('Give your body a reset.'), dense: true),
        ListTile(leading: Icon(Icons.visibility_rounded), title: Text('Rest your eyes'), subtitle: Text('Look away from the screen.'), dense: true),
        ListTile(leading: Icon(Icons.air_rounded), title: Text('Take a few slow breaths'), subtitle: Text('Relax your shoulders and jaw.'), dense: true),
      ]))), const SizedBox(height: 20),
      Row(mainAxisAlignment: MainAxisAlignment.center, children: [
        OutlinedButton(onPressed: advancing ? null : () => setState(() => running = !running), child: Text(running ? 'Pause break' : 'Resume break')),
        const SizedBox(width: 10),
        FilledButton(onPressed: advancing ? null : _next, child: Text(advancing ? 'Saving…' : 'Continue')),
      ]),
    ])))));
  }
}

class CompletionScreen extends StatelessWidget {
  const CompletionScreen({super.key, required this.plan, required this.store});
  final StudyPlan plan;
  final LocalStore store;
  @override Widget build(BuildContext context) => Scaffold(body: Center(child: Padding(padding: const EdgeInsets.all(28), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
    const Icon(Icons.emoji_events_rounded, size: 72), const SizedBox(height: 20),
    Text('Session complete', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 8),
    Text('${plan.allocatedMinutes} planned minutes are done.', textAlign: TextAlign.center), const SizedBox(height: 18),
    Card(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 18), child: Column(children: [Text('+${plan.allocatedMinutes * 2} XP', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 4), Text('Total focus time: ${store.planCompletedMinutes} min')])),
    const SizedBox(height: 26), FilledButton.icon(onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst), icon: const Icon(Icons.home_rounded), label: const Text('Back to home')),
  ])));
}
