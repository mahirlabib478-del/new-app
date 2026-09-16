import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

import 'reminder_scheduler.dart';

class NotificationService implements ReminderScheduler {
  NotificationService({FlutterLocalNotificationsPlugin? plugin})
      : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  final FlutterLocalNotificationsPlugin _plugin;
  bool _initialized = false;

  static const _channelId = 'study_os_reminders';
  static const _channelName = 'Study reminders';
  static const _channelDescription = 'Study, break and plan reminders.';

  @override
  Future<void> initialize() => _initialize();

  Future<void> _initialize({String? timeZoneName}) async {
    if (_initialized) return;

    tz.initializeTimeZones();
    final resolvedTimeZone = timeZoneName ?? await _deviceTimeZone();
    if (resolvedTimeZone != null && resolvedTimeZone.isNotEmpty) {
      try {
        tz.setLocalLocation(tz.getLocation(resolvedTimeZone));
      } on Exception {
        // Keep the timezone package default when the device timezone is unknown.
      }
    }

    const android = AndroidInitializationSettings('ic_launcher');
    const darwin = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    const settings = InitializationSettings(android: android, iOS: darwin, macOS: darwin);
    await _plugin.initialize(settings);
    _initialized = true;
  }

  Future<String?> _deviceTimeZone() async {
    try {
      return await FlutterTimezone.getLocalTimezone();
    } on Exception {
      return null;
    }
  }

  @override
  Future<bool?> requestPermissions() async {
    await _initialize();
    final android = _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) return android.requestNotificationsPermission();

    final ios = _plugin.resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>();
    if (ios != null) return ios.requestPermissions(alert: true, badge: true, sound: true);

    final macos = _plugin.resolvePlatformSpecificImplementation<MacOSFlutterLocalNotificationsPlugin>();
    if (macos != null) return macos.requestPermissions(alert: true, badge: true, sound: true);

    return null;
  }

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {
    await _initialize();
    final now = tz.TZDateTime.now(tz.local);
    final scheduled = nextDailyOccurrence(now, hour, minute);

    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDescription,
      ),
      iOS: DarwinNotificationDetails(),
      macOS: DarwinNotificationDetails(),
    );

    await _plugin.zonedSchedule(
      id,
      title,
      body,
      scheduled,
      details,
      uiLocalNotificationDateInterpretation: UILocalNotificationDateInterpretation.absoluteTime,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }

  @override
  Future<void> showNow({required int id, required String title, required String body}) async {
    await _initialize();
    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDescription,
      ),
      iOS: DarwinNotificationDetails(),
      macOS: DarwinNotificationDetails(),
    );
    await _plugin.show(id, title, body, details);
  }

  @override
  Future<void> cancel(int id) => _plugin.cancel(id);

  Future<void> cancelAll() => _plugin.cancelAll();

  static tz.TZDateTime nextDailyOccurrence(
    tz.TZDateTime now,
    int hour,
    int minute,
  ) {
    final safeHour = hour.clamp(0, 23).toInt();
    final safeMinute = minute.clamp(0, 59).toInt();
    var scheduled = tz.TZDateTime(
      now.location,
      now.year,
      now.month,
      now.day,
      safeHour,
      safeMinute,
    );

    if (!scheduled.isAfter(now)) {
      scheduled = scheduled.add(const Duration(days: 1));
    }
    return scheduled;
  }
}
