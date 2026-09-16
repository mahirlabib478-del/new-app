# Study OS — Developer / AI Handoff Roadmap

> Source-of-truth handoff for continuing development of Study OS. Update this document whenever an important feature, architecture decision, bug fix, or release decision changes.

## 1. Product vision

**Study OS** is a professional, offline-first study companion. Its core loop is:

**Plan → Focus → Break → Complete → Track → Improve**

The app is intentionally centered on studying rather than generic productivity. A user should be able to create a realistic study plan, follow focused blocks, pause/leave the app safely, resume exact progress, and understand progress through analytics and lightweight gamification.

Primary study modes:

- Regular Study — subject/topic planning for normal study days.
- Exam Preparation — priority-based exam planning.
- Next Day Exam — high-impact revision for an imminent exam.

Design goals:

- Offline-first local persistence.
- Budget-safe study allocation: never allocate more study minutes than the available plan budget.
- Item-level progress as the authoritative progress model.
- Lifecycle-safe focus/break timers.
- No duplicate completion, progress, or saved-session records.
- Personalization through themes, language, daily goal, and sound effects.
- Updates must never lock a user out because the network is unavailable.

## 2. Feature status matrix

Status meanings: **DONE** = implemented, reachable, main action works, and regression coverage exists. **PARTIAL** = usable but has a verification/coverage limitation. **TODO** = not yet implemented as a complete user-facing feature. **BUG** = known correctness problem. **NEEDS REVIEW** = implementation exists but requires a deliberate audit/verification before being treated as fully shipped.

| Feature | Status | Evidence / notes |
|---|---|---|
| Home / Today overview | DONE | Attractive Home reads TodayEngine and exposes the main daily action. |
| Today Engine | DONE | Dedicated screen is exposed from Study tab; navigation regression exists. |
| Regular Study planning | DONE | Setup + planner + allocation tests. |
| Exam Preparation | DONE | Exam planner and tests. |
| Next Day Exam | DONE | Exam planner and tests. |
| Focus Mode | DONE | FocusScreen, lifecycle persistence, item-level completion, tests. |
| Break flow | DONE | BreakScreen integrated with Focus completion and tests. |
| Saved Sessions | DONE | StudySessionStore + SavedSessionsScreen + store tests. |
| Exact resume position | DONE | Item/block position restored; canonical plan identity is centralized in StudySessionStore and covered by tests. |
| Progress Dashboard | DONE | ProgressDashboard + ProgressAnalytics integration and tests. |
| Daily history persistence | PARTIAL | LocalStore stores daily totals and ProgressAnalytics consumes them; there is no dedicated history browser/timeline UI yet. |
| XP / levels | DONE | LocalStore + Gamification; XP is 2 per completed minute and level is based on 250 XP. |
| Streak | DONE | Once-per-day streak update in LocalStore; analytics also calculates current/best streak windows. |
| Achievements | DONE | Five achievement rules are implemented and shown through progress experience. |
| Daily goal | DONE | Persisted goal, Home/Progress/Profile usage, tests. |
| Custom subjects | DONE | Add/remove and duplicate prevention are integrated into planning. |
| Themes | DONE | Midnight/Ocean/Forest/Sunrise persisted through Profile. |
| English / বাংলা | DONE | Persisted language and user-facing Home/navigation/Profile support. |
| Sound effects | DONE | Setting and interaction integration with tests. |
| Study reminders | PARTIAL | Policy/coordinator/scheduler and unit tests exist; physical Android delivery still needs device verification. |
| Update gate | PARTIAL | Mandatory/optional policy and offline/cache fallback are implemented; real signed APK upgrade flow still needs release-device verification. |
| Android CI / APK | DONE | CI builds debug + release APK and uploads a combined artifact. Signed production release is handled separately by release workflow. |
| Release / signed APK upgrade | NEEDS REVIEW | Same package ID/signing key + higher versionCode upgrade path must be verified on an actual device. |
| Legacy ProgressScreen | NEEDS REVIEW | File remains in codebase, but shipped navigation uses ProgressDashboard. Do not expose duplicate UI unless it contains unique functionality. |
| Saved-session archive ownership | NEEDS REVIEW | Two callers can archive to the same persistence key, but LocalStore was hardened to use the same canonical record shape/fingerprint. Continue monitoring before deeper refactor. |

## 3. Current architecture

### Entry / shell

- `lib/main.dart` — app shell, theme/language state, navigation, Home/Study/Progress/Profile tabs, planner entry points, Focus launch, Saved Sessions resume callback, startup reminder sync.
- `lib/widgets/attractive_home.dart` — primary Home / daily mission UI.
- `lib/widgets/update_gate.dart` — startup update UX; optional updates are non-blocking.

### Core models / persistence

- `lib/models/study_models.dart` — StudyPlan / StudyItem data model.
- `lib/services/local_store.dart` — SharedPreferences persistence, plan progress, item progress, position, timers, daily history, XP, streak, settings, and plan replacement archive.
- `lib/services/study_session_store.dart` — canonical SavedStudySession model, session list, archive/restore/delete, canonical plan identity.

### Decision / analytics engines

- `lib/services/today_engine.dart` — derives the current daily snapshot: plan, item-level completion, remaining work, next unfinished item, block position, daily goal, recommendation.
- `lib/services/progress_analytics.dart` — daily history and plan-progress analytics.
- `lib/services/gamification.dart` — achievement definitions and XP-to-next-level helper.

### Study flow

- `lib/screens/setup_screen.dart` — Regular Study setup.
- `lib/screens/exam_planner_screen.dart` — Exam Preparation / Next Day Exam planning.
- `lib/screens/focus_flow.dart` — FocusScreen, BreakScreen and completion flow; lifecycle-safe timers and completion persistence.
- `lib/screens/saved_sessions_screen.dart` — saved session list, delete, restore/resume.
- `lib/screens/today_engine_screen.dart` — Today Engine UI.

### Progress / personalization / system

- `lib/screens/progress_dashboard.dart` — shipped progress UI.
- `lib/screens/progress_screen.dart` — older/superseded progress UI.
- `lib/screens/profile_screen.dart` — theme, language, goal, sound, reminders.
- `lib/services/notification_service.dart` — OS notification scheduling.
- `lib/services/reminder_coordinator.dart` / `reminder_policy.dart` / `reminder_settings.dart` — reminder decisions and persistence.
- `lib/services/update_service.dart` — remote update policy, cache fallback, safe URL validation, version comparison.

## 4. End-to-end data flow

### Plan → Today

1. Setup or Exam Planner creates a `StudyPlan`.
2. `LocalStore.savePlan()` persists the plan and resets plan-specific progress/timers/position.
3. The outgoing unfinished plan is archived before replacement.
4. `TodayEngine.build()` reads the active plan and item-level progress.
5. Item-level progress is authoritative whenever present; legacy aggregate progress is supported for older data.

### Today → Home

`AttractiveHome` consumes a `TodaySnapshot` and presents daily goal, remaining work, next item, recommendation, XP/streak and the primary study action.

### Home / Study → Focus

`main.dart` launches `FocusScreen`. If the supplied plan is the stored active plan, canonical `StudySessionStore.samePlan()` allows TodayEngine's current item/block position to be reused; a new plan starts from position zero.

### Focus → Break

Focus timer runs one block. On block completion or early finish, `FocusScreen` records item-level completion, clears the focus timer, archives the latest unfinished snapshot, and navigates to `BreakScreen`.

### Break → Focus / Complete

Break state is persisted across lifecycle events. The next action returns to Focus at the appropriate item/block. When all allocated item minutes are complete, active saved-session state is removed and the completion experience is shown.

### Completion → Progress / History / XP / Streak

`LocalStore._recordCompletion()` updates:

- item completion;
- plan completed minutes;
- lifetime completed minutes;
- daily study history;
- XP (`minutes * 2`);
- session count;
- once-per-day streak state.

`ProgressAnalytics` consumes daily history and active-plan progress. `Gamification` derives achievements from persisted totals.

## 5. Important technical decisions

1. **Item-level progress is authoritative.** Aggregate `plan_completed_minutes` remains for compatibility with legacy data, but TodayEngine and ProgressAnalytics prefer item-level totals when available.
2. **Study allocation is budget constrained.** Completion writes clamp against plan and item remaining budgets.
3. **Canonical plan identity belongs in StudySessionStore.** UI code must not compare `toJson().toString()` ad hoc. Use `StudySessionStore.samePlan()`.
4. **Saved-session records use one canonical shape.** LocalStore's archive path no longer writes the legacy `sourcePlan` field and deduplicates by canonical plan JSON, matching StudySessionStore behavior.
5. **Outgoing study mode must be captured before async plan replacement.** Otherwise an archived Regular Study plan can accidentally be labeled with the incoming Exam mode.
6. **Lifecycle persistence is required.** Focus/break timers store remaining time and deadline so background/foreground transitions do not silently lose a session.
7. **Completion cleanup must be awaited.** Timer state, position, plan progress/session cleanup must finish before moving to completion UI.
8. **Update failures are non-blocking unless policy explicitly says the installed version is below the minimum supported version.** Network failure falls back to cached policy or no update.
9. **Release APK upgrades require the same application/package ID and signing key plus a higher Android versionCode.** CI debug/release builds alone do not prove upgrade compatibility.

## 6. Known bugs / technical issues

### Resolved recently

- Today Engine was implemented but not reachable from the Study tab → fixed and covered by `test/today_engine_navigation_test.dart`.
- Saved Sessions callback type mismatch in `main.dart` → fixed.
- Home `_StatCard` syntax error → fixed.
- Study Hub / `_Mode` widget syntax errors → fixed.
- Saved-session resume previously reset to the first item/block → fixed using stored position + canonical plan identity.
- Duplicate plan identity comparison in UI → replaced with `StudySessionStore.samePlan()`.
- LocalStore archive could write a legacy `sourcePlan` record alongside canonical session records → hardened to canonical shape and regression-tested.
- Archive regression test setup/expectation errors → corrected; CI now passes the canonical archive regression.

### Completion-idempotency audit result

The current Focus → Break → Completion flow was audited after the CI baseline. No new production duplicate-award bug was found. The important duplicate-trigger paths are guarded by `transitioning` / `advancing`, timer cancellation, persisted timer clearing, and the Break-side delta calculation before adding completion. Existing regression coverage verifies that an expired Focus timer awards one block, that repeated lifecycle resume does not duplicate navigation, and that Break Continue does not double-count a block already persisted by Focus. Therefore no production completion fix was made in this audit.

### Still open / needs verification

- **Archive ownership is duplicated:** LocalStore can archive during `savePlan`, while Focus/Break lifecycle code also calls StudySessionStore.archiveCurrentPlan. The persistence format is now aligned, but a future refactor should make one service authoritative without breaking planner behavior.
- **History UI is incomplete:** daily history exists and feeds analytics, but there is no dedicated user-facing history screen/timeline.
- **Reminder delivery needs real Android-device verification.** Unit tests cannot prove OS delivery after reboot/background restrictions.
- **Signed APK update needs real-device verification.** Confirm same package ID/signing key and higher versionCode upgrade from an installed previous release.
- **Localization coverage should be expanded beyond Home/navigation/Profile; deeper screens still contain English UI strings.**
- **Release automation:** verify signed release workflow and artifact/install path separately from normal CI.

## 7. Test / CI status

Repository CI workflow: `.github/workflows/flutter.yml`.

It runs on pushes and pull requests to `main` and performs:

1. `flutter pub get`
2. `flutter analyze`
3. `flutter test`
4. Android bootstrap
5. notification manifest/desugaring checks
6. debug APK build
7. release APK build
8. combined APK artifact upload

### Current verified CI

CI run **#367** (`35158080099`) for head `125138eeafb38f3c8125cfa210c3d6905627f8d9` is **fully green**. The `test` job completed successfully with both Analyze and Test passing. The `android-build` job also completed successfully, including notification manifest verification, dependency installation, debug APK build, release APK build, APK packaging and artifact upload.

This run specifically validates the corrected saved-session archive regression expectation. The test suite and Android build therefore provide the current verified baseline for continuation.

## 8. Roadmap by priority

### P0 — correctness / release safety

1. ~~Verify CI for the current head: Analyze + Test + Android build/artifact.~~ **DONE — CI #367 green.**
2. ~~If CI fails, fix only the real failure and add a regression test when it is a product bug.~~ **DONE for the current CI cycle; no remaining failure.**
3. ~~Audit completion idempotency across Focus → Break → Completion to ensure one user action cannot award duplicate minutes/XP/history.~~ **DONE — no new production bug found; existing regression coverage is sufficient.**
4. Audit LocalStore + StudySessionStore archive interactions with explicit tests for mode, progress, position, deduplication and completed-plan removal.
5. Verify update gate behavior against malformed, offline, cached, optional and mandatory policies.

### P1 — core intelligence

6. Improve TodayEngine recommendation quality while preserving budget constraints and item-level authority.
7. Add explicit history/timeline UX using existing daily history data.
8. Expand progress analytics with useful plan-vs-actual and consistency views.

### P2 — motivation / reminders

9. Expand gamification only where it supports study behavior; keep rules deterministic and locally persisted.
10. Verify notification delivery on physical Android devices, including background and reboot behavior.

### P3 — UX polish

11. Complete localization coverage for deeper screens.
12. Polish accessibility, empty states, error states, loading states and small-screen layouts.
13. Remove or clearly isolate superseded UI such as legacy ProgressScreen only after confirming no unique functionality is lost.

### P4 — release

14. Verify signed APK/AAB workflow.
15. Install previous release → install new signed release over it → verify data preservation and version gate behavior.
16. Document release procedure and artifact naming.

## 9. Exact next development task

**Next task: P0 — audit saved-session archive ownership, then harden update-gate tests.**

Concrete implementation sequence:

1. Inspect `LocalStore.savePlan()` / `_archiveCurrentPlan()` and all `StudySessionStore.archiveCurrentPlan()` callers in Focus/Break.
2. Add or refine regression coverage for mode preservation, progress/position preservation, canonical deduplication and completed-plan removal across the two archive entry points.
3. Make the smallest production change necessary only if the ownership audit finds a real correctness issue.
4. Inspect `update_service.dart` + `update_gate.dart` tests for malformed policy, offline/cache fallback, optional update dismissal and mandatory update behavior.
5. Fix only real update-gate correctness issues; keep network failure non-blocking.
6. Update this handoff and feature coverage documentation with the findings.
7. Re-run and verify CI before taking another feature.

## 10. Definition of done for future work

Before marking a feature **DONE**:

- code exists;
- user can reach it from the app;
- the main action completes without a dead-end;
- important state survives the intended lifecycle/restart scenario;
- regression coverage exists for integration-critical behavior;
- `flutter analyze` passes;
- `flutter test` passes;
- Android build passes when the change affects Android/runtime integration;
- CI result is actually observed before claiming green;
- this document and `docs/STUDY_OS_FEATURE_COVERAGE.md` are updated when the architecture/status changes.
