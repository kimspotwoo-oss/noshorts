# NoShorts

YouTube 쇼츠, Instagram 릴스, TikTok 같은 숏폼 시청을 줄여주는 Android 앱입니다. (iOS는 이후 지원 예정)

## 기능

- **쇼츠 버튼 가리기**: YouTube 쇼츠 탭, Instagram 릴스 탭 버튼 위에 가림막을 띄워 누르지 못하게 합니다.
- **들어가기 전에 경고하기**: 숏폼 화면에 들어가면 화면 전체를 덮는 경고를 띄웁니다. "돌아가기"를 누르면 빠져나가고, "5분만 보기"는 5초를 기다려야 눌립니다. TikTok은 앱 전체가 숏폼이라 앱을 열면 바로 경고합니다.

두 기능 모두 접근성 서비스의 오버레이를 쓰므로 "다른 앱 위에 표시" 권한은 필요 없습니다.
숏폼 화면 감지는 각 앱의 화면 구조(view id, 버튼 라벨)에 의존하므로, 앱 업데이트로 감지가 안 되면 `ShortsDetector.kt`의 `rules`를 고칩니다.

## 구조

- `lib/` Flutter UI
  - `services/blocker_channel.dart` 네이티브 차단 서비스와 통신하는 MethodChannel
- `android/app/src/main/kotlin/com/kimspotwoo/noshorts/`
  - `ShortsBlockerService.kt` 이벤트를 받아 가림막/경고를 띄우는 접근성 서비스
  - `ShortsDetector.kt` 앱별 숏폼 화면·진입 버튼 감지 규칙
  - `OverlayController.kt` 가림막과 경고 화면 오버레이
  - `BlockerSettings.kt` Flutter와 공유하는 설정
  - `MainActivity.kt` 채널 핸들러 (서비스 상태 확인, 접근성 설정 열기)
- `android/app/src/main/res/xml/shorts_blocker_service.xml` 접근성 서비스 설정

## 개발

```sh
flutter pub get
flutter analyze
flutter test
flutter run            # 연결된 Android 기기/에뮬레이터에서 실행
```

PR마다 GitHub Actions가 디버그 APK를 빌드해 `noshorts-debug-apk` 아티팩트로 올립니다.
