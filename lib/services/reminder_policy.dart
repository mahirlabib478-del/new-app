enum ReminderKind { study, breakTime, plan }

class ReminderRequest {
  const ReminderRequest({required this.kind, required this.title, required this.body});

  final ReminderKind kind;
  final String title;
  final String body;
}

class ReminderPolicy {
  const ReminderPolicy();

  ReminderRequest? studyReminder({
    required bool hasRemainingWork,
    required int goalRemainingMinutes,
  }) {
    if (!hasRemainingWork || goalRemainingMinutes <= 0) return null;

    return const ReminderRequest(
      kind: ReminderKind.study,
      title: 'Time to study',
      body: 'A focused study block can move you closer to today\'s goal.',
    );
  }

  ReminderRequest? breakReminder({required bool focusSessionCompleted}) {
    if (!focusSessionCompleted) return null;

    return const ReminderRequest(
      kind: ReminderKind.breakTime,
      title: 'Take a break',
      body: 'Your focus block is complete. Take a short break before the next block.',
    );
  }

  ReminderRequest? planReminder({required bool hasPlan, required bool hasRemainingWork}) {
    if (hasPlan || hasRemainingWork) return null;

    return const ReminderRequest(
      kind: ReminderKind.plan,
      title: 'Plan your study',
      body: 'Create today\'s study plan to get started.',
    );
  }
}
