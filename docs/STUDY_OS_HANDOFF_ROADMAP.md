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
| Study History timeline | DONE | Android HistoryScreen groups the latest 30 days of Room session logs by local calendar day; Progress and Home navigation can open it; HistoryTimelineTest covers empty, grouped, newest-first, and invalid-duration cases. |
| XP / levels | DONE | LocalStore + Gamification; XP is 2 per completed minute and level is based on 250 XP. |
| Streak | DONE | Once-per-day streak update in LocalStore; analytics also calculates current/best streak windows. |
| Achievements | DONE | Five achievement rules are implemented and shown through progress experience. |
| Daily goal | DONE | Persisted goal, Home/Progress/Profile usage, tests. |
| Custom subjects | DONE | Add/remove and duplicate prevention are integrated into planning. |
| Themes | DONE | Supported theme keys are centralized and legacy theme values are normalized during startup. |
| English / বাংলা | DONE | Persisted language and user-facing Home/navigation/Profile support. |
| Sound effects | DONE | Setting and interaction integration with tests. |
| Study reminders | PARTIAL | Policy/coordinator/scheduler and unit tests exist; physical Android delivery still needs device verification. |
| Update gate | PARTIAL | Mandatory/optional policy and offline/cache fallback are implemented; tests now cover minimum-version decisions, cached policy fallback, malformed cache and unsafe cached release URLs. Real signed APK upgrade flow still needs release-device verification. |
| Android CI / APK | DONE | CI builds debug + release APK and uploads a combined artifact. Signed production release is handled separately by release workflow. |
| Release / signed APK upgrade | NEEDS REVIEW | Same package ID/signing key + higher versionCode upgrade path must be verified on an actual device. |
| Legacy ProgressScreen | NEEDS REVIEW | File remains in codebase, but shipped navigation uses ProgressDashboard. Do not expose duplicate UI unless it contains unique functionality. |
| Saved-session archive ownership | NEEDS REVIEW | Audit confirms two archive entry points remain: LocalStore.savePlan() archives outgoing plans, while Focus/Break lifecycle paths update snapshots through StudySessionStore. Persistence format, canonical fingerprint, mode preservation, position and progress are covered by regression tests. No production correctness change was justified yet; consolidate ownership only with a targeted refactor later. |

## 3. Current architecture

The active repository is the native Android/Kotlin implementation (not the historical Flutter layout referenced in earlier handoff text).

- Android entry/application: `app/src/main/kotlin/com/aistudio/studyos/MainActivity.kt` and `StudyApplication.kt`.
- Local persistence: Room entities, DAOs and `StudyDatabase.kt` under `data/local`.
- Preferences: `ThemePreferences.kt`.
- Repositories: `data/repository`, including Firebase account/progress sync, Today recommendation and progress analytics calculators.
- UI and navigation: Android app source under `app/src/main/kotlin/com/aistudio/studyos`.
- Regression tests: `app/src/test/java/com/aistudio/studyos`.

Use the current source tree as authoritative when locating screens, models and tests; do not reintroduce old `lib/` paths.

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
- Archive regression test setup/expectation errors → corrected; CI previously passed the canonical archive regression.
- Update-gate test coverage was expanded for minimum supported version decisions, offline/cache fallback, malformed cached policy, and unsafe cached release URLs.

### Completion-idempotency audit result

The current Focus → Break → Completion flow was audited after the CI baseline. No new production duplicate-award bug was found. The important duplicate-trigger paths are guarded by `transitioning` / `advancing`, timer cancellation, persisted timer clearing, and the Break-side delta calculation before adding completion. Existing regression coverage verifies that an expired Focus timer awards one block, that repeated lifecycle resume does not duplicate navigation, and that Break Continue does not double-count a block already persisted by Focus. Therefore no production completion fix was made in this audit.

### Still open / needs verification

- **Archive ownership is duplicated:** LocalStore archives during `savePlan`, while Focus/Break lifecycle code also calls StudySessionStore.archiveCurrentPlan. The persistence format is aligned and regression coverage is in place, but one service should eventually become authoritative without breaking planner behavior.
- **History timeline is read-only:** HistoryScreen shows the latest 30 days grouped by local day. Editing/deleting session history is intentionally not exposed.
- **Reminder delivery needs real Android-device verification.** Unit tests cannot prove OS delivery after reboot/background restrictions.
- **Signed APK update needs real-device verification.** Confirm same package ID/signing key and higher versionCode upgrade from an installed previous release.
- **Localization coverage should be expanded beyond Home/navigation/Profile; deeper screens still contain English UI strings.**
- **Release automation:** verify signed release workflow and artifact/install path separately from normal CI.

## 7. Test / CI status

Repository CI workflow: `.github/workflows/android.yml` (native Android/Kotlin).

Latest previously observed GitHub Actions run **#755** (run ID `37098527484`) for head `05fbb1caad156c032708034ce20c397b93f7c054` completed with conclusion **success**. The legacy-import policy work below is newer and its CI result is not yet verified.

[Latest verified CI run](https://github.com/mahirlabib478-del/new-app/actions/runs/37098527484)

## 8. Roadmap by priority

### P0 — correctness / release safety

1. ~~Verify CI for the current baseline: Analyze + Test + Android build/artifact.~~ **DONE — CI #367 green for the prior verified head.**
2. ~~If CI fails, fix only the real failure and add a regression test when it is a product bug.~~ **DONE for the previous CI cycle; no remaining failure was observed there.**
3. ~~Audit completion idempotency across Focus → Break → Completion to ensure one user action cannot award duplicate minutes/XP/history.~~ **DONE — no new production bug found; existing regression coverage is sufficient.**
4. ~~Audit LocalStore + StudySessionStore archive interactions with explicit tests for mode, progress, position, deduplication and completed-plan removal.~~ **DONE as an audit: current dual entry points are understood and covered; no production change was justified. Future ownership consolidation remains a refactor task.**
5. ~~Harden update-gate tests for malformed policy, offline/cache fallback, optional update dismissal and mandatory update behavior.~~ **PARTIAL/DONE in code coverage: new service-level tests cover minimum-version decisions, valid cached fallback, malformed cache, and unsafe cached URLs; widget tests already cover optional dismissal and mandatory blocking. New CI still needs observation.**

### P1 — core intelligence

6. Improve TodayEngine recommendation quality while preserving budget constraints and item-level authority.
7. ~~Add explicit history/timeline UX using existing Room session logs.~~ **DONE — read-only 30-day HistoryScreen, reachable from Home and Progress, with grouping and ordering regression tests.**
8. ~~Expand progress analytics with useful plan-vs-actual and consistency views.~~ **DONE — plan-vs-actual, weekly consistency, current-day boundaries, overflow handling and regression tests are integrated; CI #753 passed.**

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

**Next task: design and test account-scoped local data isolation without losing existing offline progress.**

Current evidence: Room uses one shared `study_os_database` for all signed-in Firebase accounts. `FirebaseProgressSyncRepository` pauses automatic upload when local progress ownership is ambiguous and refuses automatic merge when both local and cloud data exist. This protects against accidental cross-account transfer, but it does not yet provide separate local datasets per account.

Implementation sequence:
1. Inventory every account-sensitive local table, preference, active timer/session snapshot and read/write entry point.
2. Define explicit ownership behavior for signed-out, first-sign-in, account-switch and same-account return flows.
3. Design a preservation-first migration (backup/rollback strategy, schema versioning, and deterministic legacy-data ownership); never silently assign ambiguous legacy records to the newly signed-in UID.
4. Add tests for account switching, empty/local/cloud combinations, interrupted migration and rollback before changing production storage.
5. Implement the smallest safe stage, run CI and observe the exact head result.
6. Update this handoff and feature coverage after verified behavior.

Do not clear or migrate shared local data without a tested preservation strategy.

## 9.1 Account-isolation audit — first stage

Source audit confirms these ownership boundaries:

- Room's `study_os_database` is one process-wide database; plans, exams, session logs and profile rows have no Firebase UID owner column.
- `ThemePreferences` uses one device-wide SharedPreferences file. It stores presentation preferences (theme/wallpaper/audio) alongside cached recent sessions and gameplay/perk state (streak shield, XP booster, spin-wheel counters and unlock timestamps).
- `FirebaseAccountRepository` signs in/out without changing local progress ownership.
- `FirebaseProgressSyncRepository` intentionally pauses upload/merge when local ownership is ambiguous; this guard must remain until scoped storage is implemented.

Safe migration decision:

1. Treat existing Room rows and account-sensitive preference keys as **legacy device-local data with unknown account ownership**. Never assign them to whichever Firebase user signs in first.
2. Keep appearance preferences device-wide unless a later product decision explicitly makes them account-scoped.
3. Before scoped storage is enabled, create a tested export/backup and rollback path for the legacy dataset.
4. On sign-in, do not copy legacy progress into an account namespace automatically. Require an explicit user choice after showing the destination account and explaining the one-time import; retain the source until verification succeeds.
5. On account switch, each UID must open only its own scoped dataset; sign-out must not delete either scoped or legacy data.
6. Add migration state/version markers so interruption resumes safely and repeated migration is idempotent.

This stage was an audit/design checkpoint only; no production storage or user data was changed.

## 9.2 Legacy import policy guard — implementation stage

Added pure Kotlin `LegacyProgressImportPolicy` and unit tests. The policy returns explicit outcomes for no legacy data, signed-out state, existing target data, missing user confirmation, and an allowed import. It does not perform a copy or migration; callers must still implement transactional backup, verification, idempotency and rollback before any data movement. CI run #758 passed for the preceding legacy-import policy/docs head; later account-isolation changes are awaiting CI.

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


## 9.3 Account dataset routing guard — implementation stage

Added pure Kotlin `AccountDataIsolationPolicy` and unit tests for signed-out access, first sign-in with legacy data, same-account return, switching to an existing account, switching to an empty account, and first sign-in without legacy data. The policy chooses a dataset visibility outcome only; it does not change Room, preferences, authentication, or stored user data. An empty account namespace must not fall back to another UID or ambiguous legacy data. This is a policy/test checkpoint, not shipped account-scoped persistence; CI must be observed for the new commits.
