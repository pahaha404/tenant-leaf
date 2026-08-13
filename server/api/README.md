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

## 3. PostgreSQL 시작

저장소 루트에서 실행합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml up -d
docker compose --env-file .env -f server/infra/docker/compose.yml ps
```

`postgres` 서비스가 `healthy`로 표시될 때까지 기다립니다.

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

서버와 PostgreSQL이 정상이면 `status`가 `UP`입니다. API가 실행 중일 때 PostgreSQL을 중지하면 `status`가 `DOWN`으로 바뀝니다. 응답에는 비밀번호와 연결 문자열을 표시하지 않습니다.

## 6. 테스트

PostgreSQL이 `healthy`인 상태에서 실행합니다.

```powershell
cd server/api
.\gradlew.bat clean test
```

테스트는 Spring 애플리케이션 시작과 Flyway가 만든 `api_schema_marker` 테이블을 확인합니다.

## 7. OpenAPI 기반 Kotlin 코드 생성

공통 계약인 `server/shared-types/openapi/openapi.yaml`에서 서버용 요청·응답 타입과 API 인터페이스를 생성합니다.

```powershell
cd server/api
.\gradlew.bat openApiValidate openApiGenerate
```

생성 결과는 `server/api/build/generated/openapi/src/main/kotlin`에 생기며 서버 컴파일 대상에 자동 포함됩니다. `build` 아래 파일은 직접 수정하거나 Git에 커밋하지 않고, 변경이 필요하면 원본 `openapi.yaml`을 수정한 뒤 다시 생성합니다.

`compileKotlin`, `test`, `build`, `bootRun`을 실행할 때도 검증과 생성이 자동으로 먼저 수행됩니다.

## 8. 종료

API를 실행한 PowerShell에서 `Ctrl+C`를 누릅니다. 그다음 저장소 루트에서 PostgreSQL을 중지합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml down
```

이 명령은 PostgreSQL 데이터 볼륨을 보존합니다.

## 9. 자주 발생하는 오류

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
