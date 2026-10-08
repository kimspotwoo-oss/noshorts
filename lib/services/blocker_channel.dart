import 'package:flutter/services.dart';

/// Android 네이티브 차단 서비스(ShortsBlockerService)와 통신하는 채널.
class BlockerChannel {
  static const _channel = MethodChannel('com.kimspotwoo.noshorts/blocker');

  /// 접근성 서비스가 켜져 있는지 확인한다.
  static Future<bool> isServiceEnabled() async {
    return await _channel.invokeMethod<bool>('isServiceEnabled') ?? false;
  }

  /// 시스템 접근성 설정 화면을 연다.
  static Future<void> openAccessibilitySettings() {
    return _channel.invokeMethod('openAccessibilitySettings');
  }
}
