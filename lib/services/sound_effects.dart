import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'local_store.dart';

/// Lightweight, offline sound feedback for study interactions.
///
/// Android uses a tiny native ToneGenerator tone so sound feedback does not
/// depend on the device's optional "touch sounds" setting. Other platforms
/// fall back to Flutter's native system sound where supported.
class SoundEffects {
  const SoundEffects(this.store);

  static const _channel = MethodChannel('study_os/sound');
  final LocalStore store;

  Future<void> tap() => _play('tap');
  Future<void> success() => _play('success');

  Future<void> _play(String effect) async {
    if (!store.soundEffectsEnabled) return;
    try {
      if (defaultTargetPlatform == TargetPlatform.android) {
        await _channel.invokeMethod<void>('play', effect);
        return;
      }
      await SystemSound.play(
        effect == 'success' ? SystemSoundType.alert : SystemSoundType.click,
      );
    } on MissingPluginException {
      // Sound is optional and must never block a study action.
    } on PlatformException {
      // Sound is optional and must never block a study action.
    }
  }
}
