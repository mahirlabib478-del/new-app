import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/screens/exam_planner_screen.dart';

void main() {
  test('exam plan preserves the requested study time', () {
    final items = generateExamPlan(
      nextDay: false,
      studyHours: 4,
      urgency: 2,
      subjects: ['Math', 'Physics', 'Chemistry'],
      priorities: {'Math': 3, 'Physics': 2, 'Chemistry': 1},
    );

    expect(items.fold<int>(0, (sum, item) => sum + item.minutes), 240);
    expect(items.every((item) => item.minutes > 0 && item.minutes <= 25), isTrue);
  });

  test('exam plan gives more focus blocks to higher weighted subjects', () {
    final items = generateExamPlan(
      nextDay: true,
      studyHours: 5,
      urgency: 3,
      subjects: ['Math', 'Physics'],
      priorities: {'Math': 3, 'Physics': 1},
    );

    final mathMinutes = items
        .where((item) => item.title == 'Math')
        .fold<int>(0, (sum, item) => sum + item.minutes);
    final physicsMinutes = items
        .where((item) => item.title == 'Physics')
        .fold<int>(0, (sum, item) => sum + item.minutes);

    expect(mathMinutes, greaterThan(physicsMinutes));
  });

  test('exam plan handles study time shorter than one focus block', () {
    final items = generateExamPlan(
      nextDay: true,
      studyHours: 1,
      urgency: 3,
      subjects: ['Physics'],
      priorities: {'Physics': 3},
    );

    expect(items, hasLength(3));
    expect(items.fold<int>(0, (sum, item) => sum + item.minutes), 60);
    expect(items.last.topic, 'Final review');
  });

  test('exam plan clamps invalid urgency and priorities safely', () {
    final items = generateExamPlan(
      nextDay: false,
      studyHours: 2,
      urgency: 99,
      subjects: ['Physics', 'Math'],
      priorities: {'Physics': -10, 'Math': 99},
    );

    expect(items.fold<int>(0, (sum, item) => sum + item.minutes), 120);
    expect(items, isNotEmpty);
  });

  test('exam plan normalizes blank and duplicate subjects', () {
    final items = generateExamPlan(
      nextDay: false,
      studyHours: 1,
      urgency: 2,
      subjects: [' Physics ', 'physics', ''],
      priorities: {'Physics': 3},
    );

    expect(items, hasLength(3));
    expect(items.every((item) => item.title == 'Physics'), isTrue);
    expect(items.fold<int>(0, (sum, item) => sum + item.minutes), 60);
  });

  test('exam plan returns empty for missing subjects or study time', () {
    expect(
      generateExamPlan(
        nextDay: false,
        studyHours: 0,
        urgency: 2,
        subjects: ['Physics'],
        priorities: {'Physics': 2},
      ),
      isEmpty,
    );
    expect(
      generateExamPlan(
        nextDay: false,
        studyHours: 2,
        urgency: 2,
        subjects: [],
        priorities: const {},
      ),
      isEmpty,
    );
  });
}
