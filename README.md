# 세입세잎

Meta AI Glasses를 활용해 청년 임대주택 현장 점검을 보조하는 Android MVP입니다.

## 개발 구조

```text
android/                      Android 앱과 Android 전용 Gradle 설정
  app/                         앱 시작점과 의존성 조립
  feature/                     매물, 현장점검, 체크리스트, 보고서, 설정 화면
  core/                        공통 UI, 네트워크, 로컬 저장소, 권한, 상태 관리

server/                       서버 실행 단위와 서버 인프라
  api/                         Kotlin + Spring Boot API
  ai-worker/                   비동기 AI 분석 작업자
  infra/                       Docker, 환경별 설정, 배포와 모니터링

ai/                           AI 학습·평가 코드와 데이터셋 메타데이터
  ml/

glasses/                      Meta 안경 연동과 SDK 어댑터
  android-integration/         Android 앱에 연결되는 안경 기능
  adapter/                     특정 SDK를 격리하는 공통 인터페이스

design/                       Figma/PDF 등 디자인 산출물
packages/                     앱·서버·AI 공통 계약과 점검 설정
docs/                         기획, API, 데이터 모델, AI 평가와 QA 문서
mds/, Tasks/                  작업 요청서와 역할별 작업 현황
```

## 통신 원칙

```text
Meta Glasses -> Android 앱 -> API 서버 -> AI Worker
                                  ^             |
                                  +-------------+
```

- 안경은 서버와 직접 통신하지 않습니다. Android 앱이 권한, 촬영, 연결 상태, 음성 출력을 책임집니다.
- API 서버는 데이터 저장과 작업 요청을 맡고, 무거운 AI 분석은 `ai-worker`에서 비동기로 처리합니다.
- AI는 관찰 후보와 근거만 제공합니다. 체크리스트의 최종 상태는 사용자가 앱에서 확정합니다.
- 앱과 서버가 함께 사용하는 상태값과 JSON 형식은 `packages/shared-types`에서 먼저 합의합니다.

## Git 작업 규칙

- `main`: 데모 가능한 통합 상태만 유지합니다.
- `feature/android-*`, `feature/api-*`, `feature/ai-*`, `feature/glass-*`: 기능별 작업 브랜치입니다.
- PR에는 변경한 API 계약, 테스트 방법, Mock Device Kit 또는 실제 기기 확인 여부를 기록합니다.
- 비밀키, 실제 촬영 미디어, `tmp/` 생성물은 커밋하지 않습니다.

## 시작 순서

1. `packages/shared-types`에 임장·체크리스트·AI 탐지 결과 형식을 정의합니다.
2. `server/api`에 해당 API와 데이터 저장을 구현합니다.
3. `android`에서 API를 연결하고, `glasses/`에 Meta SDK 어댑터를 붙입니다.
4. `server/ai-worker`가 분석 작업과 결과 저장을 담당하게 연결합니다.

## 문서와 작업 관리

- `docs/`: 제품·기술·정책의 기준 문서
- `docs/team/`: 역할별 시작 안내와 작업 순서
- `Tasks/`: 전체 일정과 역할별 체크리스트
- `mds/`: 구현·수정 요청서, 완료 기준, 작업 결과 기록
- `design/`: 디자인 파일·참고 자료·에셋

자세한 제품 범위와 API 초안은 `docs/`를 확인하세요.
