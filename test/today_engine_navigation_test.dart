import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/screens/today_engine_screen.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  testWidgets('Study hub exposes the Today Engine screen', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = LocalStore(prefs);

    await tester.pumpWidget(StudyOS(store: store, prefs: prefs, checkForUpdate: () async => null));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Study').last);
    await tester.pumpAndSettle();

    expect(find.text('Today Engine'), findsOneWidget);
    await tester.tap(find.text('Today Engine'));
    await tester.pumpAndSettle();

    expect(find.byType(TodayEngineScreen), findsOneWidget);
    expect(find.text('What do I need to do today?'), findsOneWidget);
  });
}
