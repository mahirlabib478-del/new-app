import 'local_store.dart';

class Gamification {
  const Gamification(this.store);

  final LocalStore store;

  List<Achievement> achievements() => [
        Achievement('First Focus', 'Complete your first study minute', store.completedMinutes >= 1),
        Achievement('1 Hour', 'Study for 60 total minutes', store.completedMinutes >= 60),
        Achievement('5 Focus Blocks', 'Complete five focus blocks', store.sessions >= 5),
        Achievement('3 Day Streak', 'Study three days in a row', store.streak >= 3),
        Achievement('500 XP', 'Earn 500 XP', store.xp >= 500),
      ];

  int xpToNextLevel() => 250 - store.levelProgress;
}

class Achievement {
  const Achievement(this.title, this.description, this.unlocked);

  final String title;
  final String description;
  final bool unlocked;
}
