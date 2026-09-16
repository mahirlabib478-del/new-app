# Study OS — Feature Coverage

This checklist tracks whether a feature is implemented, visible from the app UI, reachable through navigation, and covered by automated tests.

## User-facing map

| Feature | Primary UI | Entry point | Core logic | Test coverage | Status |
|---|---|---|---|---|---|
| Home / Today overview | Attractive Home | Home tab | TodayEngine | navigation/home tests | ✅ |
| Today Engine | Today Engine screen | Study tab → Today Engine | TodayEngine | today_engine tests + navigation regression | ✅ exposed |
| Regular Study | Setup + Regular Study Planner | Home/Study → Regular Study | planner + LocalStore | planner/setup tests | ✅ |
| Exam Preparation | Exam Planner | Home/Study → Exam Preparation | Exam planner | exam tests | ✅ |
| Next Day Exam | Exam Planner | Home/Study → Next Day Exam | Exam planner | exam tests | ✅ |
| Focus Mode | FocusScreen | Start/Resume plan | Focus flow + LocalStore | focus tests | ✅ |
| Break flow | BreakScreen | Focus completion | Focus flow | focus tests | ✅ |
| Saved Sessions | SavedSessionsScreen | Study tab → Saved sessions | StudySessionStore | session tests | ✅ |
| Resume exact position | FocusScreen | Saved session → Resume | TodayEngine + canonical plan identity | session/home resume tests | ✅ |
| Progress dashboard | ProgressDashboard | Progress tab | ProgressAnalytics + Gamification | dashboard/analytics tests | ✅ |
| XP / level / streak | Home + Progress | Home/Progress | Gamification + LocalStore | gamification tests | ✅ |
| Achievements | Progress dashboard | Progress tab | Gamification | dashboard/gamification tests | ✅ |
| Daily goal | Home + Progress + Profile | Profile / Progress | LocalStore | dashboard/profile tests | ✅ |
| Custom subjects | Setup | Regular Study | Setup + planner allocation | setup custom subject tests | ✅ |
| Themes | Profile | Profile tab | LocalStore + app theme | profile tests | ✅ |
| English / বাংলা | Home/nav/Profile | Profile → Language | AppLanguage | language/profile tests | ✅ |
| Sound effects | Profile + interactions | Profile → Sound Effects | SoundEffects | sound tests | ✅ |
| Study reminders | Profile | Profile → Reminders | ReminderCoordinator/Policy/Scheduler | reminder tests | ✅ |
| Update gate | Startup | App launch | UpdateService + UpdateGate | update tests | ✅ |
| Android CI / APK | CI workflow | GitHub Actions | Flutter build workflow | CI | ✅ |

## Integration notes found during audit

### Fixed in this audit

- **Today Engine was implemented but not reachable from the main navigation.** It is now exposed from the Study tab with a dedicated entry card.
- A widget regression test was added to verify `Study → Today Engine` opens `TodayEngineScreen`.

### Intentional architecture notes

- `ProgressScreen` remains in the codebase as an older progress UI. The shipped navigation uses `ProgressDashboard`, which contains the current analytics, goal, plan pace, XP, streak, and achievements experience. It is therefore not treated as a missing user-facing feature.
- Notification delivery still needs physical Android-device verification; unit tests validate policy/coordinator behavior but cannot prove OS-level delivery.
- Update behavior is tested as an application flow, while actual release installation/upgrade must still be verified with a signed APK using the same package ID/signing key and a higher versionCode.

## End-to-end journeys to keep green

1. **Regular Study:** Home/Study → Regular Study → choose subjects/time → allocate topics → start Focus → Break → completion → Progress.
2. **Exam:** Home/Study → Exam Preparation or Next Day Exam → plan → Focus → completion → Progress.
3. **Resume:** Study → Saved sessions → Resume → restore exact plan/item/block → Focus.
4. **Today action:** Home → Today mission / Study → Today Engine → next action → Focus.
5. **Personalization:** Profile → theme/language/goal/sound/reminders → setting persists after rebuild/restart.

## Audit rule

A feature is considered **shipped** only when all four are true:

- implemented in code;
- reachable from a user-facing screen;
- the main action completes without a dead-end;
- regression coverage exists for the integration-critical behavior.
