import 'package:flutter_test/flutter_test.dart';
import 'package:study_os/services/reminder_policy.dart';

void main() {
  const policy = ReminderPolicy();

  test('study reminder is created only when work and goal remain', () {
    final reminder = policy.studyReminder(
      hasRemainingWork: true,
      goalRemainingMinutes: 30,
    );

    expect(reminder?.kind, ReminderKind.study);
    expect(reminder?.title, 'Time to study');
    expect(
      policy.studyReminder(hasRemainingWork: false, goalRemainingMinutes: 30),
      isNull,
    );
    expect(
      policy.studyReminder(hasRemainingWork: true, goalRemainingMinutes: 0),
      isNull,
    );
  });

  test('break reminder is tied to a completed focus session', () {
    expect(
      policy.breakReminder(focusSessionCompleted: false),
      isNull,
    );
    expect(
      policy.breakReminder(focusSessionCompleted: true)?.kind,
      ReminderKind.breakTime,
    );
  });

  test('plan reminder is offered only when no plan exists', () {
    expect(
      policy.planReminder(hasPlan: true, hasRemainingWork: true),
      isNull,
    );
    expect(
      policy.planReminder(hasPlan: true, hasRemainingWork: false),
      isNull,
    );

    final reminder = policy.planReminder(hasPlan: false, hasRemainingWork: false);
    expect(reminder?.kind, ReminderKind.plan);
    expect(reminder?.title, 'Plan your study');
  });
}
