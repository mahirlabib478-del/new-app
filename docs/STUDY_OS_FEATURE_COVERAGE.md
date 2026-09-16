# Study OS — Feature Coverage

**Source of truth:** `docs/STUDY_OS_HANDOFF_ROADMAP.md` contains the full developer/AI handoff, architecture, data flow, known issues, CI policy and priority roadmap. This file remains the compact feature coverage checklist and should be updated alongside the handoff when feature status changes.

A feature is **shipped** only when it is implemented, user-reachable, its main action works without a dead-end, and integration-critical regression coverage exists.

## User-facing map

| Feature | Primary UI | Entry point | Core logic | Test coverage | Status |
|---|---|---|---|---|---|
| Home / Today overview | Attractive Home | Home tab | TodayEngine | navigation/home tests | DONE |
| Today Engine | Today Engine screen | Study → Today Engine | TodayEngine | engine + navigation regression | DONE |
| Regular Study | Setup + planner | Home/Study → Regular Study | planner + LocalStore | planner/setup tests | DONE |
| Exam Preparation | Exam Planner | Home/Study → Exam Preparation | Exam planner | exam tests | DONE |
| Next Day Exam | Exam Planner | Home/Study → Next Day Exam | Exam planner | exam tests | DONE |
| Focus Mode | FocusScreen | Start/Resume plan | Focus flow + LocalStore | focus tests | DONE |
| Break flow | BreakScreen | Focus completion | Focus flow | focus tests | DONE |
| Saved Sessions | SavedSessionsScreen | Study → Saved sessions | StudySessionStore | session tests | DONE |
| Resume exact position | FocusScreen | Saved session → Resume | TodayEngine + canonical plan identity | session/home resume tests | DONE |
| Progress dashboard | ProgressDashboard | Progress tab | ProgressAnalytics + Gamification | dashboard/analytics tests | DONE |
| Daily history persistence | Progress / analytics | Internal persistence | LocalStore | analytics tests | PARTIAL — no dedicated history timeline UI |
| XP / level / streak | Home + Progress | Home/Progress | Gamification + LocalStore | gamification tests | DONE |
| Achievements | Progress dashboard | Progress tab | Gamification | dashboard/gamification tests | DONE |
| Daily goal | Home + Progress + Profile | Profile / Progress | LocalStore | dashboard/profile tests | DONE |
| Custom subjects | Setup | Regular Study | Setup + planner allocation | setup custom subject tests | DONE |
| Themes | Profile | Profile tab | LocalStore + app theme | profile tests | DONE |
| English / বাংলা | Home/nav/Profile | Profile → Language | AppLanguage | language/profile tests | DONE — deeper-screen coverage still needs review |
| Sound effects | Profile + interactions | Profile | SoundEffects | sound tests | DONE |
| Study reminders | Profile | Profile → Reminders | ReminderCoordinator/Policy/Scheduler | reminder tests | PARTIAL — physical Android delivery not verified |
| Update gate | Startup | App launch | UpdateService + UpdateGate | update tests | PARTIAL — signed upgrade not device-verified |
| Android CI / APK | CI workflow | GitHub Actions | Flutter build workflow | CI | DONE — CI #367 verified |
| Signed release upgrade | Release workflow | Release | signing + versioning | manual/device verification | NEEDS REVIEW |
| Legacy ProgressScreen | Legacy UI | not in shipped nav | old progress implementation | existing tests | NEEDS REVIEW — keep out of navigation unless unique functionality is confirmed |
| Saved-session archive ownership | Internal | Focus/Break/save-plan paths | LocalStore + StudySessionStore | session regression | NEEDS REVIEW — format unified; ownership refactor remains |

## Fixed during recent audit

- Today Engine was implemented but not reachable from Study navigation; a dedicated Study entry and navigation regression were added.
- Saved Sessions resume now preserves the correct item/block position using canonical plan identity.
- Study mode archiving captures the outgoing mode before replacing the active plan.
- LocalStore saved-session archive now uses the same canonical record shape/fingerprint as StudySessionStore instead of the legacy `sourcePlan` field.
- Completion idempotency was audited across Focus → Break → Completion; existing guards and regression tests cover timer-expiry, lifecycle-resume and Break Continue duplicate-trigger paths, with no new production bug found.

## Important verification limits

- Unit tests validate reminder policy/coordinator behavior, but only a physical Android device can prove OS-level notification delivery.
- Normal CI release APK builds do not prove that an installed previous signed APK can be upgraded safely. That requires the same package ID/signing key and a higher versionCode.
- `ProgressScreen` exists as an older UI; shipped navigation uses `ProgressDashboard`.
- Daily history is persisted and analyzed but lacks a dedicated history/timeline screen.

## End-to-end journeys to keep green

1. **Regular Study:** Home/Study → Regular Study → choose subjects/time → allocate topics → Focus → Break → completion → Progress.
2. **Exam:** Home/Study → Exam Preparation or Next Day Exam → plan → Focus → completion → Progress.
3. **Resume:** Study → Saved sessions → Resume → restore exact plan/item/block → Focus.
4. **Today action:** Home → Today mission / Study → Today Engine → next action → Focus.
5. **Personalization:** Profile → theme/language/goal/sound/reminders → setting persists after rebuild/restart.

## CI policy

Never mark the current head **green** from code inspection alone. Confirm the GitHub Actions run and verify Analyze, Test, and Android build/artifact steps.

### Latest verified run

- CI **#367** / run `35158080099`
- Head: `125138eeafb38f3c8125cfa210c3d6905627f8d9`
- Analyze: PASS
- Test: PASS
- Android debug APK: PASS
- Android release APK: PASS
- APK artifact upload: PASS
