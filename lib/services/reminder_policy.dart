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
    int remainingWorkMinutes = 25,
  }) {
    if (!hasRemainingWork || goalRemainingMinutes <= 0) return null;

    final safeRemaining = remainingWorkMinutes.clamp(0, 1440).toInt();
    final body = goalRemainingMinutes <= 25
        ? 'You are close to today\'s goal. One focused block can finish it.'
        : safeRemaining <= 25
            ? 'One focused block can clear the remaining study work.'
            : 'You have $safeRemaining minutes of study work remaining. Start a focused block.';

    return ReminderRequest(
      kind: ReminderKind.study,
      title: 'Time to study',
      body: body,
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
