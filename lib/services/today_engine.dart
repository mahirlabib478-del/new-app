import '../models/study_models.dart';
import 'local_store.dart';

class TodaySnapshot {
  const TodaySnapshot({
    required this.plan,
    required this.completedMinutes,
    required this.remainingMinutes,
    required this.progress,
    required this.streak,
    required this.xp,
    required this.level,
    required this.nextItem,
    required this.currentIndex,
    required this.currentBlockIndex,
  });

  final StudyPlan? plan;
  final int completedMinutes;
  final int remainingMinutes;
  final double progress;
  final int streak;
  final int xp;
  final int level;
  final StudyItem? nextItem;
  final int currentIndex;
  final int currentBlockIndex;

  bool get hasPlan => plan != null && plan!.items.isNotEmpty;
}

class TodayEngine {
  const TodayEngine(this.store);
  final LocalStore store;

  TodaySnapshot build() {
    final plan = store.loadPlan();
    final planned = plan?.allocatedMinutes ?? 0;
    final completed = store.planCompletedMinutes.clamp(0, planned).toInt();
    final remaining = planned <= 0 ? 0 : planned - completed;
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0).toDouble();

    var index = 0;
    var blockIndex = 0;
    StudyItem? next;

    if (plan != null && plan.items.isNotEmpty) {
      final completedByItem = store.itemCompletedMinutesMap;
      for (var i = 0; i < plan.items.length; i++) {
        final item = plan.items[i];
        final itemCompleted = (completedByItem[i] ?? 0).clamp(0, item.minutes).toInt();
        if (itemCompleted < item.minutes) {
          index = i;
          blockIndex = (itemCompleted ~/ 25).clamp(0, 100000).toInt();
          next = item;
          break;
        }
      }
    }

    // If no item is left, keep the position harmless and expose no next item.
    // This prevents a completed plan from accidentally restarting at item 0.
    if (next == null && plan != null && plan.items.isNotEmpty) {
      index = (plan.items.length - 1).clamp(0, 100000).toInt();
      blockIndex = ((plan.items[index].minutes + 24) ~/ 25).clamp(0, 100000).toInt();
    }

    return TodaySnapshot(
      plan: plan,
      completedMinutes: completed,
      remainingMinutes: remaining,
      progress: progress,
      streak: store.streak,
      xp: store.xp,
      level: store.level,
      nextItem: next,
      currentIndex: index,
      currentBlockIndex: blockIndex,
    );
  }
}
