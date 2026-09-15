import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/models/study_models.dart';
import 'package:study_os/screens/exam_planner_screen.dart';

void main() {
  test('exam plan uses the full study budget without oversized blocks', () {
    final items = generateExamPlan(
      nextDay: false,
      studyHours: 4,
      urgency: 2,
      subjects: const ['Math', 'Physics', 'Biology'],
      priorities: const {'Math': 3, 'Physics': 1, 'Biology': 2},
    );

    expect(items, isNotEmpty);
    expect(items.fold<int>(0, (sum, item) => sum + item.minutes), 240);
    expect(items.every((item) => item.minutes > 0 && item.minutes <= 25), isTrue);
  });

  test('exam plan gives higher priority at least as much time as low priority', () {
    final items = generateExamPlan(
      nextDay: true,
      studyHours: 2,
      urgency: 1,
      subjects: const ['High', 'Low'],
      priorities: const {'High': 3, 'Low': 1},
    );

    int minutesFor(String subject) => items
        .where((item) => item.title == subject)
        .fold<int>(0, (sum, item) => sum + item.minutes);

    expect(minutesFor('High'), greaterThanOrEqualTo(minutesFor('Low')));
    expect(
      items.every(
        (item) => item.topic == 'High-impact revision' || item.topic == 'Final review',
      ),
      isTrue,
    );
  });

  test('empty subjects produce no exam plan', () {
    expect(
      generateExamPlan(
        nextDay: false,
        studyHours: 4,
        urgency: 2,
        subjects: const [],
        priorities: const {},
      ),
      isEmpty,
    );
  });

  test('generated plan matches StudyPlan allocation exactly', () {
    final items = generateExamPlan(
      nextDay: false,
      studyHours: 3,
      urgency: 3,
      subjects: const ['English', 'Chemistry'],
      priorities: const {'English': 2, 'Chemistry': 3},
    );
    final plan = StudyPlan(totalMinutes: 180, items: items);

    expect(plan.allocatedMinutes, plan.totalMinutes);
  });
}
