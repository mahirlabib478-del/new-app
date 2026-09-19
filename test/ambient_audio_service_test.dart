import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/ambient_audio_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  test('presets list has all 6 ambient audio soundscapes with bilingual metadata', () {
    expect(AmbientAudioService.presets.length, 6);
    final ids = AmbientAudioService.presets.map((p) => p.id).toList();
    expect(ids, containsAll(['rain', 'waves', 'stream', 'brown', 'pink', 'white']));

    for (final preset in AmbientAudioService.presets) {
      expect(preset.nameEn.isNotEmpty, isTrue);
      expect(preset.nameBn.isNotEmpty, isTrue);
      expect(preset.descriptionEn.isNotEmpty, isTrue);
      expect(preset.descriptionBn.isNotEmpty, isTrue);
      expect(preset.name(true), preset.nameBn);
      expect(preset.name(false), preset.nameEn);
    }
  });

  test('AmbientAudioService initial state and SharedPreferences persistence', () async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('ambient_sound_preset', 'waves');
    await prefs.setDouble('ambient_sound_volume', 0.75);
    await prefs.setBool('ambient_sound_auto_play', true);

    final service = AmbientAudioService.instance;
    await service.init(prefs);

    expect(service.currentPreset, 'waves');
    expect(service.volume, 0.75);
    expect(service.autoPlayOnFocus, isTrue);
    expect(service.activePresetDetails.id, 'waves');

    // Change preset
    await service.playPreset('brown', prefs: prefs);
    expect(service.currentPreset, 'brown');
    expect(service.isPlaying, isTrue);
    expect(prefs.getString('ambient_sound_preset'), 'brown');

    // Change volume with clamping
    await service.setVolume(1.5, prefs: prefs);
    expect(service.volume, 1.0);
    expect(prefs.getDouble('ambient_sound_volume'), 1.0);

    await service.setVolume(-0.2, prefs: prefs);
    expect(service.volume, 0.0);
    expect(prefs.getDouble('ambient_sound_volume'), 0.0);

    // Stop
    service.stop();
    expect(service.isPlaying, isFalse);

    // Auto play setting
    await service.setAutoPlayOnFocus(false, prefs: prefs);
    expect(service.autoPlayOnFocus, isFalse);
    expect(prefs.getBool('ambient_sound_auto_play'), isFalse);
  });

  test('timer lifecycle transitions pause, resume and end ambient audio', () async {
    final prefs = await SharedPreferences.getInstance();
    final service = AmbientAudioService.instance;
    await service.init(prefs);

    await service.playPreset('rain', prefs: prefs);
    expect(service.isPlaying, isTrue);

    service.onFocusTimerPaused();
    expect(service.isPlaying, isFalse);

    service.onFocusTimerStarted();
    expect(service.isPlaying, isTrue);

    service.onFocusTimerEnded();
    expect(service.isPlaying, isFalse);
  });
}
