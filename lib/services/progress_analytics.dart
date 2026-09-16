import 'local_store.dart';

class ProgressAnalytics {
  const ProgressAnalytics(this.store);

  final LocalStore store;

  ProgressSummary build({DateTime? today, int days = 7}) {
    final anchor = today ?? DateTime.now();
    final safeDays = days.clamp(1, 30).toInt();
    final values = <int>[];
    for (var offset = safeDays - 1; offset >= 0; offset--) {
      values.add(store.studyMinutesOn(anchor.subtract(Duration(days: offset))));
    }

    final total = values.fold<int>(0, (sum, value) => sum + value);
    final activeDays = values.where((value) => value > 0).length;
    final average = total / safeDays;
    final bestDay = values.isEmpty ? 0 : values.reduce((a, b) => a > b ? a : b);
    final goal = store.dailyGoalMinutes;
    final goalHitDays = values.where((value) => value >= goal).length;

    return ProgressSummary(
      days: safeDays,
      dailyMinutes: List.unmodifiable(values),
      totalMinutes: total,
      averageMinutes: average,
      activeDays: activeDays,
      bestDayMinutes: bestDay,
      goalHitDays: goalHitDays,
    );
  }
}

class ProgressSummary {
  const ProgressSummary({
    required this.days,
    required this.dailyMinutes,
    required this.totalMinutes,
    required this.averageMinutes,
    required this.activeDays,
    required this.bestDayMinutes,
    required this.goalHitDays,
  });

  final int days;
  final List<int> dailyMinutes;
  final int totalMinutes;
  final double averageMinutes;
  final int activeDays;
  final int bestDayMinutes;
  final int goalHitDays;
}
