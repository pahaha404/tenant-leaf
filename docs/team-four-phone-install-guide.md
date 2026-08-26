# 팀원용: 4인 휴대전화 시연 앱 설치 안내

## 제일 쉬운 방법: APK만 받아 설치

팀장은 발표 노트북에서 서버를 켠 뒤 아래 명령으로 APK 네 개를 만든다.

```powershell
.\scripts\build-demo-apks.ps1 -LanAddress <발표 노트북 Wi-Fi IP>
```

생성 위치는 `artifacts/demo-apks/`이다. 팀장은 아래 파일을 각자 폰으로 보내고, 받은 사람은 자기 파일 하나만 설치한다.

| 사람/폰 | 설치할 파일 |
| --- | --- |
| A | `tenant-leaf-judge-a.apk` |
| B | `tenant-leaf-judge-b.apk` |
| C | `tenant-leaf-judge-c.apk` |
| D | `tenant-leaf-judge-d.apk` |

전송은 Quick Share, 카카오톡 나에게 보내기, USB 중 편한 방법을 사용한다. 설치할 때 Android에서 `이 출처의 앱 허용`을 묻으면 이번 테스트에만 허용한다.

각 폰은 **반드시 발표 노트북과 같은 Wi-Fi 또는 노트북 핫스팟**에 연결한다. 같은 서버를 쓰지만 A~D 데이터는 서로 보이지 않는다.

## Android Studio로 각자 직접 설치하는 방법

1. GitHub 저장소 `pahaha404/tenant-leaf`을 연다.
2. 좌측 상단 브랜치 메뉴에서 현재는 `feature/demo-lan-server`를 선택한다.
   - 이 PR이 `develop`에 머지된 뒤에는 `develop`을 선택하면 된다.
3. Android Studio Terminal에서 실행한다.

```powershell
git fetch origin
git switch feature/demo-lan-server
git pull --ff-only origin feature/demo-lan-server
```

4. 각자 `android/local.properties` 맨 아래에 아래를 추가한다. `<발표 노트북 Wi-Fi IP>`는 팀장이 알려 준 값으로 바꾼다.

```properties
TENANT_LEAF_DEBUG_API_BASE_URL=http://<발표 노트북 Wi-Fi IP>:8080/api/v1/
TENANT_LEAF_DEMO_USER=judge-a
```

5. `TENANT_LEAF_DEMO_USER`는 사람마다 하나씩 다르게 쓴다.

| 사람/폰 | 값 |
| --- | --- |
| A | `judge-a` |
| B | `judge-b` |
| C | `judge-c` |
| D | `judge-d` |

6. Android Studio에서 `Sync Project with Gradle Files`를 한 번 누르고, 휴대전화를 연결해 `Run ▶`을 누른다.

`local.properties`는 각자 PC에만 있는 설정 파일이라 GitHub에 올리거나 공유하지 않는다.

## 발표 전 30초 확인

- 발표 노트북에서 `scripts/start-demo-lan-server.ps1` 실행
- 노트북과 네 폰이 같은 Wi-Fi인지 확인
- 각 폰에서 매물 하나 등록
- 다른 폰 매물 탭에 내 매물이 보이지 않는지 확인

서버가 켜져 있지 않거나 Wi-Fi가 다르면 매물 등록이 실패한다.
