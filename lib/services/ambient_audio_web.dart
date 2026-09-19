import 'dart:js_interop';
import 'ambient_audio_interface.dart';

@JS('ambientAudio.play')
external void _jsPlay(JSString preset, JSNumber volume);

@JS('ambientAudio.stop')
external void _jsStop();

@JS('ambientAudio.setVolume')
external void _jsSetVolume(JSNumber volume);

class AmbientAudioPlatformImpl implements AmbientAudioPlatform {
  @override
  void play(String preset, double volume) {
    try {
      _jsPlay(preset.toJS, volume.toJS);
    } catch (_) {}
  }

  @override
  void stop() {
    try {
      _jsStop();
    } catch (_) {}
  }

  @override
  void setVolume(double volume) {
    try {
      _jsSetVolume(volume.toJS);
    } catch (_) {}
  }
}
