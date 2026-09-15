import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/models/study_models.dart';

void main() {
  test('StudyPlan calculates allocated and remaining minutes', () {
    final plan = StudyPlan(
      totalMinutes: 120,
      items: [
        StudyItem(title: 'Math', topic: 'Algebra', minutes: 25),
        StudyItem(title: 'Physics', topic: 'Motion', minutes: 50),
      ],
    );

    expect(plan.allocatedMinutes, 75);
    expect(plan.remainingMinutes, 45);
  });

  test('StudyItem and StudyPlan survive JSON round trip', () {
    final original = StudyPlan(
      totalMinutes: 60,
      items: [
        StudyItem(title: 'English', topic: 'Writing • Essays', minutes: 25),
        StudyItem(title: 'ICT', topic: 'Networks', minutes: 35),
      ],
    );

    final restored = StudyPlan.fromJson(original.toJson());

    expect(restored.totalMinutes, original.totalMinutes);
    expect(restored.items.length, 2);
    expect(restored.items.first.title, 'English');
    expect(restored.items.first.topic, 'Writing • Essays');
    expect(restored.items.last.minutes, 35);
    expect(restored.allocatedMinutes, 60);
  });
}
