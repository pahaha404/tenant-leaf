# 로컬 Docker 환경

로컬 백엔드 환경에서는 PostgreSQL과 JPEG 객체 저장소인 MinIO를 실행합니다. API와 AI 작업자는 Docker Compose에 포함하지 않습니다.

저장소 루트에서 환경 파일을 준비하고 PostgreSQL과 MinIO를 시작합니다.

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f server/infra/docker/compose.yml up -d
```

상태를 확인합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml ps
```

두 서비스가 모두 `healthy`가 되면 사용할 수 있습니다. MinIO 관리 화면은 `http://localhost:9001`이며 로컬 계정은 `.env`의 `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`를 사용합니다. 관리 화면과 서명 URL을 외부에 공개하지 않습니다.

PostgreSQL과 MinIO를 중지합니다.

```powershell
docker compose --env-file .env -f server/infra/docker/compose.yml down
```

`down`은 DB와 JPEG 데이터 볼륨을 보존합니다. 테스트 데이터를 포함한 로컬 볼륨까지 삭제하려는 경우에만 `down -v`를 사용합니다.
