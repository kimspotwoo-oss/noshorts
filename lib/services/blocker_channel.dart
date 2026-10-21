import 'package:flutter/services.dart';

/// 차단 방식 설정. Android 쪽 BlockerSettings 와 같은 키를 쓴다.
class BlockerSettings {
  const BlockerSettings({
    required this.maskEnabled,
    required this.warningEnabled,
    required this.extraConfirmEnabled,
    required this.extraConfirmCount,
    required this.allowUntil,
  });

  /// 추가 확인 경고 횟수의 최대값. Android 쪽 MAX_EXTRA_CONFIRMS 와 같다.
  static const maxExtraConfirms = 5;

  /// 쇼츠/릴스 진입 버튼 위에 가림막을 띄운다.
  final bool maskEnabled;

  /// 숏폼 화면에 들어가면 경고 화면을 띄운다.
  final bool warningEnabled;

  /// "5분만 보기"를 누르면 확인 경고를 한 번 더 띄운다.
  final bool extraConfirmEnabled;

  /// 추가 확인 경고를 몇 번 띄울지. (1~[maxExtraConfirms])
  final int extraConfirmCount;

  /// "잠깐만 보기"로 차단이 멈춰 있는 마지막 시각.
  final DateTime allowUntil;

  factory BlockerSettings.fromMap(Map<Object?, Object?> map) => BlockerSettings(
        maskEnabled: map['maskEnabled'] as bool? ?? true,
        warningEnabled: map['warningEnabled'] as bool? ?? true,
        extraConfirmEnabled: map['extraConfirmEnabled'] as bool? ?? false,
        extraConfirmCount: (map['extraConfirmCount'] as num?)?.toInt() ?? 1,
        allowUntil: DateTime.fromMillisecondsSinceEpoch(
          (map['allowUntil'] as num?)?.toInt() ?? 0,
        ),
      );
}

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

  static Future<BlockerSettings> getSettings() async {
    final map = await _channel.invokeMapMethod<Object?, Object?>('getSettings');
    return BlockerSettings.fromMap(map ?? const {});
  }

  static Future<BlockerSettings> setSettings({
    bool? maskEnabled,
    bool? warningEnabled,
    bool? extraConfirmEnabled,
    int? extraConfirmCount,
  }) async {
    final map = await _channel.invokeMapMethod<Object?, Object?>('setSettings', {
      'maskEnabled': ?maskEnabled,
      'warningEnabled': ?warningEnabled,
      'extraConfirmEnabled': ?extraConfirmEnabled,
      'extraConfirmCount': ?extraConfirmCount,
    });
    return BlockerSettings.fromMap(map ?? const {});
  }
}
