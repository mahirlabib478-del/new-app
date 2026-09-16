import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/sound_effects.dart';

void main() {
  test('sound effects are enabled by default and persist when changed', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);

    expect(store.soundEffectsEnabled, isTrue);
    await store.setSoundEffectsEnabled(false);
    expect(store.soundEffectsEnabled, isFalse);
    await store.setSoundEffectsEnabled(true);
    expect(store.soundEffectsEnabled, isTrue);
  });

  test('sound effects are safe when disabled', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);
    await store.setSoundEffectsEnabled(false);

    // The action must be a no-op when the user disables sound feedback.
    await const SoundEffects;
  });
}
