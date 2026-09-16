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
    required this.currentItemCompletedMinutes,
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
  final int currentItemCompletedMinutes;

  bool get hasPlan => plan != null && plan!.items.isNotEmpty;
  bool get isComplete => hasPlan && remainingMinutes == 0;
  bool get hasRemainingWork => hasPlan && remainingMinutes > 0;
}

class TodayEngine {
  const TodayEngine(this.store);
  final LocalStore store;

  TodaySnapshot build() {
    final plan = store.loadPlan();
    final planned = plan?.allocatedMinutes ?? 0;
    final completedByItem = plan == null ? const <int, int>{} : store.itemCompletedMinutesMap;
    final itemCompletedTotal = plan == null
        ? 0
        : plan.items.asMap().entries.fold<int>(
            0,
            (sum, entry) => sum + (completedByItem[entry.key] ?? 0).clamp(0, entry.value.minutes).toInt(),
          );

    // Item-level progress drives Resume/Completion, so prefer it whenever it
    // exists. This keeps Home, Focus and Completion consistent even if an old
    // aggregate progress value is stale or was written by a legacy caller.
    final aggregateCompleted = store.planCompletedMinutes.clamp(0, planned).toInt();
    final completed = planned <= 0
        ? 0
        : completedByItem.isNotEmpty
            ? itemCompletedTotal.clamp(0, planned).toInt()
            : aggregateCompleted;
    final remaining = planned <= 0 ? 0 : planned - completed;
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0).toDouble();

    var index = 0;
    var blockIndex = 0;
    var currentItemCompletedMinutes = 0;
    StudyItem? next;

    if (plan != null && plan.items.isNotEmpty) {
      for (var i = 0; i < plan.items.length; i++) {
        final item = plan.items[i];
        final itemCompleted = (completedByItem[i] ?? 0).clamp(0, item.minutes).toInt();
        if (itemCompleted < item.minutes) {
          index = i;
          blockIndex = (itemCompleted ~/ 25).clamp(0, 100000).toInt();
          currentItemCompletedMinutes = itemCompleted;
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
      currentItemCompletedMinutes = plan.items[index].minutes;
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
      currentItemCompletedMinutes: currentItemCompletedMinutes,
    );
  }
}
