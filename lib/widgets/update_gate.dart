import 'package:flutter/material.dart';

import '../services/local_store.dart';
import '../services/update_service.dart';

class UpdateGate extends StatefulWidget {
  const UpdateGate({
    super.key,
    required this.store,
    required this.child,
    this.checkForUpdate,
  });

  final LocalStore store;
  final Widget child;
  final Future<UpdateInfo?> Function()? checkForUpdate;

  @override
  State<UpdateGate> createState() => _UpdateGateState();
}

class _UpdateGateState extends State<UpdateGate> {
  late final Future<UpdateInfo?> _check =
      (widget.checkForUpdate ?? const UpdateService().checkForUpdate)();

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<UpdateInfo?>(
      future: _check,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const _UpdateLoadingScreen();
        }

        final info = snapshot.data;
        if (info == null) return widget.child;
        return _ForceUpdateScreen(info: info);
      },
    );
  }
}

class _UpdateLoadingScreen extends StatelessWidget {
  const _UpdateLoadingScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: EdgeInsets.all(28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(),
                SizedBox(height: 18),
                Text('Checking for updates…'),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ForceUpdateScreen extends StatelessWidget {
  const _ForceUpdateScreen({required this.info});

  final UpdateInfo info;

  Future<void> _update(BuildContext context) async {
    final opened = await const UpdateService().openRelease(info);
    if (!context.mounted || opened) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Update page could not be opened. Please try again.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(26),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      CircleAvatar(
                        radius: 34,
                        backgroundColor: scheme.primaryContainer,
                        child: Icon(Icons.system_update_rounded, size: 34, color: scheme.onPrimaryContainer),
                      ),
                      const SizedBox(height: 18),
                      const Text('Update required', style: TextStyle(fontSize: 26, fontWeight: FontWeight.w900)),
                      const SizedBox(height: 8),
                      Text(
                        'A newer version of Study OS is available. Please update to continue using the app.',
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.bodyLarge,
                      ),
                      const SizedBox(height: 12),
                      Text('Version ${info.latestVersion} is ready.', style: const TextStyle(fontWeight: FontWeight.w800)),
                      const SizedBox(height: 22),
                      FilledButton.icon(
                        onPressed: () => _update(context),
                        icon: const Icon(Icons.download_rounded),
                        label: const SizedBox(width: double.infinity, child: Center(child: Text('Update now'))),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Your study data stays on this device during a normal app update.',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
