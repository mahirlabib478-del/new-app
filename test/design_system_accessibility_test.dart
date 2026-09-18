import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shared design system keeps accessible control sizing', (tester) async {
    final scheme = ColorScheme.fromSeed(seedColor: const Color(0xFF6C63FF));
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: scheme,
          cardTheme: CardThemeData(
            margin: EdgeInsets.zero,
            elevation: 0,
            color: scheme.surfaceContainerLow,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
          ),
          filledButtonTheme: FilledButtonThemeData(
            style: FilledButton.styleFrom(
              minimumSize: const Size(0, 50),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            ),
          ),
        ),
        home: Scaffold(
          body: Card(
            child: FilledButton(onPressed: () {}, child: const Text('Start')),
          ),
        ),
      ),
    );

    final card = tester.widget<Card>(find.byType(Card));
    expect(card.color, scheme.surfaceContainerLow);

    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.child, isA<Text>());

    final size = tester.getSize(find.byType(FilledButton));
    expect(size.height, greaterThanOrEqualTo(48));
  });
}
