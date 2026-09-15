# Study OS

A professional, offline-first study companion built with Flutter.

## Product vision

**Plan → Focus → Break → Complete → Track → Improve**

Study OS is designed around three study modes:

- Regular Study
- Exam Preparation
- Next Day Exam

## Zero-cost architecture

- Flutter + Dart
- Local persistence (starting with SharedPreferences; SQLite can be introduced when the data model grows)
- Offline-first
- No backend required for the first release

## Initial implementation

The first milestone focuses on the core experience:

1. Home dashboard
2. Study mode selection
3. Regular Study setup
4. Subject/topic time allocation with a hard total-time cap
5. Focus timer
6. Break flow
7. Early-finish reward
8. Progress foundation
9. Theme system

## Development

```bash
flutter pub get
flutter run
```
