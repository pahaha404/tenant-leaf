# ai-worker

PostgreSQL의 JPEG 분석 작업을 임장 단위로 가져와 MinIO 객체를 내려받고, Gemini 구역 분류와 두 단계 YOLO·Gemini 하자 검증을 별도 프로세스로 실행하는 Python 비동기 Worker입니다. Spring Boot의 HTTP 요청 처리 과정에서는 추론을 실행하지 않습니다.

## 처리 범위

- 미디어 등록이 끝난 임장의 `UPLOADED`·`QUEUED` JPEG 작업을 한 묶음으로 선점
- `source_video_offset_ms` 순서로 JPEG를 내려받고 `mediaId`와 영상 시점 manifest 생성
- `QUEUED → ANALYZING → COMPLETED/FAILED` 상태 전이
- MinIO JPEG의 형식, 크기(최대 2MiB)와 실제 픽셀 크기 재검증
- Gemini로 `욕실/주방/거실·방/구역 확인 필요` 분류 후 사진 순서를 이용해 구역 안정화
- Binary YOLO와 다중 클래스 YOLO 결과를 병합하고 Gemini로 하자 후보 2차 검증
- 사진별 구역·불확실 여부·구역 모델 버전과 살아남은 BBOX를 PostgreSQL에 저장

원본 영상·갤러리 URI·휴대전화 경로와 STT는 MVP Worker 범위가 아닙니다. Worker가 모든 사진을 최종 상태로 바꾸면 API 서버의 기존 `ReportGenerationCoordinator`가 리포트를 자동 생성합니다. AI 결과는 하자 확정이 아닌 확인 필요 관찰 후보의 원시 근거입니다.

## 실행 준비

저장소 루트의 `.env.example`을 참고해 로컬 `.env`를 설정합니다. Worker가 루트 `.env`를 자동으로 읽으며, 동일한 이름의 프로세스 환경변수가 있으면 환경변수를 우선 사용합니다. `GEMINI_API_KEY`는 Worker 환경에만 두고 Android·Git·로그에 노출하지 않습니다. 모델 가중치 `binary/best.pt`, `multiclass/best.pt`는 Git이 아닌 접근 제어된 배포 경로에 별도로 준비해야 합니다.

```powershell
cd server/ai-worker
py -3.14 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

Python 3.14는 표준 CPython x64의 `3.14.0` 또는 `3.14.2 이상`을 사용합니다. `torch==2.13.0`과 `torchvision==0.28.0` 조합을 사용하며, Torchvision 배포 조건 때문에 Python `3.14.1`은 지원하지 않습니다. free-threaded 빌드(`3.14t`)는 별도 검증 전까지 사용하지 않습니다.

임장 배치 한 건만 처리:

```powershell
python -m tenant_leaf_worker.worker --once
```

계속 대기하며 처리하려면 `--once`를 빼고 실행합니다. 비밀번호와 연결 문자열은 로그나 Git에 남기지 않습니다.

## 테스트

```powershell
$env:PYTHONPATH = (Get-Location).Path
python -m unittest discover -s tests -v
```

가중치와 Gemini 키가 없는 환경에서도 결과 계약 단위 테스트는 실행할 수 있지만 실제 통합 분석 smoke test는 실행할 수 없습니다. 통합 분석의 기본 옵션은 `room-provider=gemini`, `defect-verifier=gemini`, 두 모델 `gemini-3.5-flash-lite`, 명확한 비하자 제외 기준 `0.90`입니다.
