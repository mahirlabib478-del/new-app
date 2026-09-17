import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/services/local_store.dart';
import 'package:study_os/widgets/update_gate.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('mandatory release overlays app after async check', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final completer = Completer<UpdateInfo?>();

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => completer.future, child: const Text('Home content'))));
    await tester.pump();

    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Update required'), findsNothing);

    completer.complete(const UpdateInfo(
      latestVersion: '0.3.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
      isMandatory: true,
    ));
    await tester.pumpAndSettle();

    expect(find.text('Update required'), findsOneWidget);
    expect(find.text('Version 0.3.0 is ready.'), findsOneWidget);
    expect(find.text('Home content'), findsOneWidget);
  });

  testWidgets('optional release appears after check without blocking first frame', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());
    final completer = Completer<UpdateInfo?>();

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => completer.future, child: const Text('Home content'))));
    await tester.pump();
    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Study OS 0.3.0 is available.'), findsNothing);

    completer.complete(const UpdateInfo(
      latestVersion: '0.3.0',
      releaseUrl: 'https://github.com/mahirlabib478-del/new-app/releases/latest',
    ));
    await tester.pumpAndSettle();

    expect(find.text('Home content'), findsOneWidget);
    expect(find.text('Study OS 0.3.0 is available.'), findsOneWidget);
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

  testWidgets('no update keeps app available', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(MaterialApp(home: UpdateGate(store: store, checkForUpdate: () => Future<UpdateInfo?>.value(null), child: const Text('Home content'))));
    await tester.pump();
    expect(find.text('Home content'), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.text('Home content'), findsOneWidget);
  });

  testWidgets('update check failure never blocks app startup', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(MaterialApp(
      home: UpdateGate(
        store: store,
        checkForUpdate: () async => throw StateError('simulated startup failure'),
        child: const Text('Home content'),
      ),
    ));
    await tester.pump();

    expect(find.text('Home content'), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.text('Home content'), findsOneWidget);
  });
}
