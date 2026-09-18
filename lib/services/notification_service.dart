import 'package:flutter/services.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

import 'reminder_scheduler.dart';

class NotificationService
    implements ReminderScheduler, ReminderSchedulerNotificationAware, ReminderSchedulerTimeZoneAware {
  NotificationService({FlutterLocalNotificationsPlugin? plugin})
      : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  final FlutterLocalNotificationsPlugin _plugin;
  bool _initialized = false;
  Future<void>? _initialization;

  static const _channelId = 'study_os_reminders';
  static const _channelName = 'Study reminders';
  static const _channelDescription = 'Study, break and plan reminders.';
  static const _nativeNotificationChannel = MethodChannel('study_os/notifications');

  @override
  Future<void> initialize() => _initialize();

  Future<void> _initialize({String? timeZoneName}) {
    if (_initialized) return Future<void>.value();
    final inFlight = _initialization;
    if (inFlight != null) return inFlight;

    final initialization = _performInitialization(timeZoneName: timeZoneName);
    _initialization = initialization;
    return initialization.whenComplete(() {
      if (identical(_initialization, initialization)) {
        _initialization = null;
      }
    });
  }

  Future<void> _performInitialization({String? timeZoneName}) async {
    if (_initialized) return;

    tz.initializeTimeZones();
    await _refreshTimeZone(timeZoneName: timeZoneName);

    const android = AndroidInitializationSettings('ic_notification');
    const darwin = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    const settings = InitializationSettings(
      android: android,
      iOS: darwin,
      macOS: darwin,
    );
    await _plugin.initialize(settings);
    _initialized = true;
  }

  @override
  Future<void> refreshTimeZone() async {
    await _initialize();
    await _refreshTimeZone();
  }

  @override
  String? get timeZoneFingerprint => tz.local.name;

  Future<void> _refreshTimeZone({String? timeZoneName}) async {
    final resolvedTimeZone = timeZoneName ?? await _deviceTimeZone();
    if (resolvedTimeZone != null && resolvedTimeZone.isNotEmpty) {
      try {
        tz.setLocalLocation(tz.getLocation(resolvedTimeZone));
      } on Exception {
        // Keep the timezone package default when the device timezone is unknown.
      }
    }
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
    final android =
        _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) {
      // Request through the app Activity as a fallback as well. This keeps the
      // runtime permission flow independent from plugin-side permission timing.
      try {
        await _nativeNotificationChannel.invokeMethod<void>('requestPermission');
      } on PlatformException {
        // Fall back to the plugin API below.
      }
      return android.requestNotificationsPermission();
    }

    final ios =
        _plugin.resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>();
    if (ios != null) {
      return ios.requestPermissions(alert: true, badge: true, sound: true);
    }

    final macos =
        _plugin.resolvePlatformSpecificImplementation<MacOSFlutterLocalNotificationsPlugin>();
    if (macos != null) {
      return macos.requestPermissions(alert: true, badge: true, sound: true);
    }

    return null;
  }

  @override
  Future<bool?> areNotificationsEnabled() async {
    await _initialize();
    final android =
        _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) {
      return android.areNotificationsEnabled();
    }

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
    final enabled = await areNotificationsEnabled();
    if (enabled == false) {
      throw StateError('Notifications are disabled');
    }

    final now = tz.TZDateTime.now(tz.local);
    final scheduled = nextDailyOccurrence(now, hour, minute);

    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDescription,
        icon: 'ic_notification',
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
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }

  @override
  Future<void> showNow({
    required int id,
    required String title,
    required String body,
  }) async {
    await _initialize();
    final enabled = await areNotificationsEnabled();
    if (enabled == false) {
      throw StateError('Notifications are disabled');
    }

    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDescription,
        icon: 'ic_notification',
      ),
      iOS: DarwinNotificationDetails(),
      macOS: DarwinNotificationDetails(),
    );
    await _plugin.show(id, title, body, details);
  }

  @override
  Future<void> cancel(int id) async {
    await _initialize();
    await _plugin.cancel(id);
  }

  Future<void> cancelAll() async {
    await _initialize();
    await _plugin.cancelAll();
  }

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
      scheduled = tz.TZDateTime(
        now.location,
        now.year,
        now.month,
        now.day + 1,
        safeHour,
        safeMinute,
      );
    }
    return scheduled;
  }
}
