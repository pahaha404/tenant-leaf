# 발표용 다중 휴대전화 서버

이 문서는 발표 노트북 한 대에서 세입세잎 API·PostgreSQL·MinIO·AI Worker를 실행하고, 네 대의 Android 휴대전화를 같은 Wi-Fi에 연결하는 방법이다.

## 데이터 분리 방식

Android Debug APK는 모든 API 요청에 `X-Demo-User` 헤더를 보낸다. 서버는 아래 네 값만 서로 다른 소유자로 취급한다.

| APK | 헤더 값 | 데이터 범위 |
| --- | --- | --- |
| A | `judge-a` | A 폰이 등록한 매물·임장·리포트만 |
| B | `judge-b` | B 폰이 등록한 매물·임장·리포트만 |
| C | `judge-c` | C 폰이 등록한 매물·임장·리포트만 |
| D | `judge-d` | D 폰이 등록한 매물·임장·리포트만 |

이 값은 발표용 분리 장치일 뿐 로그인이나 보안 인증이 아니다. 외부 공개 서비스에는 사용하면 안 되며, 실제 서비스는 JWT 인증으로 교체해야 한다.

## 발표 전 실행

1. 노트북과 휴대전화 네 대를 같은 Wi-Fi 또는 노트북 핫스팟에 연결한다.
2. 관리자로 연 PowerShell에서 처음 한 번만 TCP 8080·9000 인바운드 규칙을 만든다.

```powershell
New-NetFirewallRule -DisplayName 'Tenant Leaf API demo' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080
New-NetFirewallRule -DisplayName 'Tenant Leaf MinIO demo' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 9000
```

3. 저장소 루트에서 서버를 시작한다. 스크립트가 Wi-Fi IPv4를 찾아 API·Worker를 백그라운드에서 실행하고 MinIO 공개 주소도 맞춘다.

```powershell
.\scripts\start-demo-lan-server.ps1
```

4. 아래 둘 중 하나를 선택한다.

### 방법 A: Android Studio에서 팀원 각자가 설치

각 팀원은 GitHub에서 이 기능이 포함된 `develop`을 받은 뒤, 자기 PC의 `android/local.properties` 맨 아래에 두 줄을 추가한다. 이 파일은 Git에 올라가지 않으므로 각자 다른 값을 써도 충돌하지 않는다.

```properties
TENANT_LEAF_DEBUG_API_BASE_URL=http://192.168.2.125:8080/api/v1/
TENANT_LEAF_DEMO_USER=judge-a
```

| 설치할 폰 | `TENANT_LEAF_DEMO_USER` |
| --- | --- |
| A | `judge-a` |
| B | `judge-b` |
| C | `judge-c` |
| D | `judge-d` |

설정한 뒤 Android Studio에서 `Sync Project with Gradle Files`를 한 번 실행하고, 각자 연결한 휴대전화에 `Run ▶`을 누른다. 기존 앱의 캐시를 피하려면 설치 전 앱 데이터를 삭제하거나 앱을 삭제한다.

### 방법 B: 미리 만든 APK 설치

출력된 `Android API 주소`의 IP를 사용해 네 개 APK를 만든다.

```powershell
.\scripts\build-demo-apks.ps1 -LanAddress 192.168.0.10
```

생성 위치는 `artifacts/demo-apks/`이고 Git에 올리지 않는다. 각 APK를 해당 폰에 한 개씩 설치한다.

5. 각 폰에서 매물 하나를 등록한다. 다른 폰의 매물 탭에 보이지 않는지 확인한다. 이후 한 폰에서 임장을 끝내고, 해당 폰에서만 리포트가 보이는지 확인한다.

## 발표 당일 확인

- 노트북 절전 해제·전원 연결
- Docker Desktop 실행
- `http://localhost:8080/actuator/health`가 `UP`
- 휴대전화 모두 같은 Wi-Fi
- AI Worker 로그: `%TEMP%\tenant-leaf-ai-worker\worker.out.log`
- 실제 촬영·Gemini 결과는 네트워크와 API 할당량에 영향을 받으므로, 시연용 결과 화면도 준비

## 모바일 데이터만으로 실제 외부 임장하기

로컬 LAN 서버는 휴대전화가 노트북 Wi-Fi에 붙어 있을 때만 된다. 모바일 데이터로 노트북 없이 접속하려면 HTTPS 공용 서버 또는 인증된 터널이 필요하다.

현재 앱은 실제 계정/JWT가 없으므로 인터넷에 API·MinIO를 그대로 공개하면 안 된다. 외부 임장을 진행하려면 최소한 다음이 먼저 필요하다.

1. 실제 로그인 토큰 기반 소유자 분리
2. HTTPS API와 HTTPS 객체 저장소
3. Gemini 키·PostgreSQL·모델 가중치를 서버 Secret/비공개 저장소로 이전
4. API·MinIO 공개 권한, 접근 로그, 만료 URL 정책 점검

따라서 이번 변경은 발표 현장에서 안전하게 쓰는 LAN 서버까지 구현한다. 외부 공개 배포는 팀의 클라우드 계정과 도메인 권한을 정한 뒤 별도 배포 작업으로 진행한다.
