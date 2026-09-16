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
    required this.dailyGoalMinutes,
    required this.todayCompletedMinutes,
    required this.goalProgress,
    required this.goalRemainingMinutes,
    required this.recommendedFocusMinutes,
    required this.recommendationReason,
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
  final int dailyGoalMinutes;
  final int todayCompletedMinutes;
  final double goalProgress;
  final int goalRemainingMinutes;
  final int recommendedFocusMinutes;
  final String recommendationReason;

  bool get hasPlan => plan != null && plan!.items.isNotEmpty;
  bool get isComplete => hasPlan && remainingMinutes == 0;
  bool get hasRemainingWork => hasPlan && remainingMinutes > 0;
  bool get dailyGoalReached => goalRemainingMinutes == 0;
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

    final aggregateCompleted = store.planCompletedMinutes.clamp(0, planned).toInt();
    final hasItemProgress = completedByItem.isNotEmpty;
    final completed = planned <= 0
        ? 0
        : hasItemProgress
            ? itemCompletedTotal.clamp(0, planned).toInt()
            : aggregateCompleted;
    final remaining = planned <= 0 ? 0 : planned - completed;
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0).toDouble();

    final dailyGoal = store.dailyGoalMinutes;
    final todayCompleted = store.studyMinutesOn(DateTime.now()).clamp(0, 1440).toInt();
    final goalRemaining = (dailyGoal - todayCompleted).clamp(0, dailyGoal).toInt();
    final goalProgress = (todayCompleted / dailyGoal).clamp(0.0, 1.0).toDouble();

    var index = 0;
    var blockIndex = 0;
    var currentItemCompletedMinutes = 0;
    StudyItem? next;

    if (plan != null && plan.items.isNotEmpty) {
      if (hasItemProgress) {
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
      } else {
        // Aggregate-only progress cannot tell us which subject was studied,
        // so interpret it in plan order. This is deterministic and matches
        // the way a sequential focus session consumes the plan.
        var remainingCompleted = aggregateCompleted;
        for (var i = 0; i < plan.items.length; i++) {
          final item = plan.items[i];
          if (item.minutes <= 0) continue;
          if (remainingCompleted >= item.minutes) {
            remainingCompleted -= item.minutes;
            continue;
          }
          index = i;
          currentItemCompletedMinutes = remainingCompleted.clamp(0, item.minutes).toInt();
          blockIndex = (currentItemCompletedMinutes ~/ 25).clamp(0, 100000).toInt();
          next = item;
          break;
        }
      }
    }

    if (next == null && plan != null && plan.items.isNotEmpty) {
      index = (plan.items.length - 1).clamp(0, 100000).toInt();
      blockIndex = ((plan.items[index].minutes + 24) ~/ 25).clamp(0, 100000).toInt();
      currentItemCompletedMinutes = plan.items[index].minutes;
    }

    final itemRemaining = next == null
        ? 0
        : (next.minutes - currentItemCompletedMinutes).clamp(0, next.minutes).toInt();
    final availableForGoal = goalRemaining > 0 ? goalRemaining : remaining;
    final recommended = remaining <= 0 || itemRemaining <= 0
        ? 0
        : [25, itemRemaining, remaining, availableForGoal].reduce((a, b) => a < b ? a : b);
    final reason = recommended == 0
        ? 'Today’s plan is complete.'
        : goalRemaining > 0 && goalRemaining < remaining
            ? 'Use this block to move toward your daily goal.'
            : 'Continue the first unfinished study item.';

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
      dailyGoalMinutes: dailyGoal,
      todayCompletedMinutes: todayCompleted,
      goalProgress: goalProgress,
      goalRemainingMinutes: goalRemaining,
      recommendedFocusMinutes: recommended,
      recommendationReason: reason,
    );
  }
}
