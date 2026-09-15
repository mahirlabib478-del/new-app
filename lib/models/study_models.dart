class StudyItem {
  StudyItem({required this.title, required this.minutes, this.topic = ''});
  final String title;
  final int minutes;
  final String topic;

  Map<String, dynamic> toJson() => {'title': title, 'minutes': minutes, 'topic': topic};

  factory StudyItem.fromJson(Map<String, dynamic> json) => StudyItem(
        title: json['title'] as String? ?? 'Untitled',
        minutes: (json['minutes'] as num?)?.toInt() ?? 25,
        topic: json['topic'] as String? ?? '',
      );
}

class StudyPlan {
  StudyPlan({required this.totalMinutes, required this.items});
  final int totalMinutes;
  final List<StudyItem> items;

  int get allocatedMinutes => items.fold(0, (sum, item) => sum + item.minutes);
  int get remainingMinutes => totalMinutes - allocatedMinutes;

  Map<String, dynamic> toJson() => {
        'totalMinutes': totalMinutes,
        'items': items.map((e) => e.toJson()).toList(),
      };

  factory StudyPlan.fromJson(Map<String, dynamic> json) => StudyPlan(
        totalMinutes: (json['totalMinutes'] as num?)?.toInt() ?? 0,
        items: (json['items'] as List<dynamic>? ?? const [])
            .whereType<Map>()
            .map((item) => StudyItem.fromJson(Map<String, dynamic>.from(item)))
            .toList(),
      );
}
