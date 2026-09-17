import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/widgets/update_gate.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Update gate blocks app for a mandatory release', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final check = Future<UpdateInfo?>.value(const UpdateInfo(
      latestVersion: '0.3.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
      isMandatory: true,
    ));

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => check, child: const Text('Home content'))));
    await tester.pumpAndSettle();

    expect(find.text('Update required'), findsOneWidget);
    expect(find.text('Version 0.3.0 is ready.'), findsOneWidget);
    expect(find.text('Home content'), findsNothing);
  });

  testWidgets('Update gate surfaces optional releases without blocking the app', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final check = Future<UpdateInfo?>.value(const UpdateInfo(
      latestVersion: '0.3.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
    ));

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => check, child: const Text('Home content'))));
    await tester.pumpAndSettle();

    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Study OS 0.3.0 is available.'), findsOneWidget);
    expect(find.text('Update'), findsOneWidget);
  });

  testWidgets('optional update banner can be dismissed', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final check = Future<UpdateInfo?>.value(const UpdateInfo(
      latestVersion: '0.3.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
    ));

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => check, child: const Text('Home content'))));
    await tester.pumpAndSettle();
    expect(find.text('Study OS 0.3.0 is available.'), findsOneWidget);

    await tester.tap(find.byTooltip('Dismiss'));
    await tester.pumpAndSettle();
    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Study OS 0.3.0 is available.'), findsNothing);
  });

  testWidgets('Update gate keeps app available when no update is reported', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => Future<UpdateInfo?>.value(null), child: const Text('Home content'))));
    await tester.pumpAndSettle();

    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Update required'), findsNothing);
  });

  testWidgets('Update gate fails open when update check throws', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(MaterialApp(
      home: UpdateGate(
        store: store,
        checkForUpdate: () async => throw StateError('simulated startup failure'),
        child: const Text('Home content'),
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Checking for updates…'), findsNothing);
  });

  testWidgets('Update gate does not expose app content while update check is pending', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final completer = Completer<UpdateInfo?>();

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => completer.future, child: const Text('Home content'))));
    await tester.pump();

    expect(find.text('Checking for updates…'), findsOneWidget);
    expect(find.text('Home content'), findsNothing);

    completer.complete(null);
    await tester.pumpAndSettle();
    expect(find.text('Home content'), findsOneWidget);
  });
}
