import '../models/study_models.dart';
import 'local_store.dart';

class ProgressAnalytics {
  const ProgressAnalytics(this.store);
  final LocalStore store;

  ProgressSummary build({DateTime? today, int days = 7, Map<String, int>? dailyHistory, StudyPlan? plan, Map<int, int>? itemProgress}) {
    final anchor = today ?? DateTime.now();
    final safeDays = days.clamp(1, 30).toInt();
    final history = dailyHistory ?? store.dailyStudyMinutes;
    final values = <int>[];
    for (var offset = safeDays - 1; offset >= 0; offset--) {
      final date = anchor.subtract(Duration(days: offset));
      final key = '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
      values.add(history[key] ?? 0);
    }
    final total = values.fold<int>(0, (sum, value) => sum + value);
    final activeDays = values.where((value) => value > 0).length;
    final average = total / safeDays;
    final bestDay = values.isEmpty ? 0 : values.reduce((a, b) => a > b ? a : b);
    final goal = store.dailyGoalMinutes;
    final goalHitDays = values.where((value) => value >= goal).length;
    var currentStreak = 0;
    for (var offset = 0; offset < safeDays; offset++) {
      final index = safeDays - 1 - offset;
      if (values[index] <= 0) break;
      currentStreak++;
    }
    var bestStreak = 0;
    var runningStreak = 0;
    for (final value in values) {
      if (value > 0) {
        runningStreak++;
        if (runningStreak > bestStreak) bestStreak = runningStreak;
      } else {
        runningStreak = 0;
      }
    }
    final currentPlan = plan ?? store.loadPlan();
    final plannedMinutes = currentPlan?.allocatedMinutes ?? 0;
    final itemMap = currentPlan == null ? const <int, int>{} : itemProgress ?? store.itemCompletedMinutesMap;
    final itemCompleted = currentPlan == null ? 0 : currentPlan.items.asMap().entries.fold<int>(0, (sum, entry) => sum + (itemMap[entry.key] ?? 0).clamp(0, entry.value.minutes).toInt());
    final planCompleted = plannedMinutes <= 0 ? 0 : itemMap.isNotEmpty ? itemCompleted.clamp(0, plannedMinutes).toInt() : store.planCompletedMinutes.clamp(0, plannedMinutes).toInt();
    final planRemaining = (plannedMinutes - planCompleted).clamp(0, plannedMinutes).toInt();
    final planCompletionRate = plannedMinutes <= 0 ? 0.0 : (planCompleted / plannedMinutes).clamp(0.0, 1.0).toDouble();
    final estimatedPlanDaysRemaining = planRemaining <= 0 || average <= 0 ? null : (planRemaining / average).ceil();
    return ProgressSummary(days: safeDays, dailyMinutes: List.unmodifiable(values), totalMinutes: total, averageMinutes: average, activeDays: activeDays, bestDayMinutes: bestDay, goalHitDays: goalHitDays, consistencyRate: activeDays / safeDays, goalCompletionRate: ((total / (goal * safeDays)).clamp(0.0, 1.0)).toDouble(), currentStreak: currentStreak, bestStreak: bestStreak, plannedMinutes: plannedMinutes, planCompletedMinutes: planCompleted, planRemainingMinutes: planRemaining, planCompletionRate: planCompletionRate, estimatedPlanDaysRemaining: estimatedPlanDaysRemaining);
  }
}

class ProgressSummary {
  const ProgressSummary({required this.days, required this.dailyMinutes, required this.totalMinutes, required this.averageMinutes, required this.activeDays, required this.bestDayMinutes, required this.goalHitDays, required this.consistencyRate, required this.goalCompletionRate, required this.currentStreak, required this.bestStreak, required this.plannedMinutes, required this.planCompletedMinutes, required this.planRemainingMinutes, required this.planCompletionRate, required this.estimatedPlanDaysRemaining});
  final int days;
  final List<int> dailyMinutes;
  final int totalMinutes;
  final double averageMinutes;
  final int activeDays;
  final int bestDayMinutes;
  final int goalHitDays;
  final double consistencyRate;
  final double goalCompletionRate;
  final int currentStreak;
  final int bestStreak;
  final int plannedMinutes;
  final int planCompletedMinutes;
  final int planRemainingMinutes;
  final double planCompletionRate;
  final int? estimatedPlanDaysRemaining;
}
