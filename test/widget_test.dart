import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:noshorts/main.dart';

void main() {
  const channel = MethodChannel('com.kimspotwoo.noshorts/blocker');

  late Map<String, Object> settings;
  late List<MethodCall> calls;

  Future<void> pumpWithServiceEnabled(WidgetTester tester, bool enabled) async {
    settings = {'maskEnabled': true, 'warningEnabled': true, 'allowUntil': 0};
    calls = [];
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      switch (call.method) {
        case 'isServiceEnabled':
          return enabled;
        case 'getSettings':
          return settings;
        case 'setSettings':
          settings = {...settings, ...(call.arguments as Map).cast<String, Object>()};
          return settings;
      }
      return null;
    });
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

  testWidgets('경고 스위치를 끄면 설정이 네이티브로 전달된다', (tester) async {
    await pumpWithServiceEnabled(tester, true);
    await tester.tap(find.text('들어가기 전에 경고하기'));
    await tester.pumpAndSettle();

    final call = calls.lastWhere((c) => c.method == 'setSettings');
    expect(call.arguments, {'warningEnabled': false});
    expect(settings['warningEnabled'], false);
    expect(settings['maskEnabled'], true);
  });
}
