import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/services/sound_effects.dart';

void main() {
  const channel = MethodChannel('study_os/sound');

  tearDown(() {
    channel.setMockMethodCallHandler(null);
    debugDefaultTargetPlatformOverride = null;
  });

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

  test('enabled Android sound sends a tap effect to the native channel', () async {
    TestWidgetsFlutterBinding.ensureInitialized();
    SharedPreferences.setMockInitialValues({'sound_effects_enabled': true});
    final store = LocalStore(await SharedPreferences.getInstance());
    final calls = <MethodCall>[];

    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    channel.setMockMethodCallHandler((call) async {
      calls.add(call);
      return null;
    });

    await SoundEffects(store).tap();

    expect(calls, hasLength(1));
    expect(calls.single.method, 'play');
    expect(calls.single.arguments, 'tap');
  });

  test('disabled sound effects do not send anything to the native channel', () async {
    TestWidgetsFlutterBinding.ensureInitialized();
    SharedPreferences.setMockInitialValues({'sound_effects_enabled': false});
    final store = LocalStore(await SharedPreferences.getInstance());
    var callCount = 0;

    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    channel.setMockMethodCallHandler((call) async {
      callCount++;
      return null;
    });

    await SoundEffects(store).tap();

    expect(callCount, 0);
  });
}
