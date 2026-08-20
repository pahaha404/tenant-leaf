# Android 프로젝트 기본 구조 생성을 위한 기획서

> 상태: 실행 전 기획 0.1
> 기준일: 2026-08-18
> 대상 경로: `android/`
> 관련 요청서: `server/backendmds/10. Android 앱 개발 요청.md`

## 1. 목적

현재 안내용 폴더만 있는 `android/`를 Android Studio에서 열고 빌드·실행할 수 있는 Kotlin·Jetpack Compose 프로젝트로 만든다.

첫 구현 범위는 **Android 기본 구조와 매물 CRUD 서버 연결**까지다. 화면 디자인은 최소한으로 구성하고, 이후 `design/UI/UI.pen`의 디자인을 적용하더라도 네트워크·도메인 로직을 다시 만들지 않도록 계층을 분리한다.

## 2. 현재 상태

현재 `android/`에는 다음 파일만 있다.

- `settings.gradle.kts`
- `README.md`
- `app/src/main/kotlin/README.md`
- `app/src/main/res/README.md`
- `core/README.md`
- `feature/README.md`
- `android/Task.md`

아직 아래 항목이 없으므로 Android 앱을 빌드할 수 없다.

- Gradle Wrapper
- 프로젝트·모듈 `build.gradle.kts`
- 버전 카탈로그
- `:app` 모듈 등록
- `AndroidManifest.xml`
- 애플리케이션과 `MainActivity`
- Compose 테마·화면·내비게이션
- 네트워크와 의존성 주입 구성
- 단위·UI 테스트

## 3. 기준 문서와 우선순위

구현 중 판단 우선순위는 다음과 같다.

1. 루트 `AGENTS.md`
2. `server/backendmds/도메인 규칙.md`
3. `team/00_shared/공통 api 계약.md`
4. `server/shared-types/openapi/openapi.yaml`
5. `server/backendmds/10. Android 앱 개발 요청.md`
6. `design/UI/UI.pen`

UI는 작업 중이므로 시각적 디자인과 화면 배치는 나중에 교체할 수 있어야 한다. 최신 도메인 규칙과 UI가 충돌하면 도메인 규칙을 우선한다.

OpenAPI 검토 초안 2.0은 이전 체크리스트·Frame·Detection HTTP 계약을 제거했다. 현재 앱에서는 확정·구현된 **매물 API만 연결**하고, 임장 API는 서버 구현 후에 연결한다. Media·Observation·Report API는 남은 P0 요청·응답이 확정될 때까지 앱 기능에 사용하지 않는다.

## 4. 이번 작업 범위

### 포함

- Android Gradle 프로젝트와 Wrapper 구성
- Kotlin·Jetpack Compose 앱 실행
- 최소 앱 테마와 내비게이션
- 공통 상태·오류·네트워크 구조
- 의존성 주입 구조
- OpenAPI를 기준으로 한 매물 API 연결
- 매물 목록·상세·등록·수정·삭제용 최소 화면
- ㎡·평 변환과 입력 검증
- 단위 테스트, 네트워크 테스트와 Debug 빌드 검증
- 실행 방법과 검증 결과 문서화

### 제외

- 최종 디자인과 애니메이션
- 실제 회원가입·소셜 로그인·토큰 갱신
- 임장·체크리스트·미디어·AI 분석 API 연결
- Meta SDK와 실제 안경 연동
- 영상 촬영, 갤러리 가져오기와 JPEG 추출
- Room 기반 오프라인 저장
- WorkManager 백그라운드 업로드
- CameraX
- 푸시 알림, 음성·STT
- 매거진, 리포트, 비교와 개인 맞춤 추천
- 출시 서명과 스토어 배포

제외 기능을 위한 빈 API 호출, 임의 DTO 또는 가짜 완료 화면을 미리 만들지 않는다.

## 5. 기본 기술 구성

| 구분 | 선택 | 목적 |
|---|---|---|
| 언어 | Kotlin | Android 앱 코드 |
| UI | Jetpack Compose + Material 3 | 교체 가능한 최소 화면 |
| 빌드 | Gradle Kotlin DSL + Wrapper | 팀 공통 빌드 환경 |
| JDK | 21 | 저장소 공통 개발 환경 |
| 상태 관리 | ViewModel + StateFlow | 화면 상태 단방향 전달 |
| 비동기 처리 | Kotlin Coroutines | API 호출과 상태 전환 |
| 의존성 주입 | Hilt | 구현 교체와 테스트 용이성 |
| HTTP | Retrofit + OkHttp | Spring Boot API 연결 |
| 계약 기준 | OpenAPI Generator | 서버 계약과 Android 타입 불일치 방지 |
| 테스트 | JVM 단위 테스트, MockWebServer, Compose UI 테스트 | 계층별 검증 |

JSON 직렬화 라이브러리는 Android용 OpenAPI Generator 설정과 호환되는 하나만 선택한다. 같은 모델에 여러 직렬화 방식을 섞지 않는다.

AGP, Kotlin, Compose BOM, Hilt와 OpenAPI Generator의 정확한 버전은 구현 시작 시 설치된 Android Studio·SDK와 JDK 21 호환성을 확인한 뒤 `libs.versions.toml`에 고정한다. 검증하지 않은 최신 버전을 임의로 섞지 않는다.

## 6. SDK·앱 식별자 사전 결정

빌드 파일 생성 전에 다음 값을 확정한다.

- `applicationId`
- Kotlin·Android `namespace`
- `minSdk`
- `compileSdk`
- `targetSdk`
- 에뮬레이터 API 레벨

`minSdk`는 향후 Meta SDK가 요구하는 최소 버전을 확인한 뒤 결정한다. 이번 단계에서 임의의 값으로 확정한 뒤 나중에 되돌리지 않는다.

## 7. 모듈 구조

초기부터 기능 경계를 분명히 하되 과도한 모듈은 만들지 않는다.

```text
android/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
├─ gradle/
│  ├─ libs.versions.toml
│  └─ wrapper/
├─ app/                         # 앱 시작점과 의존성 조립
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ kotlin/.../app/
│     │  │  ├─ TenantLeafApplication.kt
│     │  │  ├─ MainActivity.kt
│     │  │  └─ navigation/
│     │  └─ res/
│     └─ androidTest/
├─ core/                        # 공통 모델·네트워크·오류·UI 기반
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/kotlin/.../core/
│     │  ├─ common/
│     │  ├─ model/
│     │  ├─ network/
│     │  └─ ui/
│     └─ test/
└─ feature/
   ├─ README.md
   └─ property/                 # 매물 기능 독립 모듈
      ├─ build.gradle.kts
      └─ src/
         ├─ main/kotlin/.../property/
         │  ├─ data/
         │  ├─ domain/
         │  └─ presentation/
         ├─ test/
         └─ androidTest/
```

Gradle에는 다음 세 모듈만 등록한다.

```kotlin
include(":app")
include(":core")
include(":feature:property")
```

기능이 실제로 추가될 때 `:feature:inspection`, `:feature:media`처럼 새 모듈을 만든다. 아직 구현하지 않는 기능의 빈 모듈은 만들지 않는다.

## 8. 의존성 방향

```text
:app
 ├─> :core
 └─> :feature:property
             └─> :core
```

- `:app`은 애플리케이션 시작, 전역 테마, 내비게이션과 의존성 조립만 담당한다.
- `:core`는 특정 화면에 종속되지 않은 공통 코드만 가진다.
- `:feature:property`는 매물 기능의 데이터·도메인·화면 상태와 Compose 화면을 가진다.
- `:core`는 `:app`이나 기능 모듈에 의존하지 않는다.
- 기능 모듈끼리 직접 의존하지 않는다.
- Meta SDK 구현은 나중에 `glasses/android-integration/`에 두고 앱에는 인터페이스만 노출한다.

기능 내부 호출 흐름은 다음을 따른다.

```text
Compose Screen
→ ViewModel
→ UseCase
→ Repository 인터페이스
→ Repository 구현
→ RemoteDataSource
→ OpenAPI 기반 HTTP Client
```

UI에서 Retrofit·OkHttp, 생성 DTO 또는 HTTP 상태 코드를 직접 사용하지 않는다.

## 9. 화면 상태 규칙

화면은 최소한 다음 상태를 구분한다.

```text
Idle
Loading
Success
Empty
ValidationError
NetworkError
ServerError
```

- ViewModel은 불변 `UiState`를 `StateFlow`로 제공한다.
- 일회성 알림과 내비게이션 결과는 반복 소비되지 않는 이벤트 구조로 분리한다.
- 요청 중 버튼 중복 입력을 막는다.
- 목록 일부 갱신 실패 때문에 이미 표시한 정상 데이터를 즉시 지우지 않는다.
- 오류 화면에는 재시도 동작을 제공한다.
- 서버의 안정적인 `ErrorResponse.code`로 분기하고 `message`는 사용자 표시용으로만 사용한다.

## 10. 환경과 네트워크 설정

### Debug

- Android 에뮬레이터 기본 URL: `http://10.0.2.2:8080/api/v1/`
- `INTERNET` 권한을 선언한다.
- 로컬 HTTP 허용은 Debug 빌드에만 적용한다.
- 실제 기기에서는 개발 PC의 같은 네트워크 IP를 별도 Debug 설정으로 주입한다.

### Release

- `localhost`, `10.0.2.2`와 개발 PC IP를 하드코딩하지 않는다.
- 운영 URL이 확정되지 않았으면 Release 빌드가 개발 서버를 향하지 않도록 한다.
- API 키, 토큰, 비밀번호와 실제 주소를 `BuildConfig`, 리소스나 Git에 넣지 않는다.

API 기본 URL은 빌드 변형별 설정에서 주입한다. 화면이나 Repository에 문자열로 반복 작성하지 않는다.

## 11. 인증 경계

OpenAPI에는 `POST /auth/demo`와 Bearer 인증이 선언되어 있지만 현재 Spring Boot 구현은 고정된 `DemoUserContext`로 매물 소유자를 처리하며 로그인 Controller는 아직 없다.

따라서 이번 단계에서는 다음 원칙을 따른다.

- 존재하지 않는 데모 로그인 API를 호출하지 않는다.
- 토큰을 임의로 생성하거나 영구 저장하지 않는다.
- 네트워크 계층에 교체 가능한 `AuthTokenProvider` 경계만 둔다.
- 현재 매물 CRUD는 서버의 현행 데모 사용자 흐름으로 검증한다.
- 인증 API가 구현되면 해당 Provider와 인터셉터만 교체한다.

API 계약과 서버 구현의 인증 차이는 작업 결과와 PR의 남은 위험에 기록한다.

## 12. OpenAPI 사용 방식

- 원본 계약은 `server/shared-types/openapi/openapi.yaml`이다.
- Android Gradle에 별도 OpenAPI 생성 작업을 구성한다.
- 생성 결과는 `android/**/build/generated/` 아래에 두고 Git에 커밋하지 않는다.
- 생성 코드는 직접 수정하지 않는다.
- 이번 단계에서는 `Properties` API와 이에 필요한 공통 모델만 앱에서 사용한다.
- 이전 체크리스트·임장·분석 타입이 함께 생성되더라도 Android 기능 코드에서 참조하지 않는다.
- 생성 DTO는 화면으로 전달하지 않고 Android 도메인 모델로 변환한다.
- OpenAPI 변경 시 코드 생성과 컴파일이 자동으로 다시 실행되게 한다.

Android용 생성기 설정이 확정되기 전 임시 DTO를 여러 위치에 복제하지 않는다.

## 13. 매물 계약과 앱 모델

### 필드 규칙

| 필드 | Android 타입·처리 |
|---|---|
| `id` | UUID 문자열 또는 전용 ID 타입 |
| `name` | 필수, 공백 제거 후 빈 값 금지 |
| `addressSummary` | 선택 문자열 |
| `depositAmount` | 원 단위 `Long`, 0 이상 |
| `monthlyRentAmount` | 원 단위 `Long`, 0 이상 |
| `maintenanceFeeAmount` | 원 단위 `Long`, 0 이상 |
| `areaSquareMeters` | ㎡ 기준 `Double`, 0보다 큼 |
| `floor` | 선택 문자열 |
| `options` | 중복 없는 문자열 목록 |
| `brokerContact` | 선택 참고 정보 |
| `note` | 선택 사용자 메모 |
| `createdAt`, `updatedAt` | 서버 시각을 파싱해 표시용으로 변환 |

- 서버 저장 기준은 ㎡다.
- 평 표시는 `㎡ ÷ 3.305785`, 평 입력은 `평 × 3.305785`로 변환한다.
- 표시 반올림과 서버 전송 정밀도를 분리해 반복 변환 오차를 줄인다.
- 목록 API의 `page`는 0부터 시작하고 기본 `size`는 20을 사용한다.
- PATCH에서는 필드 미전송과 명시적 `null`의 의미가 다르므로 생성 모델의 직렬화 결과를 테스트한다.
- 삭제 후 복원 가능성을 앱이 임의로 가정하지 않는다.

## 14. 최소 화면과 내비게이션

디자인 확정 전에는 아래 기능 확인용 화면만 만든다.

```text
PropertyList
 ├─> PropertyCreate
 └─> PropertyDetail
       └─> PropertyEdit
```

- 목록: 로딩, 빈 목록, 매물 카드, 새로고침, 등록 이동
- 등록: 필수·선택값 입력, ㎡·평 전환, 저장
- 상세: 서버 응답 전체 표시, 수정·삭제 이동
- 수정: 기존값 표시, 변경 필드만 PATCH
- 삭제: 확인 대화상자, 성공 후 목록 복귀

색상, 간격, 아이콘과 카드 형태는 임시 Material 3 구성으로 제한한다. 서버 호출과 상태 처리는 재사용 가능한 ViewModel과 UseCase에 두어 나중에 Compose UI만 교체할 수 있게 한다.

## 15. 구현 순서

### 1단계: 사전 확인

- Android Studio, Android SDK와 JDK 21 확인
- `applicationId`, namespace와 SDK 버전 확정
- 현재 Git 브랜치와 변경 파일 확인
- 로컬 Spring Boot·PostgreSQL `UP` 확인

### 2단계: 빌드 기반

- Gradle Wrapper 생성
- 루트 빌드 파일과 버전 카탈로그 생성
- `:app`, `:core`, `:feature:property` 등록
- 각 모듈의 Debug 빌드 확인

### 3단계: 앱 진입점

- Manifest와 Application 구성
- `MainActivity`와 최소 Compose 테마 생성
- 내비게이션과 Hilt 구성
- 빈 시작 화면을 에뮬레이터에서 실행

### 4단계: 계약·네트워크

- OpenAPI Android 생성 작업 구성
- Debug API URL 주입
- Retrofit·OkHttp와 JSON 직렬화 구성
- 공통 `ErrorResponse`와 네트워크 오류 변환
- 데모 인증 경계 구성

### 5단계: 매물 기능

- 도메인 모델·Repository·UseCase 구현
- RemoteDataSource와 OpenAPI 모델 변환
- 목록·상세·등록·수정·삭제 ViewModel 구현
- 최소 Compose 화면 연결
- ㎡·평 입력·표시 변환 구현

### 6단계: 검증·문서화

- 단위·네트워크·UI 테스트
- 실제 로컬 서버와 에뮬레이터 CRUD 확인
- README에 실행 명령과 로컬 서버 주소 기록
- `android/android/Task.md`와 `공용 Task.md`에 실제 검증 결과 반영

## 16. 테스트 계획

### 단위 테스트

- ㎡·평 양방향 변환과 반올림
- 금액·면적·필수 이름 입력 검증
- API DTO와 도메인 모델 변환
- 서버 오류 코드와 앱 오류 상태 변환
- ViewModel의 Loading·Success·Empty·Error 전환
- PATCH의 미전송 필드와 명시적 null 구분

### 네트워크 테스트

- 매물 생성 `201`
- 목록·상세 조회 `200`
- 수정 `200`
- 삭제 `204`
- 잘못된 입력 `400`
- 없는 매물 `404`
- 서버 장애와 연결 실패
- 페이지 번호·크기와 빈 목록

### 에뮬레이터 통합 확인

1. PostgreSQL과 Spring Boot 서버 실행
2. `/actuator/health`의 `UP` 확인
3. Android 앱 실행
4. 매물 등록
5. 목록·상세 조회
6. 수정 후 변경값 재조회
7. 삭제 후 목록에서 제거 확인
8. 앱 재실행 후 서버 저장 상태 확인

## 17. 기본 검증 명령

PowerShell에서 다음 명령을 사용한다.

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:testDebugUnitTest :feature:property:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

에뮬레이터가 준비된 경우 다음 검증을 추가한다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

실제 생성한 모듈의 테스트 task 이름이 다르면 Gradle task 목록을 확인해 문서와 명령을 함께 수정한다.

## 18. 완료 기준

- JDK 21 환경에서 Gradle Wrapper가 실행된다.
- `:app`, `:core`, `:feature:property`가 컴파일된다.
- Debug APK가 생성되고 에뮬레이터에서 실행된다.
- 매물 등록·목록/상세 조회·수정·삭제가 로컬 서버와 동작한다.
- ㎡·평 변환, 입력 검증, 모델 변환과 ViewModel 테스트가 통과한다.
- 네트워크 실패와 서버 오류 상태가 앱에서 구분된다.
- OpenAPI 생성 코드를 직접 수정하거나 Git에 추가하지 않는다.
- UI 교체가 네트워크·도메인 계층 변경을 요구하지 않는다.
- 비밀번호, 토큰, 실제 주소와 촬영 자료가 Git과 로그에 없다.
- 실시간 분석, 원본 영상 업로드와 체크리스트 중심 임장 코드를 만들지 않는다.
- README와 Task 문서에 실제 실행·검증 결과가 기록된다.

## 19. 생성·수정 예상 파일

```text
android/settings.gradle.kts
android/build.gradle.kts
android/gradle.properties
android/gradle/libs.versions.toml
android/gradlew
android/gradlew.bat
android/gradle/wrapper/*
android/app/build.gradle.kts
android/app/src/main/AndroidManifest.xml
android/app/src/main/kotlin/**
android/app/src/main/res/**
android/app/src/androidTest/**
android/core/build.gradle.kts
android/core/src/main/kotlin/**
android/core/src/test/**
android/feature/property/build.gradle.kts
android/feature/property/src/main/kotlin/**
android/feature/property/src/test/**
android/feature/property/src/androidTest/**
android/README.md
android/android/Task.md
공용 Task.md
```

다음 로컬 파일과 생성물은 커밋하지 않는다.

```text
android/local.properties
android/.gradle/
android/**/build/
*.jks
*.keystore
실제 API 키·토큰·촬영 파일
```

## 20. 남은 결정과 위험

| 항목 | 처리 방향 |
|---|---|
| 앱 식별자와 SDK 버전 | 구현 전에 팀이 확정 |
| Android용 OpenAPI 생성 세부 설정 | 매물 API 생성·컴파일로 검증 후 고정 |
| OpenAPI의 이전 체크리스트 계약 | `09` 요청 완료 전 Android에서 사용 금지 |
| 데모 로그인 계약과 서버 구현 차이 | 교체 가능한 인증 경계만 만들고 위험 기록 |
| 작업 중인 `UI.pen` | UI를 얇게 유지하고 디자인 파일은 수정하지 않음 |
| Meta SDK 지원 범위 | 이번 단계 제외, 실기기 검증 전 가정 금지 |
| 실제 기기 서버 주소 | Debug 설정으로 주입, 소스 하드코딩 금지 |

이 기획서는 Android 프로젝트 기본 구조를 생성하기 위한 기준이다. 범위를 넘는 API나 기능이 필요해지면 먼저 도메인 규칙·OpenAPI와 작업 요청서를 확정한 뒤 별도 변경으로 진행한다.
