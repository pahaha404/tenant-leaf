# ai-worker

PostgreSQL의 JPEG 분석 작업을 가져와 MinIO 객체를 내려받고, 두 단계 YOLO를 별도 프로세스로 실행하는 Python 비동기 Worker입니다. Spring Boot의 HTTP 요청 처리 과정에서는 추론을 실행하지 않습니다.

## 처리 범위

- `UPLOADED` JPEG에 대해 생성된 `media_analysis_jobs` 작업 선점
- `QUEUED → ANALYZING → COMPLETED/FAILED` 상태 전이
- MinIO JPEG의 형식, 크기(최대 2MiB)와 실제 픽셀 크기 재검증
- Binary YOLO와 다중 클래스 YOLO 결과 병합
- 원시 탐지 결과와 모델 버전을 PostgreSQL에 저장
- bbox crop을 MinIO의 접근 제어된 파생 객체로 저장

원본 영상·갤러리 URI·휴대전화 경로, STT와 리포트 생성은 MVP Worker 범위가 아닙니다. AI 결과는 하자 확정이 아닌 확인 필요 관찰 후보의 원시 근거입니다.

## 실행 준비

저장소 루트의 `.env.example`을 참고해 로컬 `.env`를 설정합니다. Worker가 루트 `.env`를 자동으로 읽으며, 동일한 이름의 프로세스 환경변수가 있으면 환경변수를 우선 사용합니다. 모델 가중치 `binary/best.pt`, `multiclass/best.pt`는 Git이 아닌 접근 제어된 배포 경로에 별도로 준비해야 합니다.

```powershell
cd server/ai-worker
py -3.14 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

Python 3.14는 표준 CPython x64의 `3.14.0` 또는 `3.14.2 이상`을 사용합니다. `torch==2.13.0`과 `torchvision==0.28.0` 조합을 사용하며, Torchvision 배포 조건 때문에 Python `3.14.1`은 지원하지 않습니다. free-threaded 빌드(`3.14t`)는 별도 검증 전까지 사용하지 않습니다.

작업 한 건만 처리:

```powershell
python -m tenant_leaf_worker.worker --once
```

계속 대기하며 처리하려면 `--once`를 빼고 실행합니다. 비밀번호와 연결 문자열은 로그나 Git에 남기지 않습니다.

## 테스트

```powershell
$env:PYTHONPATH = (Get-Location).Path
python -m unittest discover -s tests -v
```

가중치가 없는 환경에서도 결과 계약 단위 테스트는 실행할 수 있지만 실제 YOLO smoke test는 실행할 수 없습니다.
