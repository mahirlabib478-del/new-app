# Android release signing

Study OS builds signed Android releases in GitHub Actions. The signing keystore is never committed to the repository.

## 1. Create the release keystore

On a machine with Java `keytool` available:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias study-os
```

Keep `upload-keystore.jks` private and back it up securely. If this signing key is lost, future Android updates cannot be signed with the same key.

## 2. Convert the keystore to Base64

Linux:

```bash
base64 -w 0 upload-keystore.jks > keystore.base64
```

macOS:

```bash
base64 upload-keystore.jks | tr -d '\n' > keystore.base64
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Content -NoNewline keystore.base64
```

The contents of `keystore.base64` become the `ANDROID_KEYSTORE_BASE64` repository secret.

## 3. Add GitHub Actions secrets

In the repository, open **Settings → Secrets and variables → Actions → New repository secret** and create:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Entire Base64 value from `keystore.base64` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password chosen during `keytool` setup |
| `ANDROID_KEY_ALIAS` | `study-os` |
| `ANDROID_KEY_PASSWORD` | Key password chosen during `keytool` setup |

Never commit these values or paste them into an issue, pull request, source file, or chat.

## 4. Create a release

Release tags must include both the semantic version and an increasing Android `versionCode`:

```text
v0.3.0+3
```

The workflow uses `0.3.0` as the Android version name and `3` as the Android version code. For the next release, use a higher version code, for example `v0.3.1+4`.

Pushing a matching tag automatically:

1. generates the Android platform project,
2. restores the signing keystore only inside the GitHub Actions runner,
3. validates the keystore and alias,
4. builds a signed APK,
5. builds a signed AAB,
6. uploads both as workflow artifacts, and
7. creates a GitHub Release containing both files.

## Security notes

- The generated `android/app/release-key.jks` exists only on the Actions runner.
- The keystore and passwords are supplied through GitHub Actions secrets.
- Do not rotate or replace the signing key casually; Android update compatibility depends on signing continuity.
