# 세입세잎 전체 작업 현황

기준일: 2026-08-14

## 이 폴더를 쓰는 방법

- 이 파일은 전체 일정, 통합 완료 기준, 완료 기록만 관리한다.
- 역할별 세부 체크리스트는 아래 역할 폴더의 `Task.md`에서 관리한다.
- 개인이 맡은 작은 조사·작업 메모는 `personal/<github-id>/Task.md`에서 관리한다.
- 구현 전에 `../mds/active/`의 작업 요청서를 읽는다.

| 역할 | 체크리스트 |
| --- | --- |
| Android | `android/Task.md` |
| Meta 글라스 | `glasses/Task.md` |
| 백엔드 | `backend/Task.md` |
| AI·ML | `ai-ml/Task.md` |
| PM·통합 | `pm-integration/Task.md` |

## 작업 규칙

- 완료가 확인된 작업만 `[x]`로 바꾼다.
- 완료 처리할 때는 아래 `완료 기록`에 날짜, 결과, 확인 근거를 한 줄로 남긴다.
- 구현하지 않은 아이디어, 진행 중인 작업, 확인하지 않은 결과는 `[ ]`로 유지한다.
- 새 작업은 해당 주차 또는 관련 기능 묶음에 추가한다.

## 8월 10일~16일: 범위와 설계 잠금

- [x] 8월 10일 MVP 계획 문서 정리
- [ ] MVP P0 / P1 / 제외 범위 확정
- [ ] 지원 플랫폼 1개와 AI 글래스 모델 1개 확정
- [ ] 안경 카메라·스피커·마이크·터치 연동 가능 여부 검증
- [ ] 녹음 모드와 음성 안내 모드의 동시 사용 제약 검증
- [ ] 방문 전 안심 가이드 콘텐츠 확정
- [ ] AI 구역·관찰 유형과 비확정 표현 확정
- [x] 매물 최소 데이터 구조 확정
- [ ] 임장·구역·미디어·관찰·근거 중심 최소 데이터 구조의 남은 P0 계약 확정
- [x] 실시간 스트리밍 분석 제외와 기본 영상 촬영·JPEG 분석 흐름 문서·계약 반영
- [ ] 전체 사용자 흐름과 정보 구조 확정
- [ ] 로우파이 화면 및 클릭 가능한 프로토타입 완성

## 8월 17일~23일: 디자인과 통합 알파

- [ ] 핵심 화면 하이파이 UI와 디자인 시스템 완성
- [ ] AI 분석 결과 JSON 형식 및 화면 상태 매핑 확정
- [ ] 앱·안경·백엔드 통합 알파 빌드 완성
- [ ] 내부 사용성 테스트 후 P0 문제 수정
- [ ] 8월 23일 디자인 프리즈

## 8월 24일~31일: 현장 검증과 데모

- [ ] 실제 원룸 임장 테스트 2회 이상 완료
- [ ] 연결 끊김·저속 네트워크·권한 거부 시나리오 검증
- [ ] 리포트의 모든 문장에 사진 또는 사용자 입력 근거 연결 확인
- [ ] 튜토리얼과 데모용 샘플 매물·리포트 준비
- [ ] 8월 28일 기능 프리즈
- [ ] 전체 시나리오를 개발자 개입 없이 연속 3회 성공
- [ ] Blocker 버그 0건 확인 및 최종 데모 완료

## 완료 기록

- 2026-08-24 — 분석 완료 JPEG에서 임계값 이상 원시 탐지를 관찰로 투영하고 자동 리포트를 생성·조회하는 서버/Android 통합 코드를 구현했다. 픽셀 `xyxy` 다중 bbox와 선택 관찰 강조, 서명 근거 사진 조회, 부분 완료·빈 결과·오류 상태를 연결했으나 Gradle 플러그인 해석 제한으로 빌드·통합 검증 전까지 관련 완료 체크는 유지하지 않는다.

- 2026-08-20 — 서버와 DB의 로컬/실배포 설정을 분리했다. Android debug/release API 주소 계약을 유지하고, 서버 `prod` 프로필은 운영 DB·객체 저장소 환경변수를 필수로 요구하도록 구성했다. 서버 `clean test`는 통과했으며 실제 배포 환경 연결은 미검증이다.

- 2026-08-10 — `docs/team/pm/01_2026-08-10_MVP-계획.md` 작성 완료. MVP 범위, 사용자 흐름, UI/UX, 일정, 완료 조건을 기록함.
- 2026-08-11 — 백엔드 개발 도구 설치 확인 완료. `scripts/check-prerequisites.ps1`로 Temurin JDK·javac 21.0.12, Docker CLI와 Docker Desktop engine 29.7.2 실행을 확인함.
- 2026-08-11 — PostgreSQL 로컬 개발 환경 구성 완료. Docker Compose에서 PostgreSQL 17을 `healthy` 상태로 실행하고, 빈 볼륨 재생성 후 Flyway 마이그레이션과 `server/api`의 `clean test` 통과를 확인함. 헬스 체크는 DB 연결 시 `UP(200)`, 중지 시 `DOWN(503)`을 반환함.
- 2026-08-12 — 공통 API 계약 1.0 확정. 데모 로그인, 사용자→매물→임장 구조, 체크리스트 상태 4개, AI 라벨·bbox 형식, 프레임 규격과 보관 정책을 `team/00_shared/공통 api 계약.md`에서 확인함.
- 2026-08-12 — 확정된 HTTP API의 OpenAPI 3.0.3 명세 작성 완료. Redocly CLI와 OpenAPI Generator 7.24.0으로 `packages/shared-types/openapi/openapi.yaml` 문법과 참조 유효성을 확인함.
- 2026-08-13 — OpenAPI 기반 Spring Boot Kotlin 코드 생성 환경 구성 완료. OpenAPI Generator 7.24.0으로 요청·응답 타입과 API 인터페이스를 생성하고 `server/api`의 `clean test` 통과를 확인함.
- 2026-08-13 — 공통 API 계약 1.1에 사용자가 직접 입력하는 매물 조건 7개를 추가함. 보증금·월세·관리비·전용면적·층수·옵션·부동산 연락처의 OpenAPI Kotlin 타입 생성과 `server/api`의 `clean test` 통과를 확인함.
- 2026-08-13 — 프로젝트 폴더를 Android·서버·AI·글래스 중심으로 정리함. Android Gradle 설정을 `android/`으로 옮기고 경로 참조를 갱신함.
- 2026-08-13 — 매물 면적의 ㎡·평 전환 규칙 확정. 서버는 ㎡만 저장하고 Android 앱이 `1평 = 3.305785㎡` 기준으로 입력·표시를 변환하도록 API 계약과 UI 요구사항에 기록함.
- 2026-08-13 — 매물 CRUD API 구현 완료. PostgreSQL 마이그레이션과 Kotlin Controller·Service·Repository를 구성하고 등록·목록/상세 조회·부분 수정·삭제·소유권·오류 응답을 `server/api`의 `clean test`로 확인함.
- 2026-08-14 — 최신 UX 변경을 반영한 서버 도메인 규칙 초안 0.2를 작성함. 현장 체크리스트 중심 구조를 구역 분류·미디어 분석·확인 필요 관찰·근거 미디어·리포트 구조로 전환하고 기존 API·UI의 교체 대상과 미확정 계약을 기록함.
- 2026-08-14 — 실시간 스트리밍 분석을 MVP에서 제외하고 안경 기본 고화질 영상→휴대전화 갤러리→촬영 중 생성 또는 촬영 후 추출한 JPEG→비동기 분석 흐름으로 문서와 API 계약 1.2를 갱신함. OpenAPI 문법 검사·Kotlin 코드 생성과 `server/api`의 `clean test` 통과를 확인함.

- 2026-08-18 Android 앱을 `android/app`으로 이전하고 `:app:assembleDebug` 빌드 성공을 확인했다. 실제 에뮬레이터 흐름은 새 경로에서 재확인 필요.
- 2026-08-18 — Android 기본 구조를 `:app`, `:core`, `:feature:property`로 구성하고 Compose·Navigation·Hilt·OpenAPI 기반 매물 CRUD를 연결함. OpenAPI 검사·생성, clean Debug 빌드, JVM 테스트 24개, Lint와 Galaxy SM-G991N(Android 15) Compose UI 테스트 2개를 통과하고 에뮬레이터에서 서버를 통한 등록·조회·수정·삭제까지 확인함.
- 2026-08-18 — 공통 API 계약을 체크리스트·Frame·Detection 중심 1.2에서 구역·Media·Observation·근거 중심 검토 초안 2.0으로 전환함. 미확정 Media·Observation·Report HTTP API는 OpenAPI에서 제외하고, 서버 OpenAPI 검사·Kotlin 생성·계약 테스트와 Android clean Debug 빌드·단위 테스트·Lint 통과를 확인함.
- 2026-08-19 — 확정된 임장 생명주기 API의 PostgreSQL·Spring Boot 구현을 추가함. 임장 생성·목록·상세·종료·취소, 소유권, 단방향 상태 전이와 임장이 있는 매물 삭제 보호를 별도 테스트 스키마에서 검증함. 미디어·관찰·리포트와 임장 보관 API는 미확정 범위로 유지함.
- 2026-08-19 — JPEG 미디어 업로드 HTTP 계약을 검토 초안 2.1로 확정함. 요청당 1~20장 배치 등록, 15분 서명 URL, 업로드 완료·재시도·목록·상세와 멱등성 충돌을 OpenAPI에 반영하고 Kotlin API 인터페이스 생성·계약 테스트를 통과함. 임장당 전체 상한과 미디어 집합 확정 API는 P0로 유지함.

## 진행 기록

- 2026-08-24 — Android 팀 공용 디버그 서명 키(`debug.keystore`) 설정(`signingConfigs.debug`)을 적용함. 팀원 각자의 PC 키 해시 등록 번거로움을 제거하고 `local.properties` 공유만으로 카카오 맵 SDK가 모든 기기에서 즉시 정상 렌더링되도록 구성함. `:app:assembleDebug`, `:app:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — Android 내 정보 화면 로그아웃 기능(다이얼로그 확인 및 토큰/환경설정 초기화, 로그인 이동)과 매물 보증금·월세 금액의 한국 단위(억/만) 자동 포맷팅(`formatKoreanAmount`)을 적용함. `:app:testDebugUnitTest`, `:feature:property:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 로컬 Android Debug 서버 연결이 끊긴 원인이 서버 중지가 아니라 USB `adb reverse` 포트 전달 누락임을 확인해 복구함. 오프라인 에뮬레이터가 있어도 연결된 실제 기기를 골라 `8080`·`9000` 포트를 전달하도록 빌드 설정을 보완함.

- 2026-08-24 — JPEG 업로드 요청의 `zone` 필수 계약이 OpenAPI 원본에서 누락돼 Android 요청과 서버 생성 모델이 어긋난 문제를 수정함. Android는 구역 확정 전 `UNKNOWN`을 보내고, 서버는 같은 값을 저장·재시도 비교에 포함함. OpenAPI 검증·Android/서버 Kotlin 컴파일을 확인했으며, 테스트 런처의 기존 `ClassNotFoundException`은 별도 해결이 필요함.

- 2026-08-24 — Android 점검 플로우 및 매물 관리 UI/UX 대규모 고도화(`feature/uiux-better`). 매물 지도 오버뷰 핀 연동 및 당겨서 새로고침(Pull-to-Refresh), 휴지통 기반 다중 선택 일괄 삭제, 매물 상세 연필 아이콘 수정 모드, 점검 준비/경고 화면 하단 CTA 버튼 고정, TTS 음성 엔진 앱 기동 사전 예열을 통한 0ms 즉각 발화, 안경 미연결 시 스마트폰 카메라 자동 전환 및 실시간 점검 화면 카드 정리, 분석 진행 탭 내 수동 선택 제거 및 갤러리 영상 100% 자동 추출·업로드 파이프라인 단일화를 구현함. `:app:testDebugUnitTest`, `:feature:property:testDebugUnitTest`, `:feature:media:testDebugUnitTest`, `:feature:inspection:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — Android 리포트 UI를 완료·근거 사진·빈 결과·부분 완료·오류·생성 중 6개 상태로 구현하고 기존 리포트 생성 진행 플래그에 연결함. AI 결과는 `확인 필요 관찰`로 표현하고, 근거 사진의 정규화 bbox·신뢰도·구역을 전체화면에서 확인하도록 구성함.
- 2026-08-24 — Android 구역 관찰 화면의 하단 고정 CTA와 AI 관찰 비확정 안내 카드를 개선함. 54dp `매물 상세로 돌아가기` 버튼, 시스템 내비게이션 패딩, `#FFF0E4` 안내 카드, 아이콘+배지 상태 표현을 적용하고 Android `:app:testDebugUnitTest` 및 정적 진단을 통과함. 실기기 화면 위치와 실제 백스택 복귀는 미검증임.
- 2026-08-22 — Android 앱이 로그인 완료와 튜토리얼 완료 상태를 `SharedPreferences`에 저장해 재실행 시 로그인과 앱 소개 흐름을 건너뛸 수 있게 연결함. `:app:testDebugUnitTest`, `:app:assembleDebug`, Galaxy SM-G991N(Android 15) `:app:installDebug`를 통과했지만, 잠금 화면과 `run-as` 제한으로 저장 상태별 실기기 분기 화면은 아직 확인하지 못함.
- 2026-08-22 — Android 점검 시작 전 체크리스트 화면의 주요 버튼과 건너뛰기를 하단 액션 영역에 고정해 튜토리얼 화면과 동일한 정렬로 맞춤.
- 2026-08-22 — Android 튜토리얼 화면의 영상 보기·건너뛰기 동작을 공통 하단 액션 영역으로 옮겨 하단 정렬함. Debug 빌드와 Galaxy SM-G991N(Android 15) 설치를 통과했으나, 약관 동의 전 상태이므로 변경된 튜토리얼 화면의 실기기 위치는 아직 확인하지 않음.
- 2026-08-21 — Android 실기기 Debug 빌드가 `127.0.0.1`만 사용하도록 고정하고, Android Studio Debug 빌드 전 API·스토리지 `adb reverse`를 자동 복구하도록 구성함. Debug 주소 회귀 테스트를 추가해 에뮬레이터 전용 주소 재유입을 방지함.
- 2026-08-21 — Android 매물 목록에서 오른쪽에서 왼쪽으로 밀 때 카드 너비의 1/4만 열리는 삭제 버튼을 기존 API에 연결함. 반대로 밀거나 삭제를 취소하면 원위치로 돌아가며, 성공 시 목록에서 제거하고 실패 시 서버 오류를 표시함. 단위 테스트·Debug 빌드·Galaxy 설치와 실기기 1/4 열림·역방향 닫힘·확인창·취소 흐름을 검증했으며 실제 매물 삭제는 수행하지 않음.
- 2026-08-19 — Android JPEG 미디어 호출 흐름을 구현하고 단위·MockWebServer·UI 테스트를 추가함. 자동 실행 환경의 Gradle 파일 접근 제한으로 Android 빌드가 미검증이며, Meta 영상 가져오기와 실기기→API→MinIO→PostgreSQL 통합 확인 전까지 통합 알파 및 관련 Android 항목은 미완료로 유지함.
- 2026-08-20 — Android 현재 위치 주소가 정밀 권한에서 GPS를 우선하도록 보정하고, 동·호수용 상세 주소 입력을 추가함. 단위 테스트와 Debug 빌드를 통과했으며 GPS 좌표는 저장·로그·API 전송하지 않음.
- 2026-08-20 — `feature/map`에서 매물 등록 주소 입력창의 현재 위치 주소 교체를 구현함. 전경 위치 권한만 사용하고 GPS 좌표는 저장·로그·API 전송하지 않으며, Android 단위 테스트·Debug APK 빌드와 Galaxy SM-G991N(Android 15) 실기기 주소 입력을 확인함.
- 2026-08-20 — 최신 디자인 UI를 기준으로 OpenAPI 네트워크와 매물·임장·JPEG 미디어 기능을 선별 통합함. 기존 실험용 화면·내비게이션은 복원하지 않았으며, 자동 실행 환경의 Gradle 접근 제한으로 빌드·테스트·Lint 및 실기기 검증 전까지 통합 알파는 미완료로 유지함.
- 2026-08-20 — `feature/map`에서 매물 등록 주소창을 주소 검색과 현재 위치 지도 핀 선택으로 분리하고 상세 주소 입력을 유지함. GPS 신규 조회에 6초 제한과 최근 위치 fallback을 적용했으며, Android 단위 테스트·Debug APK 빌드와 Galaxy SM-G991N(Android 15)에서 검색 화면 이동, Kakao 지도 표시·드래그·주소 확정 복귀를 확인함. GPS 좌표는 저장·로그·API 전송하지 않음.
- 2026-08-21 — Kakao 지도 SDK를 2.15.1로 갱신하고 Compose `clip`이 SDK `SurfaceView`를 가리던 문제를 제거함. `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과하고 Galaxy SM-G991N(Android 15)에서 실제 지도 타일 표시, 중앙 핀 고정, 지도 드래그와 주소 갱신을 확인함.
- 2026-08-21 — Kakao 지도를 전용 `LocationPickerActivity`로 분리하고 SDK의 `resume`·`pause` 생명주기를 연결함. `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과하고 Galaxy SM-G991N(Android 15)에서 최초 진입, 재진입 3회, 백그라운드 복귀 후 실제 지도 타일 표시를 확인함.
- 2026-08-21 — 주소 검색 화면 제목을 "점검할 집의 주소를 입력하세요"로 변경하고 고정 높이에 잘리던 검색 입력값을 정상 표시하도록 수정함. Android 단위 테스트·Debug 빌드와 Galaxy SM-G991N(Android 15)에서 입력값 및 검색 결과 표시를 확인함.

- 2026-08-24 — 로컬 JPEG 사진 저장소 MinIO를 Docker 없이 사용자 로컬 도구 경로에서 실행하고 `http://127.0.0.1:9000/minio/health/live` HTTP 200, API의 JPEG 업로드 URL 발급 성공, 실제 Galaxy SM-S911N의 `adb reverse` 경유 9000 포트 연결을 확인함. 이 환경은 로컬 데모용이며 원본 영상은 계속 휴대전화에만 보관함.

- 2026-08-24 — 점검 음성 기록은 서버에 업로드하지 않고 휴대전화 내부에서만 매물별 최근 기록으로 연결하도록 구현함. 매물 상세에서 WAV 재생과 STT 핵심 내용·원문 확인이 가능하며, 실제 대화는 촬영 전 동의 절차를 마친 경우에만 기록한다. Android Debug Kotlin 컴파일과 Galaxy SM-S911N 설치를 통과했고, 실제 STT 결과 재생은 현장 점검으로 추가 확인이 필요함.

- 2026-08-24 — 촬영 종료 확인 화면의 구역별 촬영 상세 목록을 제거하고, 결과 안내 문구를 사용자가 사진을 확인한 뒤 직접 결정하도록 바꿈. 세입세잎 캐릭터 원본 PNG를 Android 기본·원형 앱 아이콘으로 적용했으며, Debug APK 빌드와 Galaxy SM-S911N(Android 16) 설치를 통과함.

- 2026-08-24 — 앱 실행 뒤 보이는 시작 화면에서 집 아이콘을 세입세잎 캐릭터로 교체하고 크게 표시함. `세입세잎`과 `초보 세입자를 위한 SAFE GUIDE` 문구를 함께 유지했으며, Debug APK 빌드·Galaxy SM-S911N(Android 16) 설치와 실제 시작 화면 표시를 확인함.

- 2026-08-24 — 매물별 음성 요약은 별도 페이지에서 핵심 내용을 먼저 보여주고, 사용자가 `전체 STT 보기`를 눌렀을 때만 원문을 표시하도록 변경함. 비어 있는 STT 결과는 기존 녹음 파일로 다시 변환할 수 있으며, 최신 변환 결과가 매물 상세에 즉시 반영되도록 저장 상태를 연결함. Debug APK 빌드와 Galaxy SM-S911N(Android 16) 설치를 통과했고 실제 새 점검 STT 결과 검증은 남아 있음.

- 2026-08-24 — Android 시스템 시작 화면의 별도 아이콘 표시를 투명 아이콘과 브랜딩 화면과 같은 배경으로 통합해, 앱 실행 시 캐릭터·제목·부제가 있는 Compose 로딩 화면만 눈에 띄도록 수정함. Debug APK 빌드와 Galaxy SM-S911N(Android 16) 설치를 통과함.

- 2026-08-24 — 매물 연결 정보 없이 남아 있던 기존 기기 내 음성 녹음은 음성 요약 화면에서 최근 미연결 WAV를 현재 매물에 연결한 뒤 Android STT를 자동 재시도하도록 보완함. 원본 음성과 텍스트는 계속 휴대전화 안에만 보관하며, Debug APK 빌드·Galaxy SM-S911N(Android 16) 설치를 통과함.

- 2026-08-24 — 기존 기기 내 음성 녹음 연결·STT 재시도는 매물 상세 카드 진입 시점에도 자동 실행하도록 보완함. Debug APK 빌드와 Galaxy SM-S911N(Android 16) 설치를 통과했으며, 실제 음성 인식 결과는 해당 매물 상세를 다시 연 뒤 확인이 필요함.

- 2026-08-24 — Galaxy 기본 STT의 PCM 입력 버퍼 초과를 확인해, 녹음 파일을 한 번에 보내지 않고 실제 녹음 속도에 맞춰 전달하도록 변경함. 45초 타임아웃도 추가했으며, Debug APK 빌드와 Galaxy SM-S911N(Android 16) 설치를 통과함. 실제 STT 원문 표시 검증은 다음 재시도에서 필요함.
