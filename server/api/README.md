# 세입세잎 API

Kotlin과 Spring Boot 기반 백엔드 API 프로젝트입니다.

## 1. 필요한 프로그램과 버전

- JDK 21 LTS
- Docker Desktop과 Docker Compose
- Git

Gradle은 프로젝트에 포함된 Wrapper를 사용하므로 별도로 설치하지 않습니다. 저장소 루트에서 설치 상태를 확인합니다.

```powershell
.\scripts\check-prerequisites.ps1
```

## 패키지와 시작 클래스

- 기본 패키지: `com.tenantleaf.api`
- 시작 클래스: `com.tenantleaf.api.ApiApplication`

## 2. 환경 변수 준비

저장소 루트에서 예제 파일을 복사합니다. `.env`는 Git에 포함되지 않습니다.

```powershell
Copy-Item .env.example .env
```

기본 로컬 값은 `application.yml`의 안전한 개발용 기본값과 같습니다. 값을 변경했다면 API를 실행하는 PowerShell에도 같은 `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경 변수를 설정해야 합니다.

`OBJECT_STORAGE_ENDPOINT`는 API 서버가 MinIO에 연결할 주소이고, `OBJECT_STORAGE_PUBLIC_ENDPOINT`는 Android가 서명 URL로 접근할 주소입니다. 에뮬레이터는 기본값 `http://10.0.2.2:9000`을 사용합니다. 실제 휴대전화에서는 같은 Wi-Fi에 연결된 개발 PC의 IPv4 주소(예: `http://192.168.0.10:9000`)로 바꾸고 Windows 방화벽에서 필요한 로컬 개발 포트만 허용합니다.

### 로컬과 실배포 환경 분리

| 구분 | Android API 주소 | API 프로필 | PostgreSQL |
| --- | --- | --- | --- |
| 로컬 | `http://10.0.2.2:8080/api/v1/` | 기본 프로필 | Docker의 `localhost:5432` |
| 실배포 | `TENANT_LEAF_RELEASE_API_BASE_URL`로 지정한 HTTPS 주소 | `prod` | `DATABASE_URL`로 지정한 비공개 DB |

실배포에서는 `SPRING_PROFILES_ACTIVE=prod`와 아래 환경변수를 배포 환경의 Secret 설정으로 주입합니다. `application-prod.yml`에는 로컬 기본값이 없으므로 하나라도 빠지면 서버 시작이 실패합니다.

```text
DATABASE_URL
POSTGRES_USER
POSTGRES_PASSWORD
OBJECT_STORAGE_ENDPOINT
OBJECT_STORAGE_PUBLIC_ENDPOINT
OBJECT_STORAGE_ACCESS_KEY
OBJECT_STORAGE_SECRET_KEY
OBJECT_STORAGE_BUCKET
```

Android release 빌드에는 실제 HTTPS API 주소를 명시합니다.

```powershell
cd android
.\gradlew.bat :app:assembleRelease "-PTENANT_LEAF_RELEASE_API_BASE_URL=https://api.example.com/api/v1/"
```

실배포 PostgreSQL 포트는 인터넷에 공개하지 않고 API 서버에서만 접근하게 구성합니다. 실제 비밀번호와 연결 문자열은 Git 또는 APK에 넣지 않습니다.

## 3. PostgreSQL과 MinIO 시작

저장소 루트에서 실행합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml up -d
docker compose --env-file .env -f server/infra/docker/compose.yml ps
```

`postgres`와 `minio` 서비스가 모두 `healthy`로 표시될 때까지 기다립니다. MinIO는 분석용 JPEG 파일을 저장하고 PostgreSQL은 해당 파일의 식별자·구역·상태 같은 메타데이터만 저장합니다.

## 4. API 실행

새 PowerShell에서 실행합니다.

```powershell
cd server/api
.\gradlew.bat bootRun
```

Flyway는 API 시작 시 `src/main/resources/db/migration`의 마이그레이션을 자동 실행합니다.

## 5. 헬스 체크

API가 실행 중인 상태에서 다른 PowerShell을 열어 확인합니다.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

서버와 PostgreSQL이 정상이면 `status`가 `UP`입니다. 응답에는 비밀번호와 연결 문자열을 표시하지 않습니다. 미디어 API 호출에는 MinIO도 실행 중이어야 합니다.

## 6. 테스트

별도 테스트 PostgreSQL이 `healthy`인 상태에서 실행합니다. 테스트는 실제 로컬 매물 데이터가 있는 DB를 대상으로 실행하지 않습니다.

```powershell
cd server/api
.\gradlew.bat clean test
```

테스트는 Spring 애플리케이션 시작, Flyway 스키마와 매물·임장·미디어 API의 상태 전이 및 소유권 규칙을 확인합니다.

## 7. OpenAPI 기반 Kotlin 코드 생성

공통 계약인 `server/shared-types/openapi/openapi.yaml`에서 서버용 요청·응답 타입과 API 인터페이스를 생성합니다.

```powershell
cd server/api
.\gradlew.bat openApiValidate openApiGenerate
```

생성 결과는 `server/api/build/generated/openapi/src/main/kotlin`에 생기며 서버 컴파일 대상에 자동 포함됩니다. `build` 아래 파일은 직접 수정하거나 Git에 커밋하지 않고, 변경이 필요하면 원본 `openapi.yaml`을 수정한 뒤 다시 생성합니다.

`compileKotlin`, `test`, `build`, `bootRun`을 실행할 때도 검증과 생성이 자동으로 먼저 수행됩니다.

## 8. 종료

API를 실행한 PowerShell에서 `Ctrl+C`를 누릅니다. 그다음 저장소 루트에서 PostgreSQL과 MinIO를 중지합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml down
```

이 명령은 PostgreSQL과 MinIO 데이터 볼륨을 보존합니다.

## 9. JPEG 업로드 흐름

1. 종료된 임장에서 업로드 요청을 등록합니다.
2. 서버가 15분 동안 유효한 MinIO PUT URL을 돌려줍니다.
3. Android가 해당 URL에 `Content-Type: image/jpeg`로 JPEG를 직접 업로드합니다.
4. Android가 업로드 완료 API를 호출하면 서버가 실제 파일의 형식·크기·가로·세로를 확인합니다.
5. 확인된 메타데이터는 PostgreSQL에, JPEG 바이트는 MinIO에 남습니다.
6. 서버가 같은 `mediaId`의 분석 작업을 한 번만 만들고 상태를 `QUEUED`로 변경합니다.
7. 별도 Python Worker가 작업을 처리해 탐지 결과와 모델 버전을 PostgreSQL에 저장합니다.

원본 영상과 휴대전화 갤러리 URI는 이 API로 전송하지 않습니다.
Worker 설치와 실행 방법은 `server/ai-worker/README.md`를 따릅니다. 모델 가중치가 배포되지 않은 상태에서는 API 업로드까지 동작하지만 분석 상태가 `QUEUED`에 머뭅니다.

## 10. 자주 발생하는 오류

### Docker engine에 연결할 수 없음

Docker Desktop 왼쪽 아래가 `Engine running`인지 확인합니다.

### 5432 포트가 이미 사용 중

`.env`의 `POSTGRES_PORT`를 다른 값으로 변경하고, API를 실행하는 PowerShell의 `DATABASE_URL` 포트도 같은 값으로 설정합니다.

### Failed to configure a DataSource

PostgreSQL이 실행 중인지 확인하고 `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 값이 Compose 설정과 같은지 확인합니다.

### 비밀번호를 변경했는데 인증 실패

기존 Docker 볼륨에는 처음 만든 계정 정보가 남아 있습니다. 데이터 삭제가 괜찮은 로컬 개발 환경에서만 다음 명령으로 볼륨을 제거한 뒤 다시 시작합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml down -v
docker compose --env-file .env -f server/infra/docker/compose.yml up -d
```
