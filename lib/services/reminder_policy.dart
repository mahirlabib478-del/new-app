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
    bool bangla = false,
  }) {
    if (!hasRemainingWork || goalRemainingMinutes <= 0) return null;

    final safeRemaining = remainingWorkMinutes.clamp(0, 1440).toInt();
    final body = bangla
        ? (goalRemainingMinutes <= 25
            ? 'আজকের লক্ষ্য প্রায় পূর্ণ। আরেকটি ফোকাস ব্লকে এটি শেষ হতে পারে।'
            : safeRemaining <= 25
                ? 'আরেকটি ফোকাস ব্লকে বাকি স্টাডি শেষ করতে পারবেন।'
                : 'আরও $safeRemaining মিনিট স্টাডি বাকি। একটি ফোকাস ব্লক শুরু করুন।')
        : goalRemainingMinutes <= 25
            ? 'You are close to today\'s goal. One focused block can finish it.'
            : safeRemaining <= 25
                ? 'One focused block can clear the remaining study work.'
                : 'You have $safeRemaining minutes of study work remaining. Start a focused block.';

    return ReminderRequest(
      kind: ReminderKind.study,
      title: bangla ? 'স্টাডির সময়' : 'Time to study',
      body: body,
    );
  }

  ReminderRequest? breakReminder({required bool focusSessionCompleted, bool bangla = false}) {
    if (!focusSessionCompleted) return null;

    return const ReminderRequest(
      kind: ReminderKind.breakTime,
      title: bangla ? 'একটু বিরতি নিন' : 'Take a break',
      body: bangla ? 'ফোকাস ব্লক শেষ হয়েছে। পরের ব্লকের আগে একটু বিরতি নিন।' : 'Your focus block is complete. Take a short break before the next block.',
    );
  }

  ReminderRequest? planReminder({required bool hasPlan, required bool hasRemainingWork, bool bangla = false}) {
    if (hasPlan || hasRemainingWork) return null;

    return const ReminderRequest(
      kind: ReminderKind.plan,
      title: bangla ? 'স্টাডি প্ল্যান তৈরি করুন' : 'Plan your study',
      body: bangla ? 'শুরু করতে আজকের স্টাডি প্ল্যান তৈরি করুন।' : 'Create today\'s study plan to get started.',
    );
  }
}
