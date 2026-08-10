# android

Kotlin과 Jetpack Compose로 만드는 Android 네이티브 앱 폴더입니다.

## 책임

- 로그인과 프로젝트·매물 관리 화면
- 현장 촬영과 음성 메모
- 2분 간격 미완료 점검 항목 안내
- AI 분석 결과 확인·수정
- 상태 등급과 매물 비교

## 예정 구조

- `app/src/main/kotlin/`: Kotlin 앱 코드
- `app/src/main/res/`: Android 리소스, 아이콘, 문자열
- `feature/`: 기능별 Compose 화면·ViewModel·UseCase
- `core/`: 공통 UI·네트워크·DB·권한·디자인 시스템
- `glass/`: 글래스 카메라·마이크·TTS SDK 연동

## 권장 Android 기술

- Kotlin
- Jetpack Compose
- ViewModel + StateFlow
- Hilt
- Retrofit 또는 Ktor Client
- Room
- WorkManager
- CameraX


