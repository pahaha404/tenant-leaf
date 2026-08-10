# 개발 기본 환경

## 공통 확인

PowerShell에서 아래 명령을 실행해 도구 설치 상태를 확인합니다.

```powershell
.\scripts\check-prerequisites.ps1
```

## 환경 변수

`.env.example`을 참고해 개인 개발 환경에만 `.env`를 만듭니다. 실제 키와 비밀번호는 채팅, 문서, GitHub, 코드에 기록하지 않습니다.

## 개발 단위

| 단위 | 경로 | 기본 도구 |
| --- | --- | --- |
| Android 앱 | `apps/android` | Android Studio, Kotlin, JDK 17 |
| API | `services/api` | Kotlin, Spring Boot, PostgreSQL, Redis |
| AI 작업자 | `services/ai-worker` | Python 또는 Kotlin, 큐, AI 제공자 |
| AI 학습·평가 | `ml` | Python 3.11+, GPU 환경은 선택 |

각 서비스의 실제 실행 명령은 빌드 파일을 추가할 때 해당 폴더의 README에 기록합니다. 아직 구현되지 않은 실행 명령을 문서에 가정해서 쓰지 않습니다.
