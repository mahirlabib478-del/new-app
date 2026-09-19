import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/ambient_audio_service.dart';
import '../services/app_language.dart';

class AmbientSoundFocusCard extends StatelessWidget {
  const AmbientSoundFocusCard({
    super.key,
    required this.language,
    this.prefs,
  });

  final AppLanguage language;
  final SharedPreferences? prefs;

  @override
  Widget build(BuildContext context) {
    final audio = AmbientAudioService.instance;
    final isBn = language == AppLanguage.bangla;
    final scheme = Theme.of(context).colorScheme;

    return ListenableBuilder(
      listenable: audio,
      builder: (context, _) {
        final active = audio.activePresetDetails;
        final isPlaying = audio.isPlaying;

        return Card(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: isPlaying ? scheme.primaryContainer : scheme.surfaceContainerHighest,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        active.icon,
                        color: isPlaying ? scheme.primary : scheme.onSurfaceVariant,
                        size: 22,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Text(
                                isBn ? 'অ্যাম্বিয়েন্ট সাউন্ড' : 'Ambient sound',
                                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 13),
                              ),
                              if (isPlaying) ...[
                                const SizedBox(width: 6),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: scheme.primary.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Text(
                                    isBn ? 'চালু' : 'PLAYING',
                                    style: TextStyle(
                                      fontSize: 10,
                                      fontWeight: FontWeight.w900,
                                      color: scheme.primary,
                                      letterSpacing: 0.5,
                                    ),
                                  ),
                                ),
                              ],
                            ],
                          ),
                          const SizedBox(height: 2),
                          Text(
                            active.name(isBn),
                            style: TextStyle(
                              fontSize: 12,
                              color: scheme.onSurfaceVariant,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton.filledTonal(
                      onPressed: () => audio.togglePlay(prefs: prefs),
                      icon: Icon(isPlaying ? Icons.pause_rounded : Icons.play_arrow_rounded),
                      tooltip: isPlaying ? (isBn ? 'বিরতি' : 'Pause') : (isBn ? 'চালু করুন' : 'Play'),
                    ),
                    IconButton(
                      onPressed: () => _showPresetsSheet(context),
                      icon: const Icon(Icons.tune_rounded),
                      tooltip: isBn ? 'সাউন্ড সেটিংস' : 'Sound settings',
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: AmbientAudioService.presets.map((preset) {
                      final isSelected = audio.currentPreset == preset.id;
                      return Padding(
                        padding: const EdgeInsets.only(right: 6),
                        child: FilterChip(
                          avatar: Icon(
                            preset.icon,
                            size: 16,
                            color: isSelected ? scheme.onPrimaryContainer : null,
                          ),
                          label: Text(preset.name(isBn)),
                          selected: isSelected,
                          onSelected: (_) {
                            audio.playPreset(preset.id, prefs: prefs);
                          },
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showPresetsSheet(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => AmbientSoundBottomSheet(
        language: language,
        prefs: prefs,
      ),
    );
  }
}

class AmbientSoundBottomSheet extends StatelessWidget {
  const AmbientSoundBottomSheet({
    super.key,
    required this.language,
    this.prefs,
  });

  final AppLanguage language;
  final SharedPreferences? prefs;

  @override
  Widget build(BuildContext context) {
    final audio = AmbientAudioService.instance;
    final isBn = language == AppLanguage.bangla;
    final scheme = Theme.of(context).colorScheme;

    return ListenableBuilder(
      listenable: audio,
      builder: (context, _) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 36,
                    height: 4,
                    decoration: BoxDecoration(
                      color: scheme.outlineVariant,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Icon(Icons.waves_rounded, color: scheme.primary),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        isBn ? 'ফোকাস অ্যাম্বিয়েন্ট সাউন্ড' : 'Focus ambient sounds',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
                      ),
                    ),
                    if (audio.isPlaying)
                      TextButton.icon(
                        onPressed: () => audio.stop(),
                        icon: const Icon(Icons.stop_rounded, size: 18),
                        label: Text(isBn ? 'বন্ধ' : 'Stop'),
                      ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  isBn
                      ? 'অবিরাম প্রাকৃতিক শব্দ মনোযোগ ধরে রাখতে ও পারিপার্শ্বিক শব্দ ঢাকতে সাহায্য করে।'
                      : 'Mask background noise with procedural acoustic ambience for deep study.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 18),
                Row(
                  children: [
                    const Icon(Icons.volume_up_rounded, size: 20),
                    const SizedBox(width: 10),
                    Text(
                      '${(audio.volume * 100).round()}%',
                      style: const TextStyle(fontWeight: FontWeight.w800),
                    ),
                    Expanded(
                      child: Slider(
                        value: audio.volume,
                        onChanged: (val) => audio.setVolume(val, prefs: prefs),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  isBn ? 'সাউন্ড নির্বাচন করুন' : 'Choose sound',
                  style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 13),
                ),
                const SizedBox(height: 8),
                ...AmbientAudioService.presets.map((preset) {
                  final isSelected = audio.currentPreset == preset.id;
                  final isPlayingThis = isSelected && audio.isPlaying;

                  return Card(
                    color: isSelected ? scheme.primaryContainer.withValues(alpha: 0.5) : null,
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: isSelected ? scheme.primary : scheme.surfaceContainerHighest,
                        child: Icon(
                          preset.icon,
                          color: isSelected ? scheme.onPrimary : scheme.onSurfaceVariant,
                          size: 20,
                        ),
                      ),
                      title: Text(
                        preset.name(isBn),
                        style: TextStyle(fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600),
                      ),
                      subtitle: Text(preset.description(isBn)),
                      trailing: IconButton(
                        icon: Icon(
                          isPlayingThis ? Icons.pause_circle_filled_rounded : Icons.play_circle_fill_rounded,
                          color: scheme.primary,
                          size: 32,
                        ),
                        onPressed: () {
                          if (isPlayingThis) {
                            audio.stop();
                          } else {
                            audio.playPreset(preset.id, prefs: prefs);
                          }
                        },
                      ),
                      onTap: () {
                        audio.playPreset(preset.id, prefs: prefs);
                      },
                    ),
                  );
                }),
              ],
            ),
          ),
        );
      },
    );
  }
}
