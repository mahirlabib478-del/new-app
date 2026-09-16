import 'package:flutter/material.dart';

import '../services/local_store.dart';
import '../services/study_session_store.dart';

class SavedSessionsScreen extends StatefulWidget {
  const SavedSessionsScreen({super.key, required this.store, required this.onOpenFocus});

  final LocalStore store;
  final Future<void> Function() onOpenFocus;

  @override
  State<SavedSessionsScreen> createState() => _SavedSessionsScreenState();
}

class _SavedSessionsScreenState extends State<SavedSessionsScreen> {
  late final StudySessionStore sessionStore = StudySessionStore(widget.store);
  bool busy = false;

  Future<void> _resume(SavedStudySession session) async {
    if (busy) return;
    setState(() => busy = true);
    final restored = await sessionStore.restore(session.id);
    if (!mounted) return;
    if (!restored) {
      setState(() => busy = false);
      return;
    }
    Navigator.pop(context);
    await widget.onOpenFocus();
  }

  Future<void> _reset(SavedStudySession session) async {
    if (busy) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset saved session?'),
        content: const Text('This will clear the saved progress and return the session to the first study item. Your current active plan will not be changed.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Reset')),
        ],
      ),
    );
    if (confirmed != true) return;
    setState(() => busy = true);
    await sessionStore.reset(session.id);
    if (mounted) setState(() => busy = false);
  }

  Future<void> _delete(SavedStudySession session) async {
    if (busy) return;
    await sessionStore.delete(session.id);
    if (mounted) setState(() {});
  }

  String _resumeLabel(SavedStudySession session) {
    if (session.plan.items.isEmpty) return 'Invalid saved session';
    final index = session.currentIndex.clamp(0, session.plan.items.length - 1).toInt();
    final item = session.plan.items[index];
    final topic = item.topic.trim();
    final block = session.currentBlockIndex + 1;
    return topic.isEmpty ? 'Resume from ${item.title} • Block $block' : 'Resume from ${item.title} • $topic • Block $block';
  }

  @override
  Widget build(BuildContext context) {
    final sessions = sessionStore.sessions;
    return Scaffold(
      appBar: AppBar(title: const Text('Saved sessions')),
      body: sessions.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(28),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.bookmark_border_rounded, size: 56, color: Theme.of(context).colorScheme.primary),
                    const SizedBox(height: 14),
                    Text('No saved sessions yet', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 6),
                    const Text('When you start another plan, unfinished sessions stay here instead of disappearing.'),
                  ],
                ),
              ),
            )
          : ListView(
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
              children: [
                Text('Continue where you left off', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                const SizedBox(height: 6),
                const Text('Each session keeps its latest topic, block and progress until you resume or reset it.'),
                const SizedBox(height: 18),
                ...sessions.map(_sessionCard),
              ],
            ),
    );
  }

  Widget _sessionCard(SavedStudySession session) {
    final progress = session.plan.allocatedMinutes == 0 ? 0.0 : (session.completedMinutes / session.plan.allocatedMinutes).clamp(0.0, 1.0).toDouble();
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text(session.mode, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17))),
                IconButton(onPressed: busy ? null : () => _delete(session), tooltip: 'Delete session', icon: const Icon(Icons.delete_outline_rounded)),
              ],
            ),
            Text('${session.plan.allocatedMinutes} min plan • ${session.remainingMinutes} min remaining'),
            const SizedBox(height: 6),
            Text(_resumeLabel(session), style: const TextStyle(fontWeight: FontWeight.w800)),
            const SizedBox(height: 10),
            LinearProgressIndicator(value: progress, minHeight: 8),
            const SizedBox(height: 8),
            Text('${session.plan.items.length} study items • ${session.savedAt.toLocal().toString().substring(0, 16)}'),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: FilledButton.icon(onPressed: busy ? null : () => _resume(session), icon: const Icon(Icons.play_arrow_rounded), label: const Text('Resume'))),
                const SizedBox(width: 10),
                OutlinedButton.icon(onPressed: busy ? null : () => _reset(session), icon: const Icon(Icons.restart_alt_rounded), label: const Text('Reset')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
