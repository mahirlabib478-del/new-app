import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

import 'reminder_scheduler.dart';

/// Single Android/iOS notification gateway.
///
/// The app never talks to flutter_local_notifications directly outside this
/// class. Initialization, timezone setup, permission requests, channel setup,
/// scheduling and immediate delivery all use the same plugin instance.
class NotificationService
    implements ReminderScheduler, ReminderSchedulerTimeZoneAware {
  NotificationService({FlutterLocalNotificationsPlugin? plugin})
      : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  final FlutterLocalNotificationsPlugin _plugin;

  static const channelId = 'study_os_reminders';
  static const channelName = 'Study reminders';
  static const channelDescription = 'Study, break and plan reminders.';

  Future<void>? _initializing;
  bool _initialized = false;

  @override
  Future<void> initialize() {
    if (_initialized) return Future<void>.value();

    final running = _initializing;
    if (running != null) return running;

    final future = _initializeInternal();
    _initializing = future;
    return future.whenComplete(() {
      if (identical(_initializing, future)) {
        _initializing = null;
      }
    });
  }

  Future<void> _initializeInternal() async {
    if (_initialized) return;

    tz.initializeTimeZones();
    await _setDeviceTimezone();

    const androidSettings = AndroidInitializationSettings('ic_notification');
    const darwinSettings = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );

    await _plugin.initialize(
      settings: const InitializationSettings(
        android: androidSettings,
        iOS: darwinSettings,
        macOS: darwinSettings,
      ),
    );

    final android = _android;
    if (android != null) {
      await android.createNotificationChannel(
        const AndroidNotificationChannel(
          channelId,
          channelName,
          description: channelDescription,
          importance: Importance.high,
          playSound: true,
        ),
      );
    }

    _initialized = true;
  }

  AndroidFlutterLocalNotificationsPlugin? get _android =>
      _plugin.resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>();

  @override
  Future<bool?> requestPermissions() async {
    await initialize();

    final android = _android;
    if (android != null) {
      return android.requestNotificationsPermission();
    }

    final ios = _plugin.resolvePlatformSpecificImplementation<
        IOSFlutterLocalNotificationsPlugin>();
    if (ios != null) {
      return ios.requestPermissions(alert: true, badge: true, sound: true);
    }

    final macos = _plugin.resolvePlatformSpecificImplementation<
        MacOSFlutterLocalNotificationsPlugin>();
    if (macos != null) {
      return macos.requestPermissions(alert: true, badge: true, sound: true);
    }

    return true;
  }

  @override
  Future<void> refreshTimeZone() async {
    await initialize();
    await _setDeviceTimezone();
  }

  @override
  String? get timeZoneFingerprint => tz.local.name;

  Future<void> _setDeviceTimezone() async {
    try {
      final name = await FlutterTimezone.getLocalTimezone();
      if (name.isNotEmpty) {
        tz.setLocalLocation(tz.getLocation(name));
      }
    } on Exception {
      // The timezone package already has a usable fallback location.
    }
  }

  @override
  Future<void> scheduleDailyReminder({
    required int id,
    required String title,
    required String body,
    required int hour,
    required int minute,
  }) async {
    await initialize();

    final now = tz.TZDateTime.now(tz.local);
    final scheduled = nextDailyOccurrence(now, hour, minute);

    await _plugin.zonedSchedule(
      id: id,
      title: title,
      body: body,
      scheduledDate: scheduled,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          channelId,
          channelName,
          channelDescription: channelDescription,
          importance: Importance.high,
          priority: Priority.high,
          playSound: true,
          icon: 'ic_notification',
        ),
        iOS: DarwinNotificationDetails(),
        macOS: DarwinNotificationDetails(),
      ),
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
    await initialize();

    await _plugin.show(
      id: id,
      title: title,
      body: body,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          channelId,
          channelName,
          channelDescription: channelDescription,
          importance: Importance.high,
          priority: Priority.high,
          playSound: true,
          icon: 'ic_notification',
        ),
        iOS: DarwinNotificationDetails(),
        macOS: DarwinNotificationDetails(),
      ),
    );
  }

  @override
  Future<void> cancel(int id) async {
    await initialize();
    await _plugin.cancel(id: id);
  }

  Future<void> cancelAll() async {
    await initialize();
    await _plugin.cancelAll();
  }

  static tz.TZDateTime nextDailyOccurrence(
    tz.TZDateTime now,
    int hour,
    int minute,
  ) {
    final safeHour = hour.clamp(0, 23).toInt();
    final safeMinute = minute.clamp(0, 59).toInt();

    var result = tz.TZDateTime(
      now.location,
      now.year,
      now.month,
      now.day,
      safeHour,
      safeMinute,
    );

    if (!result.isAfter(now)) {
      result = tz.TZDateTime(
        now.location,
        now.year,
        now.month,
        now.day + 1,
        safeHour,
        safeMinute,
      );
    }

    return result;
  }
}
