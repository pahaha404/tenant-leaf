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

## 진행 기록

- 2026-08-26 — 매물 지도 화면(`PropertyMapOverviewScreen`)의 상단 바 뒤로가기 아이콘을 제거하고, 매물 상세 화면과 100% 동일한 좌측 하단 56.dp 시그니처 그린 3D 서클 플로팅 버튼(`Alignment.BottomStart`)으로 통일 적용 완료.

- 2026-08-26 — 매물 지도 화면(`PropertyMapOverviewScreen`) 상단 바에서 '매물 N개' 수량 표시 배지(`StateBadge`)를 좌측 타이틀 옆에서 우측 `+` 매물 추가 버튼의 바로 좌측 영역(`[ 매물 N개 ]  +`)으로 이동 재배치 완료.

- 2026-08-26 — 기존 `DeepGreen` 컬러 토큰이 거의 검은색에 가까운 숯색(`0xFF1C1C1E`)으로 정의되어 있던 문제를 짙은 다크 그린(`0xFF1E3A2B`)으로 바로잡고, 좌측 하단 플로팅 뒤로가기 버튼 배경을 명확하고 또렷한 그린(`Green = 0xFF2F6848`) 색상으로 적용하여 더 이상 검은색으로 보이지 않도록 전면 수정 완료.

- 2026-08-26 — 점검 음성 기록 카드 하단의 '최근 점검에서~' 서브텍스트를 전면 삭제하고 타이틀만 군더더기 없이 단정하게 유지함. 좌측 하단 56.dp 플로팅 뒤로가기 버튼을 시그니처 다크 그린(`DeepGreen`) 배경과 선명한 흰색 화살표 아이콘으로 변경 완료.

- 2026-08-26 — 매물 상세 화면(`PropertyDetailScreen`)의 세로 스크롤 동작을 비활성화(`scrollable = false`)하여 고정 화면 레이아웃으로 설정 완료.

- 2026-08-26 — 요청 사양에 따라 탭 스와이프 기능 및 스크롤 고정 시도 이전의 안정된 상태로 완전 원복(탭 스와이프 전면 비활성화 & 매물 상세 세로 스크롤 정상 활성화) 완료.

- 2026-08-26 — 탭 간 좌우 스와이프 제스처를 수직 스크롤과 이벤트 충돌이 없는 `draggable(Orientation.Horizontal)` 기반 제스처 감지 로직으로 개편하여, 튕김 현상 없이 손끝 제스처만으로 부드럽고 쾌적하게 탭이 전환되도록 원복 개선 완료.

- 2026-08-26 — 탭 간 좌우 슬라이드(스와이프) 제스처(`swipeToChangeTab`)를 원래대로 다시 복구 연동하고, 매물 상세 화면(`PropertyDetailScreen`)에서 위아래 세로 스크롤이 되지 않도록 세로 스크롤 동작을 비활성화(`scrollable = false`) 처리 완료.

- 2026-08-26 — 탭 좌우 스와이프/슬라이드 제스처(`swipeToChangeTab`)를 완전히 비활성화하고 기존의 탭 클릭 내비게이션 방식으로 깔끔하게 원복 처리 완료.

- 2026-08-26 — 매물 상세 상단 타이틀 바에 '매물 상세  |  매물 이름' 형태(`Text("매물 상세")` + `1.2.dp` 구분선 바 + `Text(property.name)`)로 인라인 구분 레이아웃(`titleContent`)을 구성하고 본문 중복 매물명을 정리하여 한층 깔끔한 상단 헤더로 개편 완료.

- 2026-08-26 — '점검 결과 리포트' 오렌지 버튼의 상하 패딩(`18.dp` ➔ `11.dp`), 좌측 아이콘 박스(`44.dp` ➔ `34.dp`), 폰트 크기(`17.sp` ➔ `14.5.sp` Bold)를 줄여 과하지 않고 슬림하며 단정한 캡슐 3D 버튼 스타일로 개편 완료.

- 2026-08-26 — '점검 결과 리포트' 오렌지 버튼 내부의 하단 설명 텍스트("촬영한 현장 기록과 AI 분석 결과를 확인해요.")를 삭제하고 메인 타이틀만 한층 깔끔하고 명확하게 표시되도록 반영 완료.

- 2026-08-26 — '점검 결과 리포트' 카드의 색상을 첨부 이미지 사양과 100% 동일한 3D 오렌지 캡슐 카드(`Color(0xFFF28A3A)`, 반투명 흰색 아이콘 박스 + 흰색 텍스트/화살표, `ReportActionOrangeCard`)로 적용 완료. 상단 타이틀 '매물 상세'와 Hero 매물명 간의 간격을 밀접하게 밀착 조정함. 카카오 지도 카드의 높이를 `210.dp`로 확장하고 스크롤 하단 여백 `Spacer(84.dp)`를 부여하여 좌측 하단 플로팅 뒤로가기 버튼에 지도가 짤리는 현상을 완전히 방지 완료.

- 2026-08-26 — 보증금 패널 내부에 네모난 안쪽 그림자가 투영되던 잔상을 불투명 솔리드 `PaleGreen` 및 `CardDefaults.cardElevation` 적용으로 완벽히 제거 완료. 매물 상세 메인 매물명 폰트 크기를 `19.sp`에서 `23.sp`(`ExtraBold`), 주소문구를 `13.5.sp`, 금액 타일 텍스트를 `14.5.sp`로 시원하게 확대 적용함. 뒤로가기 버튼을 화면 좌측 하단(`Alignment.BottomStart`)에 떠있는 56.dp 입체 3D 서클 플로팅 버튼으로 새로 반영 완료.

- 2026-08-26 — 매물 상세 화면의 상단 여백을 매물 탭과 1:1 동일한 `top = 36.dp`로 확장하고, 우측 상단 편집/휴지통 버튼을 첨부 이미지(지도/휴지통 알약 버튼)와 100% 동일한 듀얼 캡슐 알약 버튼(`EditTrashDualPillButton`) 스타일로 적용 완료. 모든 패널/카드의 외곽선 테두리(Border/Outline)를 제거하고 은은하게 떠오르는 `1.5.dp` 3D 소프트 섀도우만 깔끔히 남겨 단정하게 변경 반영 완료.

- 2026-08-26 — 매물 상세 화면(`PropertyDetailScreen`)의 상단 수정·삭제 버튼(44.dp 3D 서클 버튼), 보증금·월세·관리비 3단 타일(`FinancialMetricTile`), 점검 음성 기록 카드(`PropertyVoiceRecordCard`), 카카오 지도 카드(`PropertyKakaoMapCard`)의 여백 및 카드 스타일을 공통 통일 디자인 사양(`18.dp` 라운딩 + `1.5.dp` 소프트 섀도우)으로 완벽 변경 반영 완료.

- 2026-08-26 — 홈 탭 우측 상단 알림 버튼(`NotificationsNone`)과 매물 탭 우측 상단 지도/휴지통 듀얼 알약 버튼(`MapTrashDualPillButton`)의 수직 위치를 메인 타이틀 텍스트("세입세잎", "매물")의 하단 라인과 1:1 선으로 똑같이 들어맞도록 하단 수직 정렬(`verticalAlignment = Alignment.Bottom`)로 변경 반영 완료.

- 2026-08-26 — 그림자가 100% 잘려 안 보이던 근본 원인(Root Cause) 발견 및 해결 완료. `SwipeToDeletePropertyCard` 최외곽 `Box`에 `clip(RoundedCornerShape(18.dp))`이 걸려 있어 하위 `PropertyCard`가 바깥으로 투사하는 외부 그림자를 강제로 모두 잘라내던 버그를 분석·원인 제거 완료. 최외곽 `clip`을 제거하고 배경색을 `PageBackground (0xFFF7F8FA)`로 맞추어 은은하고 풍성한 입체 3D 소프트 섀도우가 완벽히 표시되도록 정돈 완료.

- 2026-08-26 — 순백색(`Color.White`) 배경에서 그림자가 연해져 평면처럼 보이던 문제를 명시적 `ambientColor`(`0.12f`), `spotColor`(`0.16f`), `4.dp` 입체 엘레베이션 및 `1.dp` 미세 경계 외곽선(`Color(0xFFEFEFEF)`) 조합을 통해 눈에 띄게 은은하고 선명하게 떠오르는 3D 소프트 섀도우 입체감으로 정돈 완료.

- 2026-08-26 — 두 번째 참조 이미지(홈 탭 레퍼런스 스타일)와 100% 동일하게 매물 탭 스타일 리뉴얼 완료. 배경색을 퓨어 화이트(`Color.White`)로 적용하고, 카드 모서리를 `18.dp`, 소프트 입체 3D 그림자를 `1.5.dp`, 우측 포레스트 그린 이동 화살표(`ArrowForward`) 아이콘을 완벽히 복구 반영 완료.

- 2026-08-26 — Meta View 참조 이미지 UI 스타일과 100% 동일하게 매물 탭 카드 디자인 리뉴얼 완료. (모서리 라운딩 `24.dp` 캡슐 스쿼클 카드, 짙은 그림자 대신 은은한 `1.dp` 섀도우 엘레베이션, 좌측 `44.dp` `Color(0xFFF2F4F7)` 미니멀 아이콘 박스, `horizontal = 20.dp, vertical = 16.dp` 여백 패딩 적용)

- 2026-08-26 — 입체감 있는 3D 버튼 & 카드(Tactile 3D Elevation) 전면 적용 완료. 매물 카드·주요 버튼(`PrimaryButton`/`SecondaryButton`)·안내 카드(`InfoCard`)·알약 버튼(`MapTrashDualPillButton`)에 `3.dp` 섀도우 엘레베이션을 적용하고, 상단 뒤로가기 버튼을 입체형 44.dp 서클 버튼 스타일로 변경 완료. 전체 페이지 배경을 3D 카드가 돋보이는 웜 톤 소프트 오프화이트(`Color(0xFFF7F8FA)`)로 정돈 완료.

- 2026-08-26 — 홈 탭의 `HomeBackground` (`Color.White`)와 100% 동일하게 공통 디자인 토큰 `PageBackground`를 `Color.White`로 수정하여, 매물 탭 배경이 옅은 회색(`0xFFF7F8FA`)으로 이질감이 생기던 현상을 원인 분석 후 완벽히 일치 수정 완료.

- 2026-08-26 — 매물 탭 배경색(`PageBackground 0xFFF7F8FA`)을 `Scaffold` 레벨까지 확실히 적용하고, 스와이프 모션에서만 삭제 배경이 노출되도록 개선하여 카드의 빨간색 테두리 번짐 현상을 근본적으로 해결 완료. 또한 홈 탭과 100% 동일한 소프트 섀도우(`shadow(1.5.dp, 18.dp)` + `background(Color.White, 18.dp)`)를 적용하고 우측 화살표 아이콘을 제거 완료.

- 2026-08-26 — 첨부 이미지(홈 탭 리포트 카드 디자인)와 100% 일치하도록 매물 탭 카드 디자인 업데이트 완료. (`20.dp` 라운딩, `1.5.dp` 입체 3D 소프트 섀도우, 좌측 38.dp 그린 주택 아이콘 박스, 굵은 매물명 및 주소 레이아웃, 우측 초록색 이동 화살표 `ArrowForward` 아이콘 반영)

- 2026-08-26 — 매물 지도 화면(`PropertyMapOverviewScreen`) 헤더의 "매물 지도" 타이틀 폰트 크기를 `16.5.sp`에서 `21.sp` (`FontWeight.ExtraBold`)로 확대하여 우측 "매물 N개" 뱃지 캡슐 높이와 시각적으로 1:1 세로 높이 균형을 맞춤 반영 완료.

- 2026-08-26 — 매물 지도 화면(`PropertyMapOverviewScreen`) 상단 헤더 바의 상단 여백을 다른 주요 화면들과 동일한 `top = 36.dp`로 상단 여백을 일치 반영 완료.

- 2026-08-26 — 사용자 요청에 따라 매물 카드의 기존 아이템 스타일(14.dp 곡률, 9.dp vertical padding, 14.5.sp 폰트 사양)을 원복 완료. (배경색, 16.dp 여백 간격, 지도+휴지통 듀얼 알약 버튼, 56.dp 원형 그린 FAB 스타일은 그대로 유지)

- 2026-08-26 — 매물 탭 디자인 시스템을 홈 탭과 100% 동일한 스타일(배경색: `PageBackground (0xFFF7F8FA)`, 섹션 간격: `Arrangement.spacedBy(16.dp)`, 카드 곡률/입체감: `18.dp` 라운딩 + `1.5.dp` soft shadow, 상단 액션 버튼 스타일 통일)로 업데이트 반영 완료.

- 2026-08-26 — 매물 탭 우측 상단 메인 액션을 첨부 이미지와 100% 동일한 듀얼 캡슐 알약 버튼(`MapTrashDualPillButton`)으로 변경. 기존 지도 아이콘(`Icons.Outlined.Map`) 및 휴지통 버튼을 통합하고, 터치/눌림 시 선택 영역만 은은한 배경 피드백(`Color(0xFFF2F4F7)`)이 채워지도록 구현 완료.

- 2026-08-26 — 홈 탭을 제외한 주요 탭(매물, 리포트 선택, 내 정보) 서브타이틀 텍스트 제거 및 18.dp 스페이서 배치로 메인 타이틀 Y-좌표 위치 고정. 매물 탭 지도 버튼을 우측 하단 매물 추가 FAB와 동일한 56.dp 원형 그린 FAB 스타일로 좌측 하단에 배치. 매물 다중 삭제 휴지통 버튼을 홈 탭 알림 버튼과 동일한 44.dp 입체 원형 스타일로 우측 상단 헤더 위치에 반영 완료.

- 2026-08-26 — 하단바 주요 탭(매물, 리포트 선택, 내 정보) 메인 헤더의 상단 여백(top = 36.dp), 폰트 스타일(23.sp ExtraBold DeepGreen), 및 서브타이틀 위치를 홈 탭("세입세잎")과 100% 동일한 좌표 및 타이포그래피로 통일 반영 완료.

- 2026-08-26 — 기존의 프리미엄 다크 그린 사이버 글래스모피즘 카드 스타일(회전 연속 그라데이션 림, 3D AR 안경 HUD 뷰포트, 데이터 스트림 이퀄라이저)을 원복하고, "Ray-Ban Meta AI" 타이틀 바로 아래에 네온 그린 dot + "연결됨" 연결 상태 표시 라인을 추가 반영 완료.

- 2026-08-26 — Ray-Ban Meta AI 안경 기기 상태 카드(`GlassStatusCard`) 디자인을 첨부 이미지와 100% 동일하게 리뉴얼(연한 민트 배경 박스 내 안경 아이콘, "Ray-Ban Meta AI" 헤더, 초록색 상태 dot + "연결됨" 텍스트, 연한 민트 라임 "관리" 버튼) 반영 완료.

- 2026-08-26 — 자취 매거진 섹션 우측 "전체보기 ›" 텍스트 및 각 카드 우측 화살표(`ArrowForward`) 아이콘 색상을 다크 그린(`Green = Color(0xFF2F6848)`)으로 변경 반영 완료.

- 2026-08-26 — 하단바 탭 선택(포커싱) 시 아이콘 및 라벨 텍스트 색상을 시그니처 오렌지 컬러(`Orange = Color(0xFFF68B38)`)로 완벽 복구 완료.

- 2026-08-26 — `HorizontalPager` 드래그 터치 이벤트를 가로막던 하위 뷰의 터치 인터셉터(`swipeToChangeTab`)를 전면 제거하여, 화면을 쓸어넘기는 동안 옆 탭 화면이 손끝을 따라 실시간으로 자연스럽게 미끄러져 들어오는 실크 스와이프 제스처 버그 수정 완료.

- 2026-08-26 — 메인 탭 화면들(홈/매물/리포트/내 정보)을 `HorizontalPager` 기반 페이징 호스트(`MainTabsPagerScreen`)로 전환. 손가락으로 화면을 쓸어넘기는 동안 옆 탭 화면이 실시간으로 옆에서 미끄러져 보이는 실크처럼 부드러운 스와이프 인터랙션 구현 완료.

- 2026-08-26 — "최근 점검 리포트" 카드(`RecentReportCard`)의 아이콘(`Article`) 색상을 다크 그린(`Green = Color(0xFF2F6848)`)으로 설정 완료.

- 2026-08-26 — 실제 하단바 아이콘 색상인 다크 차콜 폰트/아이콘 대표 색상(`DeepGreen = Color(0xFF1C1C1E)`)을 하단바 선택 아이콘, 자취 매거진 오른쪽 화살표(`ArrowForward`), "전체보기" 링크에 동일하게 완벽 반영함.

- 2026-08-26 — 자취 매거진 아티클 카드(`MagazineArticle`)의 오른쪽 화살표(`ArrowForward`) 및 "전체보기" 링크 색상을 하단바 아이콘 색상인 브랜드 다크 그린(`Green = Color(0xFF2F6848)`) 색상과 동일하게 설정함.

- 2026-08-26 — 자취 매거진 아티클 카드(`MagazineArticle`)의 폰트 스타일을 "오늘의 점검 팁"과 동일하게 맞춤(태그: `13.sp` ExtraBold Orange, 본문 제목: `13.sp` Normal DeepGreen). 아티클 카드 스타일은 소프트 섀도우 적용 이전의 플랫 카드 스타일로 롤백함.

- 2026-08-26 — "오늘의 점검 팁"과 "자취 매거진" 섹션 사이에 얇은 구분선(`HorizontalDivider`, `Border` 색상) 추가 완료. 자취 매거진 섹션 제목(`19.2.sp`), 전체보기(`13.2.sp`), 아티클 태그(`11.sp`) 및 아티클 제목(`13.5.sp`) 폰트 크기를 1.2배 확대하고 `1.5.dp` 3D 카드 입체감 적용.

- 2026-08-26 — "오늘의 점검 팁" 카드(`InspectionTipCard`)의 배경색을 기존 연회색(`Color(0xFFF2F3F5)`)과 흰색(`Color(0xFFFFFFFF)`)의 중간 톤인 매우 은은하고 밝은 오프화이트 톤(`Color(0xFFF8F9FA)`)으로 변경함.

- 2026-08-26 — "오늘의 점검 팁" 카드 하단 페이저 인디케이터 도트(원)들의 수직 정렬을 수평 중심축 수직 중앙 정렬(`verticalAlignment = Alignment.CenterVertically`)로 수정하여 정중앙 가운데 선 정렬 완료.

- 2026-08-26 — 좌우 스와이프 드래그 제스처 기반 탭 전환 기능(`swipeToChangeTab`) 추가. 홈 ⇄ 매물 ⇄ 리포트 ⇄ 내 정보 순서로 손가락으로 화면을 좌우로 쓸어넘기면 탭이 즉시 전환되도록 구현함.

- 2026-08-26 — "최근 점검 리포트" 카드(`RecentReportCard`)의 좌측 아티클 아이콘 박스 외곽선 테두리(`border`)를 제거하여 보다 깔끔한 미니멀 디자인으로 정돈함.

- 2026-08-26 — 우측 상단 3D 알림 버튼(`NotificationsNone`)에 터치 이벤트(`clickable`)와 콜백(`onNotificationClick`), 그리고 클릭 시 안내 토스트 메시지("새로운 알림이 없습니다.")를 연동함.

- 2026-08-26 — 우측 상단 알림 버튼을 Meta 앱 참조 이미지의 좌상단 동그란 입체 버튼 스타일(`CircleShape`, 3D 소프트 섀도우 `1.5.dp`, `Color.White`, 크기 `44.dp`)로 변경하고, 전체 섹션 수직 간격(`16.dp`) 및 퀵 액션 간격(`12.dp`)을 레이아웃에 맞게 정돈함.

- 2026-08-26 — 상단 헤더 서브문구("오늘도 안심되는 자취"), 최근 점검 리포트 부제목, 오늘의 점검 팁 본문/라벨 등 소형 폰트 크기(`10.sp` / `11.sp`)를 기존 1.5배에서 1.3배 확대(`13.sp` / `14.3.sp`)로 적정 조정함.

- 2026-08-26 — 상단 헤더 서브문구("오늘도 안심되는 자취"), 최근 점검 리포트 부제목, 오늘의 점검 팁 본문/라벨 등 소형 폰트 크기(`10.sp` / `11.sp`)를 1.5배 확대하여 `15.sp` / `16.5.sp`로 적용함.

- 2026-08-26 — "오늘의 점검 팁" 카드 배경을 밝은 회색(`Color(0xFFF2F3F5)`)으로 변경하고, "최근 점검 리포트"의 아이콘 박스 배경을 흰색(`Color.White`) 및 은은한 테두리로 변경, 아이콘 색상을 하단바 리포트 아이콘과 동일한 포레스트 다크그린(`Green`, `Color(0xFF2F6848)`)으로 일치시킴.

- 2026-08-26 — 홈 화면 퀵 액션 버튼("매물 등록하기", "체크리스트 확인")의 아이콘 색상을 포인트 포레스트 다크그린(`Green`, `Color(0xFF2F6848)`)으로 변경함.

- 2026-08-26 — 홈 화면 메인 주황색 "점검 시작하기" 버튼(`StartInspectionCard`)의 세로 패딩을 기존 9.dp에서 24.dp로 2배 확대하고, 아이콘 크기(`24.dp`) 및 폰트 크기(`17.sp`)를 시인성 있게 맞추어 높이를 2배 확장함.

- 2026-08-26 — 3D 카드 및 버튼의 그림자 농도/깊이(Elevation)를 기존 3.dp/8.dp에서 1.5.dp/3.dp 수준으로 은은하고 옅게 축소하여 보다 자연스럽고 부드러운 입체감을 적용함.

- 2026-08-26 — 홈 화면 헤더("세입세잎") 아래 하단 여백(`8.dp`)을 추가하고, 우측 상단 알림 종 아이콘의 연초록 배경을 제거하여 투명 처리 및 아이콘 색상을 딥 다크차콜(`Color(0xFF1C1C1E)`)로 변경함.

- 2026-08-26 — 홈 화면 상단 헤더("오늘도 안심되는 자취") 상단 여백을 기존 18.dp에서 36.dp로 2배 확장 적용함.

- 2026-08-26 — 홈 화면 퀵 액션 버튼("매물 등록하기", "체크리스트 확인") 배경을 3D 입체감 있는 흰색 카드(`Color.White`)로 변경하고 폰트 색상을 딥 다크차콜(`Color(0xFF1C1C1E)`)로 적용함. "오늘의 점검 팁" 카드는 요청대로 입체 그림자를 제거하여 평면 롤백함.

- 2026-08-26 — Meta UI 스타일 기반 입체감 있는 3D 버튼/카드 디자인 적용. 흰색/투명 버튼 및 카드에 3D 소프트 섀도우 엘레베이션(3.dp) 및 둥근 모서리를 적용하고, 폰트 색상을 메타 전용 딥 다크차콜(`Color(0xFF1C1C1E)`) 및 뉴트럴 다크그레이(`Color(0xFF767676)`)로 일괄 변경함 (단, 버튼 내 흰색 텍스트 및 오렌지 포커싱 폰트/아이콘 제외).

- 2026-08-26 — 하단 내비게이션 바(`AppBottomNavigation`, `NavigationBarItem`) 포커싱 스타일 수정. 선택 시 표시되던 보라색 배경 인디케이터를 제거(`indicatorColor = Color.Transparent`)하고, 포커싱된 탭의 아이콘과 텍스트 라벨 색상을 주황색(`Orange`)으로 변경함.

- 2026-08-25 — 모바일 데이터 임시 시연용 Cloudflare Quick Tunnel 시작·종료 및 Android APK 생성 스크립트를 추가했다. 외부 HTTPS API·MinIO health 응답과 `judge-a` APK의 외부 API 주소 포함 빌드는 확인했으나, 실제 휴대전화에서 Wi-Fi를 끄고 모바일 데이터만 사용하는 종단 간 임장·JPEG 업로드·리포트는 아직 미검증이다.

- 2026-08-25 — 발표용 다중 휴대전화 LAN 시연을 위해 Debug APK가 `X-Demo-User` 헤더(`judge-a`~`judge-d`)를 전송하도록 추가하고, 노트북 LAN API 주소를 넣은 APK 4종을 생성했다. Core 컴파일·APK 패키징과 API의 A/B 매물 분리 호출을 확인했다. 실제 휴대전화 4대 동시 연결과 방화벽 허용은 발표 네트워크에서 확인이 남아 있다.
- 2026-08-25 — 리포트의 촬영 공간 다시 보기를 Gemini 공간 분류 기반 갤러리로 보완했다. 대표 사진을 `주방`, `거실·방`, `화장실`, `공간 확인 필요`로 묶고 같은 공간의 여러 사진 및 전체화면 좌우 탐색을 지원한다. OpenAPI 응답의 공간·불확실 여부를 UI 모델로 연결하고 정적 검사를 통과했으나 Android Gradle Plugin `9.3.2`를 오프라인 환경에서 해석하지 못해 빌드·단위 테스트·실기기 확인은 미완료 상태다.

- 2026-08-25 — 점검 리포트에 촬영 순서 기반 대표 사진 가로 목록과 전체 화면 좌우 넘김 뷰어를 추가했다. 확인 필요 관찰은 불확실한 AI 구역명으로 묶지 않고 근거 사진의 영상 시점 순으로 표시하며, 서버 진행 수치가 한 번에 증가해도 화면에서는 사진 단위로 순차 반영하도록 보완했다. 구현과 정적 검사는 완료했으나 Kotlin `2.3.21`·Android Gradle Plugin `9.3.2` 의존성을 오프라인 환경에서 해석하지 못해 Android 빌드·단위 테스트·실기기 검증은 미완료 상태다.

- 2026-08-25 — `InspectionCountdownScreen`에서 기존 `3초 뒤 촬영이 시작됩니다` 안내 후 신발장 곰팡이·습기·천장/벽지 하자·주방 누수/배수·화장실 누수/곰팡이·창틀 결로/방충망 체크리스트를 이어서 TTS 발화하도록 구현함. 화면의 `3→2→1` 카운트다운은 유지하되 숫자는 발화하지 않고, Android TTS 무음 큐를 사용해 각 안내 문장 사이에 2초 간격을 적용함. `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과했으며 실기기 TTS 발화 순서·간격·음량은 미검증임.

- 2026-08-25 — `MainActivity`의 앱 시작 시 주변 기기 권한 자동 요청을 제거하고 `Permissions` 화면 진입 시 `BLUETOOTH_SCAN`·`BLUETOOTH_CONNECT`를 포함한 미허용 권한만 요청하도록 수정함. `MediaUploadApiRoute`도 분석 화면 진입 시 Android 13 이상의 사진·동영상 권한과 Android 14의 `READ_MEDIA_VISUAL_USER_SELECTED`를 처리하며, 콜백 맵 대신 실제 승인 상태를 재확인하도록 보완함. SDK별 회귀 테스트를 추가했고 `:app:testDebugUnitTest`, `:app:assembleDebug`를 통과함. 실기기 시스템 권한 팝업 위치는 미검증임.

- 2026-08-25 — 신규 사용자의 3단 온보딩을 로그인 화면보다 먼저 표시하도록 시작 라우팅을 `Welcome → Login → Consent` 순서로 변경함. 온보딩 완료 상태는 저장해 재실행 시 건너뛰고, 로그인 성공 후에는 동의 화면으로 이동하도록 `initialRouteFor` 회귀 테스트를 보강함. 실기기 화면 흐름은 미검증임.

- 2026-08-25 — 실시간 점검의 `점검 나가기 (취소)` 간헐적 무반응을 수정함. 초기 임장 조회 완료 전에도 취소 요청을 허용하고, 취소 성공 후에만 음성 녹음을 폐기하며, 처리 중 중복 입력 잠금·취소 오류 표시를 추가함. `:feature:inspection:testDebugUnitTest`, `:app:compileDebugKotlin` 통과.
- 2026-08-25 — 홈 화면(`HomeScreen`) 레이아웃을 `Scaffold` 구조로 리팩터링하여 세로 스크롤 먹통 현상 해결. 기존 `Box` + `align(BottomCenter)` 하단바 오버레이 구조에서 발생하던 스크롤 제스처 충돌 및 뷰포트 측정 오류를 `Scaffold(bottomBar = { AppBottomNavigation(...) })`와 `padding(innerPadding)` 기반의 독립 스크롤 컨테이너로 정돈하여 상하단 부드러운 스크롤을 복구함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-25 — 점검할 매물 선택 화면(`PropertySelectScreen`) 상단 여백 및 제목·소제목 레이아웃 정돈. 상단바와의 시각적 간격을 위해 상단 패딩(`top = 12.dp`)을 추가하고, "어느 매물을 점검할까요?" 제목과 "점검 기록은 선택한 매물에 저장돼요." 소제목을 하나의 그룹(`Column`, `spacedBy(4.dp)`)으로 묶어 전체적인 위치를 하단으로 자연스럽게 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-25 — 앱 전체 실행 주기(`MainActivity`) 동안 안드로이드 시스템 바(상단 상태바 및 하단 네비게이션바/제스처바 전체 `Type.systemBars()`) 전역 숨김(Full Immersive Mode) 적용. `onCreate`, `onResume`, `onWindowFocusChanged`에 `WindowInsetsControllerCompat`를 적용하여 앱 실행 중에는 항상 상단 상태창과 하단 네비게이션 토글바가 노출되지 않도록 처리함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-25 — 홈 화면(`HomeScreen`, `AppNavGraph`) "매물 등록하기" 퀵 액션 버튼 내비게이션 경로 직결. 기존 매물 리스트(`Route.PropertyList`)로 이동하던 경로를 매물 등록 화면(`Route.PropertyForm`)으로 직접 이동하도록 수정하여 불필요한 뎁스를 단축함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-25 — 리포트 UI에서 참고 점수·/100·감점 설명과 관련 UI 모델 필드를 제거했다. JPEG 전송 완료 후에는 로딩 스피너를 멈추고 완료 안내와 리포트 진행 버튼을 표시하도록 보완했다. Android Gradle Plugin `9.3.2`를 오프라인에서 해석하지 못해 빌드·실기기 검증은 미완료다.
- 2026-08-25 — 구역 명칭 간소화(현관·공용 ➔ 현관). `UiCatalog`, 실시간 점검(`LiveInspectionScreen`), 리포트(`ReportApiRoute`) 등 UI 전반에서 '현관·공용'으로 표기되던 명칭을 '현관'으로 간결하게 변경 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).

- 2026-08-25 — 실시간 점검 화면(`LiveInspectionScreen`) UI/UX Best Practices 고도화. 1) 카메라 프리뷰 시인성 극대화(높이 230dp 확대, REC 펄스 애니메이션 뱃지, 실시간 경과 시간 및 소스 뱃지 오버레이), 2) 가로 스크롤 구역(Zone) 퀵 스위처 칩 바를 탑재하여 이동 동선에 맞춰 원하는 구역 가이드로 즉각 전환 지원, 3) 체크리스트를 번호 인덱스 기반 촬영 리마인더 카드로 재정돈, 4) 뒤로가기 터치 및 시스템 백 제스처 시 이탈 방지 안전 확인 다이얼로그(`showExitDialog`, `BackHandler`) 연동, 5) 하단 엄지 조작 영역의 일시정지/재개 및 점검 종료 액션바 인터랙션을 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-25 — 실시간 점검 화면(`LiveInspectionScreen`) 확인 안내 항목 스타일 통일. 현관·공용 및 각 구역 확인 안내 체크리스트에서 첫 번째 항목에 적용되어 있던 연초록(`PaleGreen`) 배경 및 채워진 원(`●`) 강조 스타일을 제거하고, 모든 항목이 기본 배경(`Color(0xFFF8F8F6)`) 및 빈 원(`○`)으로 동일하게 표시되도록 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-25 — 온보딩 시작 화면(`Welcome`) 3단 슬라이드에 세입세잎 전용 온보딩 일러스트(`onboarding_1`, `onboarding_2`, `onboarding_3`) 적용. `HorizontalPager` 전체화면 렌더링 및 하단 인디케이터·'서비스 시작하기' 액션 버튼 연동 완료. `:app:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 등록 화면에서 키보드 `다음`을 누르면 매물 이름→상세 주소→보증금→월세 순서로 포커스가 이동하도록 하고, 포커스 입력칸을 자동 스크롤해 키보드에 가리지 않도록 처리함. 앱 전체 페이지에도 IME 여백을 적용함. `:app:compileDebugKotlin` 및 Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함.
- 2026-08-24 — 로컬 서버는 실행 중이었지만 휴대전화의 `adb reverse` 포트 전달이 사라져 앱이 `127.0.0.1:8080`에 연결하지 못한 것을 확인함. 실제 기기 `8080`·`9000` 전달을 복구하고, Debug 빌드가 오프라인 에뮬레이터 대신 연결된 실제 기기 serial을 골라 포트 전달하도록 수정함. 기기 포트 연결 검사 성공.

- 2026-08-24 — 점검 MP4의 AAC 오디오와 STT용 WAV가 마이크를 따로 점유하던 구조를 하나의 `InspectionVideoRecorder` PCM 흐름으로 통합함. 영상 AAC와 STT용 WAV에 같은 PCM을 동시에 저장하도록 바꿔, 동영상 소리 누락과 마이크 경쟁을 제거함. Android OpenAPI 재생성·`:app:compileDebugKotlin`·Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함. 실제 촬영 후 갤러리 음성 재생·STT 결과는 사용자 현장 확인이 남아 있음.
- 2026-08-24 — JPEG 업로드 계약에 필수 `zone`을 복구하고 Android는 AI/사용자 확정 전 `UNKNOWN`으로 전송하도록 수정함. OpenAPI 원본·생성 Android 모델·서버 저장·중복 확인을 함께 맞췄으며, 로컬 API를 새 코드로 재시작함. Android OpenAPI 검증·생성 및 `:app:compileDebugKotlin`, 서버 OpenAPI 검증·Kotlin 컴파일을 통과함. Gradle 테스트 실행은 기존 테스트 런처의 `ClassNotFoundException`으로 별도 실패했음.

- 2026-08-24 — 임장 라이브 진입과 함께 `VoiceRecorder`가 16kHz PCM 음성을 앱 내부 `files/voice-records/<임장 ID>/`에 자동 저장하고, 점검 종료 후 WAV 재생 파일을 만든 뒤 Android 시스템 STT로 텍스트화를 시작하도록 변경함. 종료 확인 화면에는 재생, STT 원문, 두 문장 요약 카드를 추가함. 서버 업로드·AI 제공자 호출은 추가하지 않았고, 실제 녹음·STT·재생 및 안경 촬영과의 동시 사용은 아직 실기기 검증 전이므로 미완료로 유지함.
- 2026-08-25 — 리포트 목록·분석/생성 중·완료·빈 결과·부분 완료·오류 화면을 기존 앱 디자인으로 복원하고, 관찰 항목에서 여는 최신 `근거 사진 분석` 전체 화면과 원본 비율 `xyxy` BBOX·이전/다음·검토 완료 기능만 유지했다. API 연동 모델과 콜백은 보존했으며, AI 공간 분류 제외 대상인 기존 `ENTRANCE_COMMON`·`WINDOW_VENTILATION` 응답은 별도 구역으로 노출하지 않고 `구역 확인 필요`로 정규화했다. 로컬 Gradle 배포본으로 컴파일을 시도했으나 Android Gradle Plugin 9.3.1을 외부 저장소에서 내려받을 수 없는 실행 환경이라 빌드·실기기 검증은 미완료다.
- 2026-08-25 — Figma 리포트 시안을 기준으로 목록·분석/생성 중·완료·빈 결과·부분 완료·오류·근거 사진 뷰어 7개 Compose 화면을 재구성했다. 기존 리포트 API 폴링과 내비게이션을 유지하고 실제 매물 주소·날짜·참고 점수·관찰 검토 상태를 연결했으며, 원본 비율 기반 `xyxy` BBOX 표시와 세잎클로버 브랜드 아이콘을 적용했다. 로컬 Gradle 9.5 배포본은 확인했으나 Android Gradle Plugin 9.3.1 플러그인 메타데이터를 오프라인에서 해석하지 못하고 실행 환경의 외부 다운로드가 차단되어 빌드·단위 테스트는 미검증 상태다.
- 2026-08-24 — 리포트 선택 화면의 고정 샘플 3개를 제거하고 실제 매물 목록과 매물별 최신 리포트 API를 조회하도록 연결함. 완료·부분 완료·생성 중·실패·없음 상태와 실제 완료 리포트 수를 표시하고, 선택 시 해당 `inspectionId`의 실제 상세 리포트 화면으로 이동함. 로컬 API에서 완료 리포트와 `inspectionId` 반환을 확인했으나 Gradle 9.5 배포본 다운로드가 실행 환경 네트워크 정책으로 차단되어 단위 테스트·앱 빌드는 미검증 상태임.
- 2026-08-24 — Android 팀 공용 디버그 서명 키(`debug.keystore`) 설정 적용(`signingConfigs.debug`). 개발자별 PC의 `debug.keystore` 불일치로 인한 카카오 맵 SDK 인증 실패(타일 미렌더링)를 방지하기 위해 프로젝트 공용 `debug.keystore`를 추가하고 `build.gradle.kts`에 `signingConfigs.debug`를 지정함. `:app:assembleDebug`, `:app:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 리포트 진행률 분모를 서버 `totalMediaCount`에 연결하고 처리 수를 성공+최종 실패 사진 수로 계산하도록 수정함. `WAITING_FOR_ANALYSIS`는 `사진을 분석하고 있어요`, 실제 `GENERATING`은 `리포트를 만들고 있어요`로 분리했으며, 원격 Gradle 플러그인 접근 제한으로 앱 빌드·실기기 확인은 미검증 상태임.
- 2026-08-24 — JPEG 업로드 완료 뒤 실제 임장 리포트 API를 2초 간격으로 조회하고 서버 상태를 `Generating`, `Completed`, `Empty`, `Partial`, `Error` UI에 연결함. 근거 사진은 원본 픽셀 `xyxy`를 `ContentScale.Fit` 표시 영역에 맞춰 변환하며 같은 사진의 여러 bbox를 함께 그리고 선택 관찰을 마지막에 굵게 표시하도록 변경함.
- 2026-08-24 — `design/UI/UI.pen`의 리포트 상세 상태를 Compose로 옮겨 `Completed`, `Evidence Viewer`, `Empty`, `Partial`, `Error`, `Generating` 화면을 상태 모델로 구성함. 기존 점검 종료 후 `reportProcessing` 상태에는 생성 중 화면을 연결하고, 관찰 카드에서 서명 URL 근거 사진과 bbox를 전체화면으로 확인할 수 있게 구현함. 참고 점수는 관찰 1건당 5점 차감·최저 0점으로 계산하며 단위 테스트를 추가함.
- 2026-08-24 — 내 정보 화면(`ProfileScreen`, `AppNavGraph`) 로그아웃 기능 및 확인 다이얼로그 추가. 프로필 탭 내 계정 관리 영역에 로그아웃 버튼을 배치하고 확인 다이얼로그 팝업 후 승인 시 `TokenStorage.clearToken()` 및 `LoginPreferences` 초기화, 백스택 클리어 후 로그인 화면(`Route.Login`)으로 안전하게 이동하도록 구현 완료. `:app:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 보증금 및 월세 금액 한국 단위(억/만) 표기 적용(`formatKoreanAmount`, `PropertyScreens`, `PropertyMapOverviewScreen`). 10,000만원 이상일 경우 '1억원', '1억 5,000만원' 등 한국어 부동산 금액 체계에 맞게 억·만 단위를 자동 계산·포맷팅하여 매물 목록 카드, 매물 상세 3단 메트릭 타일, 지도 오버뷰 하단 프리뷰 카드에 가독성을 개선함. `:app:testDebugUnitTest` 및 `:feature:property:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).

- 2026-08-24 — 분석 진행 화면(`AnalysisProgressScreen`, `MediaApiRoute`) 내 "영상 직접 선택" 버튼 제거 및 갤러리 최신 영상 자동 분석·업로드 단일화. 촬영 종료 확인 후 분석 진행 화면 진입 시 사용자의 추가 수동 선택 없이 휴대전화 갤러리의 최근 촬영 비디오를 자동으로 감지하여 3초 구간별 JPEG 추출 및 서버 업로드를 즉각 수행하도록 워크플로우를 단순화함. `:app:testDebugUnitTest`, `:feature:media:testDebugUnitTest`, `:feature:inspection:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 및 매물 상세 탭 당겨서 새로고침(`Pull-to-Refresh`) 기능 구현(`AppPageScaffold`, `PropertyListScreen`, `PropertyDetailScreen`). Material 3 `PullToRefreshBox` 및 `PullToRefreshDefaults.Indicator`를 공통 스캐폴드에 연동하여 화면을 아래로 당겼을 때 상단 인디케이터와 함께 `PropertyListViewModel.refresh()` 및 `PropertyDetailViewModel.load()`가 호출되어 최신 매물 목록과 상세 정보로 즉시 갱신되도록 완성함. `:app:testDebugUnitTest` 및 `:feature:property:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 상세 화면 휴지통 좌측 연필(`Edit`) 아이콘 추가 및 매물 수정 기능 연동(`PropertyDetailScreen`, `PropertyFormScreen`, `Route.PropertyEdit`). 매물 상세 탭 상단 액션바에 연필 아이콘을 배치하여 탭 시 기존 매물 정보를 불러와 즉시 수정(`PropertyPatch` 기반 업데이트)할 수 있도록 화면 및 내비게이션 경로를 완성함. `:app:testDebugUnitTest` 및 `:feature:property:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 우측 상단 휴지통 아이콘 추가 및 다중 선택 일괄 삭제 기능 구현(`PropertyListScreen`, `PropertyListViewModel.deleteMultiple`). 지도 아이콘 왼쪽에 휴지통(`DeleteOutline`) 아이콘을 배치하여 탭 시 다중 선택 모드로 전환되며, 개별 체크/전체 선택/전체 해제 및 하단 "선택한 N개 매물 삭제하기" 액션 버튼을 통해 일괄 삭제 다이얼로그 확인 후 한 번에 안전하게 삭제할 수 있도록 완성함. `:app:testDebugUnitTest` 및 `:feature:property:testDebugUnitTest` 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — TTS 음성 엔진 `TenantLeafApplication` 레벨 초조기 사전 예열 및 빨간 버튼 탭 0ms 즉시 발화 적용. 안드로이드 프로세스 기동(`Application.onCreate`) 즉시 TextToSpeech 엔진 백그라운드 바인딩을 수행하고, 빨간 확인 버튼 탭 시 미디어 스트림(`STREAM_MUSIC`)을 통해 터치와 동시에 "3초 뒤 촬영이 시작됩니다" 음성이 딜레이 없이 즉각 출력되도록 개선 완료. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 3초 카운트다운 화면(`InspectionCountdownScreen`) 유지 및 "3초 뒤 촬영이 시작됩니다" 단독 음성 안내(TTS) 정합. 촬영 전 경고 동의 후 3초 카운트다운 화면(`3 -> 2 -> 1`)이 정상적으로 노출되며 TTS 음성("3초 뒤 촬영이 시작됩니다")이 출력되고, 이후 실시간 점검 화면에서는 추가 음성 발화 없이 깔끔하게 카메라 촬영 모드로 돌입하도록 가이드 사운드 흐름을 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 실시간 점검 안경 미연결 시 스마트폰 카메라 자동 전용 촬영 전환(`CameraSource.PHONE`). Meta 안경이 블루투스로 연결되어 있지 않은 경우, 안경 전환 토글을 숨기고 즉시 스마트폰 후면 카메라로 실시간 프리뷰와 녹화가 실행되도록 폴백 로직을 강화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 실시간 점검 화면(`InspectionLiveScreen`) "AI 음성 안내" 및 "실시간 인식 구역" 카드 제거. 실시간 촬영 중 사용자가 카메라 프리뷰 및 핵심 점검 체크리스트 가이드에 온전히 집중할 수 있도록 화면의 불필요한 보조 카드들을 정리하여 쾌적한 점검 뷰를 구성함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 촬영 전 경고 화면(`InspectionPermissionWarningScreen`) "허가를 확인했고 촬영을 계속합니다" 확인 버튼 화면 최하단(`bottomAction`) 고정 배치. 통신비밀보호법 및 촬영 동의 법적 고지 카드의 길이와 무관하게 사용자가 화면 최하단에서 안정적으로 동의하고 다음 단계로 진입할 수 있도록 개선함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 점검 준비 화면(`InspectionPrepScreen`) "점검 시작하기" 버튼 화면 최하단(`bottomAction`) 고정 배치. 촬영 주의 경고 카드 및 점검 매물/촬영 전 확인 안내 카드의 스크롤 위치와 무관하게 사용자가 한 손으로 언제든 즉시 점검을 시작할 수 있도록 하단 액션바로 고정 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 점검할 매물 선택 화면(`PropertySelectScreen`) UI 레이아웃 개선. "다른 매물이 없나요?" 안내 카드를 매물이 하나도 없을 때만 조건부로 표시하도록 변경하고, "선택한 매물로 계속하기" 메인 CTA 버튼을 스크롤 영역에서 분리하여 화면 최하단(`bottomAction`)에 고정 배치함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 우측 하단 원형 플로팅 액션 버튼(Circular FAB `+`) 배경색을 세잎세잎 시그니처 그린(`Green`)으로 변경. 앱 전반의 딥그린/에메랄드 톤앤매너와 조화롭게 어우러지도록 브랜드 그린 컬러를 일관되게 적용함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 "새 매물 등록" 하단 가로 직사각형 버튼을 우측 하단 원형 플로팅 액션 버튼(Circular FAB `+`)으로 변경. 리스트 스크롤 시 화면 하단 영역을 가리지 않고 쾌적하게 목록을 탐색할 수 있으며, 엄지손가락으로 손쉽게 탭하여 신규 매물을 추가할 수 있도록 직관적인 모바일 표준 FAB UX를 완성함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 지도 내 위치 이동 0ms 초고속 반응 최적화(`fastCurrentCoordinates`). 불필요한 주소 문자열 역지오코딩 지연(3~6초)을 제거하고 안드로이드 기기 캐시 좌표(LastKnownLocation)를 즉시 반환하도록 개선하여, 지도 우측 하단 조준점(🎯) 버튼 탭 시 및 지도 진입 시 딜레이 없이 즉시 내 위치로 카메라가 부드럽게 이동하도록 고도화 완료. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 지도 오버뷰 초기 카메라 내 위치(GPS) 자동 중심 이동 및 미니멀 핀 인터랙션 적용. 지도 진입 시 사용자 현재 GPS 위치를 비동기로 조회하여 지도의 초기 중심을 내 위치로 부드럽게 이동시키고 블루 펄스 마커를 표시함. 지도 위 매물 핀은 기본 상태에서 텍스트 말풍선 없는 초소형 레드 핀(Zero Collision)으로 노출하고, 핀을 탭했을 때만 에메랄드 말풍선 뱃지와 하단 상세 카드가 팝업되는 인터랙션으로 전면 개선함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 및 지도 화면 전반 폰트/텍스트 크기 정밀 축소(Typography Refinement). 매물 목록 카드 타이틀(`15.5sp ➔ 14.5sp`), 주소(`12.5sp ➔ 11.5sp`), 상세 화면 헤더(`22sp ➔ 19sp`), 3단 메트릭 타일(`14.5sp ➔ 13sp`), 지도 핀 말풍선 텍스트(`11.5sp ➔ 9.5sp`) 및 하단 프리뷰 카드의 폰트 크기와 패딩을 아담하고 세련된 비율로 일괄 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 우측 상단 지도 아이콘 및 다중 매물 핀포인트 지도 오버뷰(`PropertyMapOverviewScreen`, `Route.PropertyMap`) 구현. 확대·축소·이동·회전·틸트 전체 인터랙티브 제스처를 지원하는 풀스크린 카카오 맵을 탑재하고, 등록된 모든 매물의 주소를 비동기 지오코딩하여 매물명 말풍선과 핀 마커로 일괄 렌더링함. 핀 탭 시 카메라 이동 및 하단 플로팅 프리뷰 카드(보증금·월세·관리비·상세보기 CTA) 노출 기능 완비. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 하단 "방문 전 확인" 안내 카드 제거. 하단 액션 영역에서 설명 카드를 없애고 깔끔하게 **"새 매물 등록"** 메인 버튼만 컴팩트하게 노출되도록 정리함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 상단 헤더 문구("점검할 매물을 관리해요") 및 뱃지("등록 매물 N개") 완전 제거. 타이틀("매물") 바로 아래에서 즉시 매물 리스트 카드가 시작되도록 상단 여백을 극대화하고 미니멀한 UI로 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 리스트 카드 높이 슬림화(Compact Property Card). 세로 패딩을 `15dp`에서 `10dp`로, 텍스트 간격을 `4dp`에서 `2dp`로 축소하고 라운드 모서리를 `14dp`로 정돈하여 목록을 한눈에 더 많이 탐색할 수 있는 콤팩트한 리스트 뷰를 완성함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 삭제 정책 소프트 삭제(Soft Delete) 연동 완료. 이제 임장 기록이 있는 매물도 사용자 스와이프 삭제 시 차단 에러 없이 부드럽게 삭제(목록에서 숨김) 처리되며, 과거 임장 세션 및 리포트 데이터는 DB에 안전하게 보존됨. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 상세 화면 Kakao 지도 카드 컴팩트화(Clean Edge-to-Edge Map Card). 지도 박스를 둘러싸고 있던 불필요한 헤더 라벨("위치 및 지도", "Kakao 지도")과 외부 패딩을 모두 제거하고, 16dp 라운드 카드 전면에 오직 깔끔한 지도 뷰와 매물 핀포인트만 콤팩트하게 렌더링되도록 단순화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 상세 화면 Kakao 지도 줌 레벨 최적화(Zoom Level 18) 적용. 매물 건물과 주변 블록·도로망의 균형감이 가장 자연스럽게 드러나도록 지도 줌 레벨을 `18`로 조정함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 상세 화면 Kakao 지도 제스처 잠금(Static Map Pinpoint) 적용. 화면 상하 스크롤 시 지도가 밀리거나 움직이지 않도록 `GestureType.entries` 전체(Pan, Zoom, Rotate, Tilt 등)에 대해 `setGestureEnable(false)`를 적용하여 매물 위치 핀포인트가 정중앙에 단단히 고정된 정적 프리뷰 뷰포트로 최적화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 상세 화면 "점검 결과 리포트"와 "이 매물 임장 시작" 사이에 주소 기반 Kakao 지도 핀포인트 카드(`PropertyKakaoMapCard`) 연동. 매물 주소를 기반으로 비동기 좌표 지오코딩(`KakaoAddressSearch.resolveAddressLocation`)을 수행하고, 카카오 벡터맵 위에 매물명 말풍선 뱃지와 선명한 레드 핀포인트 마커를 렌더링함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 상세 화면 중복 기본 정보 카드 제거 및 3단 KPI 메트릭 그리드 중심 정돈. 상단에 이미 표시되는 주소 및 3단 타일(보증금·월세·관리비)과 중복되던 하단 기본 정보 테이블 카드를 완전히 제거하고, 화면을 **[헤더 타이틀 & 주소] ➔ [3단 금융 메트릭 카드] ➔ [리포트 카드] ➔ [임장 시작 버튼]**의 군더더기 없는 베스트 프랙티스 레이아웃으로 간결화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 매물 탭 리스트 카드 간소화(Minimalist Property Card) 적용. 불필요한 태그("점검 예정") 및 하단 안내 문구("눌러서 상세 보기" / "이 매물 선택됨")를 제거하고, 매물 **제목(`property.name`)**과 **주소(`property.address`)**만 직관적이고 깔끔하게 표시하도록 단일화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`).
- 2026-08-24 — 홈 화면 "점검 시작하기" 히어로 카드 버튼 높이 슬림화(Compact Action Card) 적용. 상하 패딩을 `15dp`에서 `9dp`로 축소하고, 좌측 아이콘 박스를 `50dp`에서 `38dp`(`RoundedCornerShape(11dp)`, 아이콘 `20dp`)로 리사이징하여 상단 안경 연결 카드와 시각적 균형감이 맞는 콤팩트한 비율로 정돈함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실기기 안경 연동 터치는 실기기 미검증임.
- 2026-08-24 — 홈 화면 Meta Ray-Ban AI Glass 연결 카드 높이 슬림화(Compact Sleek HUD Profile) 적용. 세로 패딩을 `13dp`에서 `8dp`로 축소하고, 좌측 3D 클레이 뷰포트 크기를 `56x44dp`에서 `48x34dp`로, 안경 캔버스를 `36x22dp`로 조율하여 카드 전체 높이를 슬림하고 콤팩트한 캡슐형 HUD 바로 최적화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실기기 안경 연동 터치는 실기기 미검증임.
- 2026-08-24 — 홈 화면 Meta Ray-Ban AI Glass 연결 카드 배경 진한 녹색(Deep Emerald Green) 글래스모피즘 및 안경 3D 클레이모피즘(Claymorphism) 전면 구현. 카드 바탕을 딥 포레스트 에메랄드/다크 파인 그린(`Color(0xFF0F3828)` ~ `Color(0xFF08261B)`)으로 전환하고, 좌측 AR 안경을 푹신하고 도톰한 3D 클레이 볼륨 바디(하단 부드러운 드롭 섀도우, 상단 크레스트 펄 하이라이트, 안쪽에 쏙 들어간 유광 렌즈 캐비티, 3D 반사 물방울 스팟, 도톰한 키홀 브릿지/힌지)로 재설계함. 타이포그래피는 딥그린 대비 화이트-민트 볼드 텍스트로 시인성을 극대화함. `:app:testDebugUnitTest` 158개 태스크 전원 통과 (`BUILD SUCCESSFUL`). 실기기 안경 연동 터치는 실기기 미검증임.
- 2026-08-24 — 구역 관찰 화면 하단을 시스템 내비게이션 영역과 분리된 고정 Bottom Surface로 개선하고, 54dp 주황색 `매물 상세로 돌아가기` CTA를 배치함. 관찰 목록 아래에 `#FFF0E4` 안내 카드와 전체 비확정 안내 문구를 추가하고, 확인 필요 배지에 아이콘을 함께 표시함. `:app:testDebugUnitTest` 및 수정 파일 진단 통과. 실기기 화면 위치와 실제 백스택 복귀는 미검증임.
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

- 2026-08-24 — 점검 준비 화면에서 촬영 허가 경고 카드를 제거하고, 별도 `InspectionPermissionWarningScreen`에 밝은 주황 안내 카드·3개 직접 확인 체크·모두 확인해야 활성화되는 하단 `허가를 확인했고 촬영을 계속합니다` 버튼을 적용함. `:app:compileDebugKotlin`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함. 새 화면의 실제 터치·시각 확인은 설치된 기기에서 추가 확인이 필요함.

- 2026-08-24 — 임장 생성 시 매물 ID와 음성 기록을 기기 내부에 연결하고, 녹음 WAV 경로·STT 원문·짧은 요약을 매물별 최근 기록으로 보존하도록 `VoiceRecordArchive`를 추가함. 매물 상세에 `점검 음성 기록` 카드와 `음성 녹음 재생`·`음성 요약 보기` 버튼을 추가했으며, `:app:compileDebugKotlin`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함. 실제 녹음 후 STT 결과와 매물 상세 재생은 새 점검 1회로 실기기 확인이 필요함.

- 2026-08-24 — 앱 아이콘을 디자인 원본 `design/assets/generated/brand-character.png` 그대로 `res/drawable`에 복사하고 `AndroidManifest.xml`의 기본·원형 아이콘으로 연결함. 촬영 종료 확인 화면에서는 `촬영 구역 상세`와 구역별 목록을 제거하고, 안내 문구를 `분석 결과는 확인이 필요한 내용이에요. 사진을 확인하고 직접 결정하세요!`로 수정함. 원본과 앱 리소스의 SHA-256 일치, `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함.

- 2026-08-24 — 2초짜리 Compose 시작 화면을 유지하되 집 아이콘을 원본 캐릭터 이미지로 교체하고 260dp로 확대함. 아래 `세입세잎`과 `초보 세입자를 위한 SAFE GUIDE` 문구를 함께 표시함. `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과하고 실제 기기 시작 화면에서 캐릭터와 문구를 확인함.

- 2026-08-24 — 매물 상세의 `음성 요약 보기`를 팝업이 아닌 `점검 음성 요약` 페이지 이동으로 변경함. 요약을 먼저 보이고 `전체 STT 보기`를 눌러야 원문을 펼치며, STT 저장 완료 뒤 매물 카드와 요약 화면이 최신 결과를 다시 읽도록 보완함. 변환 결과가 없는 경우 저장된 PCM/WAV로 재시도할 수 있음. `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함. 실제 음성으로 요약·원문이 채워지는지의 재확인은 새 점검에서 필요함.

- 2026-08-24 — Android 12 이상 시스템 시작 화면이 앱 아이콘을 별도로 그려 2단계처럼 보이던 문제를 `core-splashscreen` 시작 테마로 통합함. 시스템 단계는 2번 화면과 같은 배경·투명 아이콘으로 처리하고, 실제 브랜딩 문구와 캐릭터는 기존 Compose 로딩 화면에서만 표시함. `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함.

- 2026-08-24 — 이전 버전에서 생성돼 매물 연결 정보가 없던 로컬 WAV는 음성 요약 화면을 처음 열 때 현재 매물의 최근 미연결 녹음으로 한 번 연결하고 Android STT 변환을 자동 재시도하도록 보완함. 기존 연결이 있는 녹음은 건드리지 않음. `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과했으며, 실제 STT 인식 결과는 사용자가 음성 요약 화면을 다시 열어 확인해야 함.

- 2026-08-24 — 기존 녹음 가져오기와 STT 재시도 처리를 음성 요약 화면뿐 아니라 매물 상세의 `점검 음성 기록` 카드가 열릴 때도 실행하도록 수정함. 매물 상세에서는 확인 중 문구를 먼저 표시하고, 최신 미연결 WAV를 찾은 뒤 재생·요약 버튼을 표시함. `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함.

- 2026-08-24 — Galaxy STT 로그에서 PCM 파일을 한 번에 전달할 때 `Audio buffer overflow`가 발생하는 것을 확인함. Android STT 입력을 파일 직접 전달에서 pipe 기반 50ms PCM 스트리밍으로 변경하고, 45초 안에 결과·오류가 없으면 변환 상태를 종료하도록 보완함. `:app:assembleDebug`, Galaxy SM-S911N(Android 16) `:app:installDebug`를 통과함. 새 STT 결과의 실제 표시 검증은 기기에서 다시 시도해야 함.
