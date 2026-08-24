# Android 프런트엔드 Task

## 감사 후 수정 기록

- 2026-08-20 — Meta 공식 DAT Android 가이드에 맞춰 등록 오류 스트림, 콜백 URI, 구형 Bluetooth 권한, DAT 앱 업데이트 안내와 종료 원인별 재연결 제한을 반영했다. `:app:testDebugUnitTest` 통과. 실제 Meta AI 등록·안경 세션·업데이트 이동은 실기기 미검증이다.
- 2026-08-20 — AI 분석 완료로 오인될 수 있던 8초 타이머·완료 알림과 고정 분석 진행 화면을 운영 경로에서 제거했다. JPEG 미제출 시 `MEDIA_REQUIRED` 상태만 표시하도록 변경했고, `:app:testDebugUnitTest` 2/2 및 `:app:assembleDebug` 성공으로 확인했다. 실제 JPEG 업로드·서버 AI 연동·실기기는 미검증이다.
- 2026-08-20 — DAT 세션이 중단되면 프리뷰를 정리하고 1·3·6초 간격으로 최대 3회 재연결하도록 보강했다. 중복 세션·프리뷰 시작을 차단하고, 점검 화면 이탈·앱 백그라운드에서 프리뷰를 종료한다. `:app:testDebugUnitTest` 4/4, `:app:installDebug` 및 Galaxy SM-G991N 실행을 확인했다. Meta AI/DCT 서비스의 실제 채널 복구와 안경 재연결은 미검증이다.

- [x] Android Studio·JDK 21 설치와 빈 앱 빌드 확인
- [ ] 앱 모듈과 공통 디자인 토큰 연결
- [ ] 하단 내비게이션과 기본 화면 이동 구현
- [ ] 매물 목록·등록·상세 화면 구현
- [x] 매물 등록 주소 검색·상세 주소 입력·현재 위치 지도 핀 선택 구현
- [ ] 매물 면적 ㎡·평 입력·표시 전환 구현
- [ ] 방문 준비·세션 시작 화면 구현
- [ ] 안경 기본 영상 촬영·종료 안내와 휴대전화 갤러리 가져오기 확인 화면 구현
- [ ] 기본 영상 녹화 중 정지 사진 동시 생성 가능 여부 실기기 검증
- [ ] 동시 생성 미지원 시 촬영 완료 영상의 2~3초 간격 JPEG 추출·품질 선별 구현
- [ ] 촬영 종료 후 JPEG 업로드·분석 진행·실패 재시도 화면 구현
- [ ] 구역별 분석 상태와 관찰 열람·리포트 제외 UI 구현
- [ ] AI 관찰 결과·근거 사진·재촬영 안내 UI 구현
- [ ] 보고서·매물 비교 화면 구현
- [ ] 로딩·오류·권한 거부·안경 연결 실패 화면 구현
- [ ] API 연결과 에뮬레이터 확인

완료 기록은 루트 `공용 Task.md`에도 영향을 확인한 뒤 작성한다.

## 진행 기록

- 2026-08-24 — 실시간 점검 중 체크리스트 가이드 확인 시 녹화 세션 중단 버그 수정. 외부 라우트 이동으로 `LiveInspectionScreen` 컴포저블이 언마운트되며 레코더(`InspectionVideoRecorder`)가 강제 닫히던 문제를 해결하고, 화면 내부 모달 오버레이 다이얼로그(`activeGuideIndex`) 방식으로 변경하여 가이드 확인 후 되돌아오더라도 끊김 없이 녹화 및 프리뷰가 연속 유지되도록 수정함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실기기 가이드 모달 터치는 실기기 미검증임.
- 2026-08-24 — 점검 비디오 레코더 표준 9:16 세로 해상도(`720x1280`) 복구 및 Center-Crop 렌더링 유지. 갤러리 재생 시 좌우 왜곡(뚱뚱해지는 현상)을 방지하고 스마트폰 화면에 자연스럽게 꽉 차도록 표준 해상도로 복원함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실기기 갤러리 재생 비율은 실기기 미검증임.
- 2026-08-24 — 실시간 점검 카메라 스트리밍 제어 기능 추가. 일시정지/재개 시 프리뷰 정지/재개, 타이머 일시정지 및 TTS 음성 피드백 연동, 동적 스트림 화질 수동 선택 칩 및 드롭다운(`HIGH`/`MEDIUM`/`LOW`), Meta AI 안경 ↔ 스마트폰 카메라(`PhoneCameraPreviewHelper`) 수동 소스 전환 버튼을 상단 바에 구현함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 스마트폰 카메라 실제 하드웨어 센서 및 실기기 안경 전환은 실기기 미검증임.
- 2026-08-24 — 점검 종료 시 음성 안내(TTS) 구현. 촬영 종료 팝업의 `[네, 종료할게요]` 버튼 클릭 시 "촬영을 종료합니다. 해당 영상을 업로드해주세요." 음성이 출력되도록 연결하고, 화면 내비게이션 시에도 음성이 끊기지 않도록 `VoiceGuideManager` 싱글톤을 도입함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실기기 TTS 발화 음량 및 하드웨어 스피커 출력은 실기기 미검증임.
- 2026-08-24 — Meta AI 안경 실시간 카메라 스트리밍 프리뷰 복원 및 최적화. `com.meta.wearable:mwdat-camera:0.9.0` 의존성 추가, HEVC 하드웨어 비디오 디코더(`HevcDecoder`), VPS/SPS/PPS 파라미터 수집기(`HevcParameterSetCollector`), 전역 세션 공유 풀(`rememberGlassConnectionViewModel`), Center-Crop 비율 맞춤 Matrix Transform 및 고화질(VideoQuality.HIGH)/30fps/2.5Mbps 설정을 적용함. `:app:testDebugUnitTest` 144개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실제 Meta AI 안경 실기기 연동은 실기기 미검증임.
- 2026-08-22 — 로그인 완료와 튜토리얼 완료 여부를 앱 `SharedPreferences`에 저장해 재실행 시 로그인은 건너뛰고, 튜토리얼까지 끝난 경우 홈으로 바로 진입하도록 연결함. `initialRouteFor` 단위 테스트를 추가했고 `:app:testDebugUnitTest`, `:app:assembleDebug`, Galaxy SM-G991N(Android 15) `:app:installDebug`를 통과함. 잠금 화면과 `run-as` 권한 제한 때문에 저장 상태별 실기기 화면 분기는 미검증임.
- 2026-08-22 — 점검 시작 전 체크리스트 화면의 `기본 체크리스트 훑어보기`와 `건너뛰기`도 튜토리얼과 동일한 하단 액션 영역으로 이동함. 본문만 스크롤되고 두 동작은 화면 하단에 고정되도록 구성함.
- 2026-08-22 — 튜토리얼의 `60초 영상 보기`와 `건너뛰기`를 공통 `AppPageScaffold`의 하단 액션 영역으로 옮겨 화면 하단에 고정함. `:app:assembleDebug`, Galaxy SM-G991N(Android 15) Debug APK 설치를 통과함. 현재 기기는 약관 동의 전 상태라 튜토리얼 화면의 실기기 위치 확인은 미검증임.
- 2026-08-21 — 실기기 Debug API 기본 주소를 `127.0.0.1:8080`으로 고정하고 Debug 빌드 전에 연결된 기기의 `8080`·`9000` 포트를 자동 `adb reverse`하는 Gradle 작업을 추가함. Debug BuildConfig 회귀 테스트로 주소 재유입을 막고, 기기가 없는 빌드에서는 자동 작업만 건너뜀.
- 2026-08-21 — 매물 목록 카드를 오른쪽에서 왼쪽으로 밀면 카드 너비의 1/4만 이동해 삭제 버튼이 보이도록 구현하고 기존 매물 삭제 API에 연결함. 반대로 밀거나 삭제를 취소하면 원위치로 돌아가며, 삭제 성공 시 목록에서 제거하고 실패 시 기존 목록과 서버 오류를 유지함. 단위 테스트, Debug 빌드, Galaxy SM-G991N(Android 15) 설치를 통과했고 실기기에서 1/4 열림·역방향 닫힘·확인창·취소 후 매물 2개 유지를 확인함. 실제 데이터 삭제는 수행하지 않음.
- 2026-08-20 — 현재 위치 조회가 정밀 권한에서 GPS를 우선하도록 수정하고, 대략적 권한 사용 시 확인 안내와 동·호수용 상세 주소 입력을 추가함. 주소 결합·위치 제공자 선택 단위 테스트와 `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과함. GPS 정확도는 실내·수신 상태에 따라 달라지며 동·호수는 사용자 입력으로 유지함.
- 2026-08-20 — `feature/map`에서 현재 위치 아이콘을 눌러 주소를 교체하는 흐름을 구현함. 주소 교체·실패 시 기존 입력 보존 단위 테스트, `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과하고 Galaxy SM-G991N(Android 15)에서 현재 위치 주소 입력을 확인함. GPS 좌표는 저장·로그·API 전송하지 않음.
- 2026-08-20 — 최신 디자인 Compose 화면과 내비게이션을 유지한 채 OpenAPI 네트워크, 매물·임장 ViewModel, 영상 선택·고정 3초 JPEG 추출·2MiB 변환·미디어 업로드 기능을 선별 통합함. 자동 실행 환경의 네트워크·Gradle 캐시 접근 제한으로 빌드·테스트·Lint가 미검증이므로 관련 항목은 완료 처리하지 않음.
- 2026-08-20 — `feature/map`에서 주소 입력창 본문은 카카오 주소 검색 화면, 현재 위치 아이콘은 Kakao 지도·고정 중앙 핀 화면으로 분리하고 상세 주소 입력칸을 바로 아래에 유지함. 새 GPS 조회가 6초 안에 끝나지 않으면 최근 위치로 지도를 여는 fallback을 추가함. `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과하고 Galaxy SM-G991N(Android 15)에서 주소 검색 이동, 지도 표시·드래그·역지오코딩·주소 확정 복귀와 상세 주소 분리를 확인함. GPS 좌표는 저장·로그·API 전송하지 않음.
- 2026-08-21 — Kakao 지도 SDK 2.15.1과 Compose `SurfaceView` 호환 레이아웃을 적용함. `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과하고 Galaxy SM-G991N(Android 15)에서 실제 지도 타일 표시, 중앙 핀 고정, 드래그 후 주소 갱신을 확인함.
- 2026-08-21 — 반복 진입 시 지도 타일이 간헐적으로 사라지는 문제를 막기 위해 지도 화면을 전용 `LocationPickerActivity`로 분리하고 SDK의 `resume`·`pause` 생명주기를 연결함. `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과하고 Galaxy SM-G991N(Android 15)에서 최초 진입, 재진입 3회, 백그라운드 복귀 후 실제 지도 타일 표시를 확인함.
- 2026-08-21 — 주소 검색 화면 제목을 "점검할 집의 주소를 입력하세요"로 변경함. 검색창의 고정 높이와 빈 안내 영역이 입력 글자를 자르던 문제를 제거하고 `:app:testDebugUnitTest`, `:app:assembleDebug`와 Galaxy SM-G991N(Android 15)에서 입력값 및 검색 결과 표시를 확인함.
