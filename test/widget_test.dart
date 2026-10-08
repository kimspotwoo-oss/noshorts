import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:noshorts/main.dart';

void main() {
  const channel = MethodChannel('com.kimspotwoo.noshorts/blocker');

  Future<void> pumpWithServiceEnabled(WidgetTester tester, bool enabled) async {
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (call) async => call.method == 'isServiceEnabled' ? enabled : null,
    );
    await tester.pumpWidget(const NoShortsApp());
    await tester.pumpAndSettle();
  }

  testWidgets('서비스가 꺼져 있으면 켜기 버튼을 보여준다', (tester) async {
    await pumpWithServiceEnabled(tester, false);
    expect(find.text('숏폼 차단이 꺼져 있어요'), findsOneWidget);
    expect(find.text('접근성 설정에서 켜기'), findsOneWidget);
  });

  testWidgets('서비스가 켜져 있으면 켜기 버튼을 숨긴다', (tester) async {
    await pumpWithServiceEnabled(tester, true);
    expect(find.text('숏폼 차단이 켜져 있어요'), findsOneWidget);
    expect(find.text('접근성 설정에서 켜기'), findsNothing);
  });
}
