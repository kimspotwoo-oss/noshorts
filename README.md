# NoShorts

YouTube 쇼츠, Instagram 릴스, TikTok 같은 숏폼 시청을 줄여주는 Android 앱입니다. (iOS는 이후 지원 예정)

## 구조

- `lib/` Flutter UI
  - `services/blocker_channel.dart` 네이티브 차단 서비스와 통신하는 MethodChannel
- `android/app/src/main/kotlin/com/kimspotwoo/noshorts/`
  - `ShortsBlockerService.kt` 숏폼 화면을 감지하는 접근성 서비스
  - `MainActivity.kt` 채널 핸들러 (서비스 상태 확인, 접근성 설정 열기)
- `android/app/src/main/res/xml/shorts_blocker_service.xml` 감시 대상 앱 목록

## 개발

```sh
flutter pub get
flutter analyze
flutter test
flutter run            # 연결된 Android 기기/에뮬레이터에서 실행
```

PR마다 GitHub Actions가 디버그 APK를 빌드해 `noshorts-debug-apk` 아티팩트로 올립니다.
