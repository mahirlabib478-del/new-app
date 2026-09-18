import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../services/local_store.dart';
import '../services/update_service.dart';

export '../services/update_service.dart' show UpdateInfo;

/// Non-blocking update notifier. Core study functionality never depends on
/// the update service, network availability, or release metadata.
class UpdateGate extends StatefulWidget {
  const UpdateGate({super.key, required this.store, required this.child, this.checkForUpdate});
  final LocalStore store;
  final Widget child;
  final Future<UpdateInfo?> Function()? checkForUpdate;
  @override State<UpdateGate> createState() => _UpdateGateState();
}

class _UpdateGateState extends State<UpdateGate> {
  late final Future<UpdateInfo?> _check = _runCheck();
  bool dismissed = false;

  Future<UpdateInfo?> _runCheck() async {
    try {
      if (widget.checkForUpdate != null) return await widget.checkForUpdate!();
      if (kReleaseMode) {
        await Future<void>.delayed(const Duration(milliseconds: 1000));
      }
      final packageInfo = await PackageInfo.fromPlatform();
      return await UpdateService(currentVersion: '${packageInfo.version}+${packageInfo.buildNumber}', prefs: widget.store.prefs).checkForUpdate();
    } catch (_) {
      // Update checks are optional. Never let them block or terminate the app.
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        widget.child,
        FutureBuilder<UpdateInfo?>(
          future: _check,
          builder: (context, snapshot) {
            final info = snapshot.data;
            if (snapshot.connectionState != ConnectionState.done || info == null || dismissed) return const SizedBox.shrink();
            // Mandatory/optional policies are intentionally presented the same
            // way for now: as a dismissible, non-blocking update notice.
            return Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: _UpdateBanner(info: info, onDismiss: () => setState(() => dismissed = true)),
            );
          },
        ),
      ],
    );
  }
}

class _UpdateBanner extends StatelessWidget {
  const _UpdateBanner({required this.info, required this.onDismiss});
  final UpdateInfo info;
  final VoidCallback onDismiss;

  Future<void> _update(BuildContext context) async {
    try {
      final opened = await const UpdateService().openRelease(info);
      if (!context.mounted || opened) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Update page could not be opened. Please try again.')));
    } catch (_) {
      if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Update page could not be opened. Please try again.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.secondaryContainer,
      elevation: 2,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 8, 10),
          child: Row(
            children: [
              Icon(Icons.system_update_rounded, color: scheme.onSecondaryContainer),
              const SizedBox(width: 10),
              Expanded(child: Text('Study OS ${info.latestVersion} is available.', style: TextStyle(fontWeight: FontWeight.w800, color: scheme.onSecondaryContainer))),
              TextButton(onPressed: () => _update(context), child: const Text('Update')),
              IconButton(onPressed: onDismiss, tooltip: 'Dismiss', icon: const Icon(Icons.close_rounded)),
            ],
          ),
        ),
      ),
    );
  }
}
