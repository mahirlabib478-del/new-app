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
  });

  final StudyPlan? plan;
  final int completedMinutes;
  final int remainingMinutes;
  final double progress;
  final int streak;
  final int xp;
  final int level;
  final StudyItem? nextItem;

  bool get hasPlan => plan != null && plan!.items.isNotEmpty;
}

class TodayEngine {
  const TodayEngine(this.store);
  final LocalStore store;

  TodaySnapshot build() {
    final plan = store.loadPlan();
    final planned = plan?.totalMinutes ?? 0;
    final completed = store.completedMinutes;
    final remaining = planned <= 0 ? 0 : (planned - completed).clamp(0, planned);
    final progress = planned <= 0 ? 0.0 : (completed / planned).clamp(0.0, 1.0);

    StudyItem? next;
    if (plan != null) {
      for (final item in plan.items) {
        if (item.minutes > 0) {
          next = item;
          break;
        }
      }
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
    );
  }
}
