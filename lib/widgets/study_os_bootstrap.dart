import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class StudyOSBootstrap extends StatefulWidget {
  const StudyOSBootstrap({super.key, required this.builder});

  final Widget Function(SharedPreferences prefs) builder;

  @override
  State<StudyOSBootstrap> createState() => _StudyOSBootstrapState();
}

class _StudyOSBootstrapState extends State<StudyOSBootstrap> {
  late Future<SharedPreferences> _prefsFuture;

  @override
  void initState() {
    super.initState();
    _prefsFuture = SharedPreferences.getInstance();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<SharedPreferences>(
      future: _prefsFuture,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            home: Scaffold(
              body: Center(
                child: FilledButton.icon(
                  onPressed: () => setState(() {
                    _prefsFuture = SharedPreferences.getInstance();
                  }),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Retry'),
                ),
              ),
            ),
          );
        }

        final prefs = snapshot.data;
        if (prefs == null) {
          return const MaterialApp(
            debugShowCheckedModeBanner: false,
            home: Scaffold(
              body: Center(child: CircularProgressIndicator()),
            ),
          );
        }

        return widget.builder(prefs);
      },
    );
  }
}
