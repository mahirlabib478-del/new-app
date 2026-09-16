import 'dart:convert';

import 'local_store.dart';
import '../models/study_models.dart';

class SavedStudySession {
  const SavedStudySession({
    required this.id,
    required this.mode,
    required this.savedAt,
    required this.plan,
    required this.itemProgress,
    required this.planCompletedMinutes,
    required this.currentIndex,
    required this.currentBlockIndex,
  });

  final String id;
  final String mode;
  final DateTime savedAt;
  final StudyPlan plan;
  final Map<int, int> itemProgress;
  final int planCompletedMinutes;
  final int currentIndex;
  final int currentBlockIndex;

  int get completedMinutes => itemProgress.values.fold(0, (sum, value) => sum + value);
  int get remainingMinutes => (plan.allocatedMinutes - completedMinutes).clamp(0, 1440).toInt();

  Map<String, dynamic> toJson() => {
        'id': id,
        'mode': mode,
        'savedAt': savedAt.toIso8601String(),
        'plan': plan.toJson(),
        'itemProgress': itemProgress.map((key, value) => MapEntry(key.toString(), value)),
        'planCompletedMinutes': planCompletedMinutes,
        'currentIndex': currentIndex,
        'currentBlockIndex': currentBlockIndex,
      };

  factory SavedStudySession.fromJson(Map<String, dynamic> json) {
    final plan = StudyPlan.fromJson(Map<String, dynamic>.from(json['plan'] as Map));
    final rawProgress = json['itemProgress'];
    final progress = <int, int>{};
    if (rawProgress is Map) {
      for (final entry in rawProgress.entries) {
        final index = int.tryParse(entry.key.toString());
        final value = entry.value;
        if (index != null && index >= 0 && value is num) {
          progress[index] = value.toInt().clamp(0, 1440).toInt();
        }
      }
    }
    return SavedStudySession(
      id: json['id'] as String? ?? DateTime.now().microsecondsSinceEpoch.toString(),
      mode: json['mode'] as String? ?? 'Study',
      savedAt: DateTime.tryParse(json['savedAt'] as String? ?? '') ?? DateTime.now(),
      plan: plan,
      itemProgress: progress,
      planCompletedMinutes: (json['planCompletedMinutes'] as num? ?? 0).clamp(0, 1440).toInt(),
      currentIndex: (json['currentIndex'] as num? ?? 0).clamp(0, 100000).toInt(),
      currentBlockIndex: (json['currentBlockIndex'] as num? ?? 0).clamp(0, 100000).toInt(),
    );
  }
}

class StudySessionStore {
  StudySessionStore(this.store);

  final LocalStore store;
  static const _key = 'saved_study_sessions';

  List<SavedStudySession> get sessions {
    final raw = store.prefs.getString(_key);
    if (raw == null) return const [];
    try {
      final list = jsonDecode(raw) as List<dynamic>;
      final result = <SavedStudySession>[];
      for (final value in list) {
        if (value is! Map) continue;
        try {
          result.add(SavedStudySession.fromJson(Map<String, dynamic>.from(value)));
        } catch (_) {
          // Ignore malformed saved sessions instead of blocking the app.
        }
      }
      result.sort((a, b) => b.savedAt.compareTo(a.savedAt));
      return result;
    } catch (_) {
      return const [];
    }
  }

  Future<void> archiveCurrentPlan({String mode = 'Study'}) async {
    final plan = store.loadPlan();
    if (plan == null || plan.items.isEmpty) return;
    final completed = store.planCompletedMinutes.clamp(0, plan.allocatedMinutes).toInt();
    if (completed >= plan.allocatedMinutes) return;

    final session = SavedStudySession(
      id: '${DateTime.now().microsecondsSinceEpoch}',
      mode: mode,
      savedAt: DateTime.now(),
      plan: plan,
      itemProgress: store.itemCompletedMinutesMap,
      planCompletedMinutes: completed,
      currentIndex: store.currentPlanIndex,
      currentBlockIndex: store.currentBlockIndex,
    );
    final next = [...sessions.where((item) => item.id != session.id), session];
    await store.prefs.setString(_key, jsonEncode(next.map((item) => item.toJson()).toList()));
  }

  Future<void> delete(String id) async {
    final next = sessions.where((item) => item.id != id).toList();
    await store.prefs.setString(_key, jsonEncode(next.map((item) => item.toJson()).toList()));
  }

  Future<bool> restore(String id) async {
    final target = sessions.cast<SavedStudySession?>().firstWhere((item) => item?.id == id, orElse: () => null);
    if (target == null) return false;

    await store.prefs.setString('study_plan', jsonEncode(target.plan.toJson()));
    await store.prefs.setString('study_plan_date', _dateKey(DateTime.now()));
    await store.prefs.setInt('plan_completed_minutes', target.planCompletedMinutes);
    if (target.itemProgress.isEmpty) {
      await store.prefs.remove('item_completed_minutes');
    } else {
      await store.prefs.setString('item_completed_minutes', jsonEncode(target.itemProgress.map((key, value) => MapEntry(key.toString(), value))));
    }
    await store.prefs.setInt('current_plan_index', target.currentIndex);
    await store.prefs.setInt('current_block_index', target.currentBlockIndex);
    await store.clearFocusTimerState();
    await store.clearBreakTimerState();
    await delete(id);
    return true;
  }

  String _dateKey(DateTime date) => '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
