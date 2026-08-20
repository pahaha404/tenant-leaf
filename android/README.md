# android

Kotlin과 Jetpack Compose로 만드는 Android 네이티브 앱 폴더입니다.

## 책임

- 로그인과 프로젝트·매물 관리 화면
- 안경 기본 영상 촬영 안내, 휴대전화 갤러리 가져오기와 분석용 JPEG 생성
- 2분 간격 미완료 점검 항목 안내
- AI 분석 결과 확인·수정
- 상태 등급과 매물 비교

## 예정 구조

- `app/src/main/kotlin/`: Kotlin 앱 코드
- `app/src/main/res/`: Android 리소스, 아이콘, 문자열
- `feature/`: 기능별 Compose 화면·ViewModel·UseCase
- `core/`: 공통 UI·네트워크·DB·권한·디자인 시스템
- 글래스 SDK 연동 코드는 최상위 `glasses/android-integration/`에서 관리

## 권장 Android 기술

- Kotlin
- Jetpack Compose
- ViewModel + StateFlow
- Hilt
- Retrofit 또는 Ktor Client
- Room
- WorkManager
- CameraX

## MVP 촬영 원칙

- 실시간 스트리밍 AI 분석은 하지 않습니다.
- 원본 임장 영상은 사용자의 휴대전화 갤러리에 보관하고 서버에 올리지 않습니다.
- 기본 영상 녹화 중 정지 사진 동시 생성이 실기기에서 검증되면 사용합니다.
- 동시 생성이 지원되지 않으면 촬영 완료 영상에서 2~3초 간격으로 JPEG를 추출합니다.
- 서버에는 분석용 JPEG와 원본 영상 내 시점 메타데이터만 전송합니다.
