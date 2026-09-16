import 'package:flutter/services.dart';

import 'local_store.dart';

/// Lightweight, offline sound feedback using the platform's native system sounds.
/// No audio assets or network access are required.
class SoundEffects {
  const SoundEffects(this.store);

  final LocalStore store;

  Future<void> tap() => _play(SystemSoundType.click);
  Future<void> success() => _play(SystemSoundType.alert);

  Future<void> _play(SystemSoundType type) async {
    if (!store.soundEffectsEnabled) return;
    try {
      await SystemSound.play(type);
    } on PlatformException {
      // Sound must never block study actions on unsupported platforms.
    }
  }
}
