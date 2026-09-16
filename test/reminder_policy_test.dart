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

  test('study reminder adapts when the daily goal is nearly complete', () {
    final reminder = policy.studyReminder(
      hasRemainingWork: true,
      goalRemainingMinutes: 20,
      remainingWorkMinutes: 90,
    );

    expect(reminder?.body, contains('close to today\'s goal'));
  });

  test('study reminder adapts to a single remaining block', () {
    final reminder = policy.studyReminder(
      hasRemainingWork: true,
      goalRemainingMinutes: 40,
      remainingWorkMinutes: 25,
    );

    expect(reminder?.body, contains('One focused block'));
  });

  test('study reminder includes larger remaining workload', () {
    final reminder = policy.studyReminder(
      hasRemainingWork: true,
      goalRemainingMinutes: 120,
      remainingWorkMinutes: 100,
    );

    expect(reminder?.body, contains('100 minutes'));
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
