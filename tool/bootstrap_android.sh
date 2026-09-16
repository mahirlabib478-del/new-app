#!/usr/bin/env bash
set -euo pipefail

# Generate the Android host project from the installed Flutter SDK and apply
# Study OS-specific notification build/runtime configuration.
flutter create --platforms=android --org com.mahirlabib --project-name study_os .

python3 - <<'PY'
from pathlib import Path

build = Path('android/app/build.gradle.kts')
text = build.read_text()

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

if 'desugar_jdk_libs' not in text:
    text += '\n\ndependencies {\n'
    text += '    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")\n'
    text += '}\n'

build.write_text(text)

manifest = Path('android/app/src/main/AndroidManifest.xml')
text = manifest.read_text()

permission = '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n'
if 'android.permission.RECEIVE_BOOT_COMPLETED' not in text:
    text = text.replace('<manifest', '<manifest', 1)
    insert_at = text.find('>', text.find('<manifest')) + 1
    text = text[:insert_at] + '\n' + permission + text[insert_at:]

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

manifest.write_text(text)
PY

echo "Android platform prepared with notification scheduling support."
