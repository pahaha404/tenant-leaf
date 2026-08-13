# 로컬 Docker 환경

현재 백엔드 기본 구조에서는 PostgreSQL 하나만 실행합니다. API와 AI 작업자는 Docker Compose에 포함하지 않습니다.

저장소 루트에서 환경 파일을 준비하고 PostgreSQL을 시작합니다.

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f server/infra/docker/compose.yml up -d
```

상태를 확인합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml ps
```

PostgreSQL을 중지합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml down
```

`down`은 데이터 볼륨을 보존합니다. 테스트 데이터를 포함한 로컬 볼륨까지 삭제하려는 경우에만 `down -v`를 사용합니다.
