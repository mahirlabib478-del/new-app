import 'package:flutter/services.dart';

import 'ambient_audio_interface.dart';

class AmbientAudioPlatformImpl implements AmbientAudioPlatform {
  static const MethodChannel _channel = MethodChannel('study_os/ambient_audio');

  @override
  void play(String preset, double volume) {
    _channel.invokeMethod<void>('play', <String, dynamic>{
      'preset': preset,
      'volume': volume,
    }).catchError((_) {});
  }

  @override
  void stop() {
    _channel.invokeMethod<void>('stop').catchError((_) {});
  }

  @override
  void setVolume(double volume) {
    _channel.invokeMethod<void>('setVolume', <String, dynamic>{
      'volume': volume,
    }).catchError((_) {});
  }
}
