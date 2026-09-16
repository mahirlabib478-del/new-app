import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:study_os/main.dart';
import 'package:study_os/services/local_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Study Hub planner is removed before focus starts', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final store = LocalStore(await SharedPreferences.getInstance());

    await tester.pumpWidget(StudyOS(store: store));
    await tester.tap(find.text('Study').last);
    await tester.pumpAndSettle();
    expect(find.text('Choose your next move'), findsOneWidget);

    await tester.tap(find.text('Exam Preparation').last);
    await tester.pumpAndSettle();
    expect(find.text('Exam Preparation'), findsWidgets);
  });
}
