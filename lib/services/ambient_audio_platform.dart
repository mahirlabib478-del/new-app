import 'ambient_audio_interface.dart';
import 'ambient_audio_stub.dart'
    if (dart.library.html) 'ambient_audio_web.dart';

export 'ambient_audio_interface.dart';

AmbientAudioPlatform createPlatformAudio() => AmbientAudioPlatformImpl();
