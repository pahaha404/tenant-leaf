# 모바일 데이터 임시 시연

이 문서는 노트북에서 실행 중인 세입세잎 API와 MinIO를 Cloudflare Quick Tunnel로 잠깐 공개해, Wi-Fi가 아닌 모바일 데이터로 연결된 Android 폰 한 대를 테스트하는 방법이다.

## 범위와 주의

- 발표 전 테스트용이다. 실제 서비스 배포나 인증 구현이 아니다.
- API와 JPEG 저장소가 임시 공개되므로 실제 주소, 음성, 개인 사진을 사용하지 않는다.
- `.env`, Gemini 키, DB 비밀번호는 GitHub에 올리지 않는다.
- 종료 즉시 터널을 끈다.

## 한 번만 준비할 것

관리자 PowerShell에서 다음을 실행한다.

```powershell
winget install --id Cloudflare.cloudflared --exact
```

## 터널 시작

프로젝트 루트 PowerShell에서 실행한다.

```powershell
.\scripts\start-mobile-data-tunnel.ps1 -RestartApi
```

출력되는 `Android API 주소`는 매번 달라진다. 아래 스크립트는 그 주소를 자동으로 읽어 APK에 넣는다.

```powershell
.\scripts\build-mobile-data-apk.ps1 -DemoUser judge-a
```

생성된 `artifacts/mobile-data/tenant-leaf-mobile-data-judge-a.apk`를 테스트 폰에 설치한다. 폰의 Wi-Fi를 끄고 모바일 데이터를 켠 뒤 매물 등록, 점검, JPEG 업로드, 리포트를 차례로 확인한다.

## 종료

```powershell
.\scripts\stop-mobile-data-tunnel.ps1
```

터널만 중지하며 PostgreSQL과 MinIO 데이터는 지우지 않는다.
