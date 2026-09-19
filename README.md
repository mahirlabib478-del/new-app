# Study OS

A professional, offline-first study companion built with Android & Jetpack Compose.

## Product vision

**Plan → Focus → Break → Complete → Track → Improve**

Study OS is designed around three core study modes:

- **Regular Study**: Structured Pomodoro & Deep Focus intervals with customizable block counts.
- **Exam Preparation**: Priority-ranked exam planner with syllabus topics and readiness tracking.
- **Next Day Exam**: Blitz cram sessions for upcoming exams due tomorrow.

## Architecture

- **Jetpack Compose & Material 3**: Fluid UI with dynamic theming (Midnight, Pitch Black AMOLED, Espresso, Ocean, Forest, Paper Sepia, Mint, and Sunrise).
- **Room SQLite Local Persistence**: 100% offline-first local storage for study plans, exams, session logs, streaks, and user profile.
- **Synthesized Ambient Audio**: Real-time noise and binaural tone generator using Android AudioTrack (White Noise, Gentle Rain, Deep Focus 196Hz Sine wave, Forest Stream).
- **Gamification & Habit Engine**: XP gain per study minute, scholar levels, daily study targets, and streak tracking.

## Development

Build with Gradle:
```bash
gradle assembleDebug
```
