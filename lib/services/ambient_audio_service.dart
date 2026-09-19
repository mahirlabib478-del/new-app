import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'ambient_audio_platform.dart';

class AmbientPreset {
  const AmbientPreset({
    required this.id,
    required this.nameEn,
    required this.nameBn,
    required this.descriptionEn,
    required this.descriptionBn,
    required this.icon,
  });

  final String id;
  final String nameEn;
  final String nameBn;
  final String descriptionEn;
  final String descriptionBn;
  final IconData icon;

  String name(bool isBangla) => isBangla ? nameBn : nameEn;
  String description(bool isBangla) => isBangla ? descriptionBn : descriptionEn;
}

class AmbientAudioService extends ChangeNotifier {
  AmbientAudioService._();

  static final AmbientAudioService instance = AmbientAudioService._();

  static const List<AmbientPreset> presets = [
    AmbientPreset(
      id: 'rain',
      nameEn: 'Gentle Rain',
      nameBn: 'মৃদু বৃষ্টি',
      descriptionEn: 'Continuous rainfall for deep calm',
      descriptionBn: 'গভীর মনোযোগের জন্য অবিরাম বৃষ্টির শব্দ',
      icon: Icons.water_drop_rounded,
    ),
    AmbientPreset(
      id: 'waves',
      nameEn: 'Ocean Waves',
      nameBn: 'সাগরের ঢেউ',
      descriptionEn: 'Rhythmic rolling ocean surf',
      descriptionBn: 'ছন্দময় সাগরের শান্ত ঢেউ',
      icon: Icons.waves_rounded,
    ),
    AmbientPreset(
      id: 'stream',
      nameEn: 'Forest Stream',
      nameBn: 'বনের ঝরনা',
      descriptionEn: 'Tranquil flowing woodland creek',
      descriptionBn: 'সবুজ বনের শান্ত বয়ে চলা ঝরনা',
      icon: Icons.forest_rounded,
    ),
    AmbientPreset(
      id: 'brown',
      nameEn: 'Brown Noise',
      nameBn: 'ব্রাউন নয়েজ',
      descriptionEn: 'Deep, warm rumble for focus & ADHD',
      descriptionBn: 'গভীর ও উষ্ণ লো-ফ্রিকোয়েন্সি শব্দ',
      icon: Icons.graphic_eq_rounded,
    ),
    AmbientPreset(
      id: 'pink',
      nameEn: 'Pink Noise',
      nameBn: 'পিংক নয়েজ',
      descriptionEn: 'Balanced natural acoustic roll-off',
      descriptionBn: 'প্রাকৃতিক ও ভারসাম্যপূর্ণ ব্যাকগ্রাউন্ড সাউন্ড',
      icon: Icons.equalizer_rounded,
    ),
    AmbientPreset(
      id: 'white',
      nameEn: 'White Noise',
      nameBn: 'হোয়াইট নয়েজ',
      descriptionEn: 'Even frequency distraction masking',
      descriptionBn: 'আশেপাশের বিভ্রান্তিকর আওয়াজ ঢাকতে উপযোগী',
      icon: Icons.grain_rounded,
    ),
  ];

  final AmbientAudioPlatform _platform = createPlatformAudio();

  String _currentPreset = 'rain';
  double _volume = 0.5;
  bool _isPlaying = false;
  bool _autoPlayOnFocus = false;
  bool _wasPlayingBeforePause = false;

  String get currentPreset => _currentPreset;
  double get volume => _volume;
  bool get isPlaying => _isPlaying;
  bool get autoPlayOnFocus => _autoPlayOnFocus;

  AmbientPreset get activePresetDetails {
    return presets.firstWhere(
      (p) => p.id == _currentPreset,
      orElse: () => presets.first,
    );
  }

  Future<void> init(SharedPreferences prefs) async {
    _currentPreset = prefs.getString('ambient_sound_preset') ?? 'rain';
    _volume = (prefs.getDouble('ambient_sound_volume') ?? 0.5).clamp(0.0, 1.0);
    _autoPlayOnFocus = prefs.getBool('ambient_sound_auto_play') ?? false;
    notifyListeners();
  }

  Future<void> playPreset(String presetId, {SharedPreferences? prefs}) async {
    _currentPreset = presetId;
    _isPlaying = true;
    _platform.play(presetId, _volume);
    notifyListeners();
    if (prefs != null) {
      await prefs.setString('ambient_sound_preset', presetId);
    }
  }

  void togglePlay({SharedPreferences? prefs}) {
    if (_isPlaying) {
      stop();
    } else {
      playPreset(_currentPreset, prefs: prefs);
    }
  }

  void stop() {
    _isPlaying = false;
    _wasPlayingBeforePause = false;
    _platform.stop();
    notifyListeners();
  }

  void pause() {
    if (_isPlaying) {
      _wasPlayingBeforePause = true;
      _isPlaying = false;
      _platform.stop();
      notifyListeners();
    }
  }

  void resume() {
    if (_wasPlayingBeforePause) {
      _wasPlayingBeforePause = false;
      playPreset(_currentPreset);
    }
  }

  Future<void> setVolume(double newVolume, {SharedPreferences? prefs}) async {
    _volume = newVolume.clamp(0.0, 1.0);
    _platform.setVolume(_volume);
    notifyListeners();
    if (prefs != null) {
      await prefs.setDouble('ambient_sound_volume', _volume);
    }
  }

  Future<void> setAutoPlayOnFocus(bool enable, {required SharedPreferences prefs}) async {
    _autoPlayOnFocus = enable;
    await prefs.setBool('ambient_sound_auto_play', enable);
    notifyListeners();
  }

  void onFocusTimerStarted() {
    if (_autoPlayOnFocus && !_isPlaying) {
      playPreset(_currentPreset);
    } else if (_wasPlayingBeforePause) {
      resume();
    }
  }

  void onFocusTimerPaused() {
    if (_isPlaying) {
      pause();
    }
  }

  void onFocusTimerEnded() {
    if (_isPlaying) {
      stop();
    }
  }
}
