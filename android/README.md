# Android 앱

Kotlin과 Jetpack Compose로 만드는 세입세잎 Android 앱입니다. 현재 기본 구조와 매물 등록·조회·수정·삭제 흐름까지 연결되어 있습니다.

## 현재 구조

- `app/`: 애플리케이션 진입점, Compose 테마, 내비게이션
- `core/`: OpenAPI 생성 코드, Retrofit·OkHttp, 공통 오류·화면 상태
- `feature/property/`: 매물 data/domain/presentation 계층과 화면·테스트
- `build/generated/`: OpenAPI Generator가 만드는 코드. 직접 수정하거나 Git에 올리지 않습니다.
- 글래스 SDK 연동 코드는 최상위 `glasses/android-integration/`에서 관리합니다.

기능 의존 방향은 `UI → ViewModel → UseCase → Repository → RemoteDataSource`입니다. 서버 Entity나 HTTP 응답을 화면에서 직접 사용하지 않습니다.

## 개발 환경

- JDK 21로 Gradle 실행
- Kotlin/JVM target 17
- Android SDK 35, minSdk 24
- Jetpack Compose Material 3, Navigation Compose
- Hilt
- Retrofit, OkHttp, Moshi
- OpenAPI Generator

버전은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)에서 한곳에 관리합니다.

## 실행과 검증

Android Studio에서 `android/` 폴더를 프로젝트로 열거나 PowerShell에서 실행합니다.

```powershell
cd android
.\gradlew.bat :app:assembleDebug testDebugUnitTest lintDebug
```

UI 계측 테스트는 연결된 에뮬레이터 또는 기기가 있을 때 실행합니다.

```powershell
.\gradlew.bat :feature:property:connectedDebugAndroidTest
```

Debug 앱의 기본 API 주소는 Android 에뮬레이터용 `http://10.0.2.2:8080/api/v1/`입니다. 실제 기기에서는 개발 PC의 같은 네트워크 IP를 다음처럼 주입합니다.

```powershell
.\gradlew.bat :app:installDebug -PTENANT_LEAF_DEBUG_API_BASE_URL=http://192.168.0.10:8080/api/v1/
```

Release API 주소는 배포 환경에서 `TENANT_LEAF_RELEASE_API_BASE_URL`로 별도 주입해야 하며 localhost나 에뮬레이터 주소를 사용하지 않습니다. 값을 주입하지 않으면 안전한 `.invalid` 주소를 사용해 실수로 개발 서버에 연결하지 않습니다.

## 현재 범위

- 매물 목록·등록·상세·수정·삭제
- 금액은 원 단위 정수, 면적은 서버에 ㎡로 저장하고 앱에서 평으로 변환
- 로딩·빈 결과·검증·네트워크·서버 오류 상태
- 현재 서버의 고정 데모 사용자 흐름. 실제 토큰이 생길 때 교체할 인증 경계만 마련

로그인, 임장, 체크리스트, 촬영, 미디어 업로드, AI 분석은 아직 앱에서 호출하지 않습니다.

## MVP 촬영 원칙

- 실시간 스트리밍 AI 분석은 하지 않습니다.
- 원본 임장 영상은 사용자의 휴대전화 갤러리에 보관하고 서버에 올리지 않습니다.
- 기본 영상 녹화 중 정지 사진 동시 생성이 실기기에서 검증되면 사용합니다.
- 동시 생성이 지원되지 않으면 촬영 완료 영상에서 2~3초 간격으로 JPEG를 추출합니다.
- 서버에는 분석용 JPEG와 원본 영상 내 시점 메타데이터만 전송합니다.
