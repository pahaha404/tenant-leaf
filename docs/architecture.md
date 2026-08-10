# 개발 아키텍처

## 배포 단위

| 단위 | 경로 | 배포 방식 | 책임 |
| --- | --- | --- | --- |
| Android 앱 | `apps/android` | Android APK/AAB | 화면, 사용자 입력, 권한, 현장 점검 상태 |
| 안경 연동 | `apps/android/glass` | Android 앱에 포함 | Meta SDK, 카메라/사진, 터치, 배터리, 음성 출력 |
| API 서버 | `services/api` | 컨테이너 | 인증, 매물/세션/체크리스트 저장, 업로드, 결과 제공 |
| AI 작업자 | `services/ai-worker` | 별도 컨테이너 | 비동기 사진 분석, STT, 보고서 초안 생성 |

`packages/glass-adapter`는 앱의 다른 코드가 Meta SDK에 직접 의존하지 않게 하는 인터페이스 계층입니다. 실제 SDK 구현은 Android의 `glass/`가 담당합니다.

## 요청 흐름

1. Android 앱이 방문 세션을 시작하고 API 서버에서 `VisitSession`을 생성한다.
2. 앱이 Meta 안경을 제어해 사진 또는 영상 프레임을 받는다.
3. 앱은 API 서버에서 발급한 업로드 주소로 미디어를 올린다.
4. API 서버는 AI 작업자에 분석 작업을 요청한다.
5. AI 작업자는 관찰 후보, 근거 미디어, 신뢰도와 처리 상태를 저장한다.
6. 앱은 결과를 보여주고 사용자가 체크리스트 상태를 최종 확정한다.

## 경계 규칙

- 안경은 서버에 직접 접속하지 않는다.
- 앱은 AI 제공자 API 키를 보관하지 않는다.
- API 서버의 HTTP 요청 안에서 무거운 AI 분석을 끝내지 않는다.
- AI 결과는 `AIObservation`이며, `ChecklistResult`의 최종 판정이 아니다.
- 계약 변경은 `packages/shared-types`와 `docs/api-spec.md`를 같은 변경 묶음에서 갱신한다.
