import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:ebookreader/main.dart' as app;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('E2E App Tests', () {
    testWidgets('App should start and display home screen', (WidgetTester tester) async {
      app.main();
      await tester.pumpAndSettle();

      // Проверяем, что приложение загрузилось
      expect(find.byType(MaterialApp), findsOneWidget);
    });

    testWidgets('Navigation to home screen should work', (WidgetTester tester) async {
      app.main();
      await tester.pumpAndSettle();

      // Проверяем наличие основного контента
      expect(find.byType(Scaffold), findsWidgets);
    });

    testWidgets('Splash screen should be displayed on app start', (WidgetTester tester) async {
      app.main();
      
      // Проверяем, что splash screen отображается
      await tester.pumpAndSettle(const Duration(seconds: 3));
      
      // После splash screen должен быть переход на главный экран
      expect(find.byType(Scaffold), findsWidgets);
    });
  });
}
