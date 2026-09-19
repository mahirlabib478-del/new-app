import os
from pathlib import Path

build = Path("app/build.gradle.kts")
if not build.exists():
    raise SystemExit("app/build.gradle.kts not found")

text = build.read_text()
marker = "    buildTypes {"
signing = """    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

"""

if marker in text and 'create("release")' not in text:
    text = text.replace(marker, signing + marker, 1)

if 'signingConfig = signingConfigs.getByName("release")' not in text:
    text = text.replace(
        "isMinifyEnabled = false",
        "isMinifyEnabled = false\n            signingConfig = signingConfigs.getByName(\"release\")",
        1,
    )

build.write_text(text)
print("Release signing configured successfully.")
