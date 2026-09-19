#!/usr/bin/env bash
set -euo pipefail

# Generate the Android host project from the installed Flutter SDK and apply
# Study OS-specific notification, sound, and launcher icon configuration.
flutter create --platforms=android --org com.mahirlabib --project-name study_os .

# flutter create adds its default counter-app widget_test.dart when the
# repository does not already contain one. Study OS has its own test suite,
# so remove only that generated default test before CI analysis/test runs.
# flutter create also generates a default analysis_options.yaml. The app does
# not keep that generated lint profile in source control; remove it so release
# analysis uses the same analyzer configuration as normal CI.
rm -f analysis_options.yaml
rm -f test/widget_test.dart

python3 - <<'PY'
from pathlib import Path

build = Path('android/app/build.gradle.kts')
text = build.read_text()

# Keep plugin resolution resilient when the Gradle Plugin Portal is temporarily
# missing a Kotlin artifact that is available from Maven Central.
settings = Path('android/settings.gradle.kts')
settings_text = settings.read_text()
if 'mavenCentral()' not in settings_text:
    marker = '    repositories {\n'
    if marker not in settings_text:
        raise SystemExit('Could not find pluginManagement repositories block')
    settings_text = settings_text.replace(marker, marker + '        mavenCentral()\n', 1)
    settings.write_text(settings_text)

text = text.replace('targetSdk = flutter.targetSdkVersion', 'targetSdk = 35', 1)

# flutter_local_notifications 22.x requires compileSdk 35+; use 36 on the current stable toolchain.
text = text.replace('compileSdk = flutter.compileSdkVersion', 'compileSdk = 36', 1)

if 'isCoreLibraryDesugaringEnabled = true' not in text:
    marker = '    defaultConfig {'
    if marker not in text:
        raise SystemExit('Could not find android defaultConfig block')
    text = text.replace(
        marker,
        '    compileOptions {\n'
        '        isCoreLibraryDesugaringEnabled = true\n'
        '        sourceCompatibility = JavaVersion.VERSION_17\n'
        '        targetCompatibility = JavaVersion.VERSION_17\n'
        '    }\n\n'
        + marker,
        1,
    )
    if 'targetSdk = 35' not in text:
        target_marker = '    defaultConfig {'
        text = text.replace(target_marker, target_marker + '\n        targetSdk = 35', 1)

if 'desugar_jdk_libs' not in text:
    text += '\n\ndependencies {\n'
    text += '    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")\n'
    text += '}\n'

build.write_text(text)

manifest = Path('android/app/src/main/AndroidManifest.xml')
text = manifest.read_text()

permissions = (
    '    <uses-permission android:name="android.permission.INTERNET" />\n'
    '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n'
    '    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />\n'
)
if 'android.permission.RECEIVE_BOOT_COMPLETED' not in text:
    insert_at = text.find('>', text.find('<manifest')) + 1
    text = text[:insert_at] + '\n' + permissions + text[insert_at:]
elif 'android.permission.POST_NOTIFICATIONS' not in text:
    anchor = '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n'
    if anchor not in text:
        raise SystemExit('Could not find notification permission anchor')
    text = text.replace(anchor, permissions, 1)

receiver = '''
        <receiver
            android:exported="false"
            android:name="com.dexterous.flutterlocalnotifications.ScheduledNotificationReceiver" />
        <receiver
            android:exported="false"
            android:name="com.dexterous.flutterlocalnotifications.ScheduledNotificationBootReceiver">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
                <action android:name="com.htc.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>
'''
if 'ScheduledNotificationReceiver' not in text:
    marker = '    </application>'
    if marker not in text:
        raise SystemExit('Could not find Android application block')
    text = text.replace(marker, receiver + marker, 1)

# Use the Study OS launcher icon on the generated Android host.
if 'android:icon="@mipmap/ic_launcher"' in text:
    text = text.replace(
        'android:icon="@mipmap/ic_launcher"',
        'android:icon="@drawable/study_os_logo"',
        1,
    )
elif 'android:icon="@drawable/study_os_logo"' not in text:
    application_marker = '<application'
    text = text.replace(
        application_marker,
        '<application android:icon="@drawable/study_os_logo"',
        1,
    )

manifest.write_text(text)

drawable = Path('android/app/src/main/res/drawable/study_os_logo.xml')
drawable.parent.mkdir(parents=True, exist_ok=True)
drawable.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#081A33" android:pathData="M8,0 L100,0 Q108,0 108,8 L108,100 Q108,108 100,108 L8,108 Q0,108 0,100 L0,8 Q0,0 8,0 Z" />
    <path android:fillColor="#168AAD" android:pathData="M18,57 A36,36 0,1 1,90 57 A36,36 0,1 1,18 57 Z" />
    <path android:fillColor="#081A33" android:pathData="M23,57 A31,31 0,1 1,85 57 A31,31 0,1 1,23 57 Z" />
    <path android:fillColor="#F5F7FA" android:pathData="M24,60 C34,58 44,60 54,68 L54,88 C44,80 34,78 24,81 Z" />
    <path android:fillColor="#E8EDF3" android:pathData="M54,68 C64,60 74,58 84,60 L84,81 C74,78 64,80 54,88 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M54,67 L54,89 L50,85 L50,68 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M50,41 L55,41 L55,55 L50,55 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M52,50 L72,34 L75,38 L55,55 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M52,50 L37,38 L34,42 L50,55 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M48,51 A6,6 0,1 1,60 51 A6,6 0,1 1,48 51 Z" />
    <path android:fillColor="#2A9D8F" android:pathData="M79,60 C78,49 84,38 96,34 C96,48 90,57 79,60 Z" />
    <path android:fillColor="#43D3A5" android:pathData="M79,64 C85,55 92,52 99,53 C96,62 89,66 79,68 Z" />
</vector>
''')
notification_icon = Path('android/app/src/main/res/drawable/ic_notification.xml')
notification_icon.parent.mkdir(parents=True, exist_ok=True)
keep = Path('android/app/src/main/res/raw/keep.xml')
keep.parent.mkdir(parents=True, exist_ok=True)
keep.write_text('''<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/*" />
''')

notification_icon.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M4,5 L10,4 L12,6 L14,4 L20,5 L20,18 C17,17 14,18 12,20 C10,18 7,17 4,18 Z" />
    <path android:fillColor="#081A33" android:pathData="M11,6 L12,7 L13,6 L13,16 L12,17 L11,16 Z" />
</vector>
''')


activity = Path('android/app/src/main/kotlin/com/mahirlabib/study_os/MainActivity.kt')
activity.parent.mkdir(parents=True, exist_ok=True)
activity.write_text('''package com.mahirlabib.study_os

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val soundChannel = "study_os/sound"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, soundChannel)
            .setMethodCallHandler { call, result ->
                if (call.method != "play") {
                    result.notImplemented()
                    return@setMethodCallHandler
                }

                val effect = call.arguments as? String ?: "tap"
                val tone = if (effect == "success") {
                    ToneGenerator.TONE_PROP_ACK
                } else {
                    ToneGenerator.TONE_PROP_BEEP
                }

                try {
                    val generator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                    val duration = if (effect == "success") 250 else 80
                    generator.startTone(tone, duration)
                    Handler(Looper.getMainLooper()).postDelayed(
                        { generator.release() },
                        (duration + 100).toLong(),
                    )
                    result.success(null)
                } catch (error: Exception) {
                    result.error("SOUND_ERROR", error.message, null)
                }
            }
    }
}
''')


PY

echo "Android platform prepared with Study OS launcher icon, notification scheduling, and native study sound support."
