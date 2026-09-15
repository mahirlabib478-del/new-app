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
    final completed = store.planCompletedMinutes.clamp(0, planned);
    final remaining = planned <= 0 ? 0 : planned - completed;
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0);
    final index = plan == null || plan.items.isEmpty
        ? 0
        : store.currentPlanIndex.clamp(0, plan.items.length - 1);
    final next = plan == null || plan.items.isEmpty ? null : plan.items[index];

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
      currentBlockIndex: store.currentBlockIndex,
    );
  }
}
