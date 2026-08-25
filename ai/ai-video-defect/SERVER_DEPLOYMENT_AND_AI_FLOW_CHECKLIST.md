# 서버 배포 및 AI 이미지 묶음 처리 체크리스트

작성일: 2026-08-20  
최종 갱신일: 2026-08-24

## 1. 확정된 처리 흐름

백엔드가 영상 샘플링과 Blur·밝기·중복 필터를 완료한 뒤 시간순 이미지 묶음을 Python AI 처리 모듈에 전달한다. Python에서는 영상을 열거나 이미지 품질을 다시 검사하지 않는다.

```text
백엔드
  ├─ 영상 샘플링·이미지 품질 선별
  └─ 시간순 이미지 저장 및 작업 생성
       ↓
AI Python 모듈
  ├─ 이미지 파일 또는 시간순 이미지 폴더 확인
  ├─ Gemini 공간 분류와 연속 결과 안정화
  ├─ 전체 이미지 Binary YOLO 추론
  ├─ 전체 이미지 다중 클래스 YOLO 추론
  ├─ 두 모델 결과 병합
  ├─ 이미지별 탐지 결과와 observation crop 생성
  └─ 공간·하자·crop 통합 JSON 생성
       ↓
백엔드
  └─ AI 결과 저장 및 사용자 리포트 연결
```

## 2. 서버에 설치할 항목

필수 설치:

- Python 3.11 또는 3.12 실행 환경 권장
- `ultralytics==8.4.117`
- PyTorch 및 torchvision
- 서버 GPU를 사용할 경우 서버 CUDA와 호환되는 PyTorch 빌드
- `opencv-python-headless` — 이미지 읽기와 crop 생성
- `Pillow` — 이미지 검증 및 처리
- `numpy` — 이미지 배열 처리
- `google-genai==2.19.0` — Gemini 공간 분류 API

가상환경을 만든 다음 서버 GPU·CUDA에 맞는 PyTorch와 torchvision을 공식 설치 명령으로 먼저 설치하고, 이어서 `requirements.txt`를 설치한다. GPU가 없는 서버는 CPU용 PyTorch를 설치한다.

로컬 `.venv`는 서버로 복사하지 않는다. 가상환경에는 생성 당시 Python의 절대 경로가 들어가므로 서버에서 새로 생성해야 한다.

Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

공간 분류의 기본 모델은 `gemini-3.5-flash-lite`다. `gemini-2.5-flash-lite`는 신규 프로젝트에서 404가 발생할 수 있으므로 배포 설정에서 사용하지 않는다.

AI worker 프로세스에 `GEMINI_API_KEY`를 비밀 환경변수로 설정하고 변경 후 worker를 재시작한다. systemd·Docker·queue worker는 로그인 셸과 환경이 다를 수 있으므로 실제 worker 설정에 등록해야 한다. 키는 코드, manifest, DB, 로그, Git에 저장하지 않는다.

```bash
export GEMINI_API_KEY="발급받은_API_KEY"
```

```powershell
$env:GEMINI_API_KEY="발급받은_API_KEY"
```

네트워크는 AI worker에서 `generativelanguage.googleapis.com`으로 나가는 HTTPS 443을 허용한다. Gemini 때문에 새 인바운드 포트를 열 필요는 없다. 대상 이미지는 외부 Gemini API로 전송되므로 데이터 정책과 사용자 동의 범위를 확인한다.

설치 후 다음 명령을 worker와 같은 가상환경에서 확인한다.

```bash
python -m pip show google-genai
python -c "from google import genai; print('google-genai import OK')"
python -m training.process_image_batch_room_defect --help
```

API 연결 없이 YOLO·crop·JSON 흐름만 점검할 때는 실행 명령에 `--room-provider disabled`를 추가한다. 상세 설치·오류 대응은 `BACKEND_ROOM_CLASSIFICATION_API_CHANGES.md`의 6절을 따른다.

Linux 서버:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

설치하지 않아도 되는 항목:

- LangGraph, LangChain, LlamaIndex
- 학습 데이터 전체
- Roboflow 원본 압축 파일
- DACON, Places365 등 테스트·학습용 원본 데이터
- `runs/` 전체 학습 결과

## 3. 서버로 옮길 파일

### 폴더 구조

파일은 `ai-video-defect` 폴더 하나로 전달하되, 내부의 `training/`과 `models/` 구조는 유지한다.

```text
ai-video-defect/
├── requirements.txt
├── BACKEND_MODEL_OUTPUT_CONTRACT.md
├── BACKEND_ROOM_CLASSIFICATION_API_CHANGES.md
├── SERVER_IMAGE_ROOM_CLASSIFICATION.md
│
├── training/
│   ├── __init__.py
│   ├── gemini_room_classifier.py
│   ├── process_image_batch_room_defect.py
│   ├── process_images_two_stage.py
│   └── predict_dacon_two_stage.py
│
└── models/
    └── active/
        └── two_stage_negative_rot4/
            ├── manifest.json
            ├── binary/
            │   └── best.pt
            └── multiclass/
                └── best.pt
```

`ai-video-defect` 폴더 전체를 압축해 전달할 수 있다. 서버에서는 압축을 푼 폴더를 작업 디렉터리로 사용한다.

```bash
cd /server/ai-video-defect
python -m training.process_image_batch_room_defect \
  --input /server/input/property_001/images \
  --manifest /server/input/property_001/manifest.json \
  --job-id property-001 \
  --output /server/output/property-001
```

모든 파일을 한 디렉터리에 평평하게 넣으면 Python 모듈 import와 기본 모델 경로 탐색이 실패할 수 있으므로 위 구조를 유지한다.

### 선택

```text
training/visualize_dacon_two_stage.py  # 결과 박스 시각화가 필요할 때
```

현재 활성 모델은 다음과 같다. 서버 묶음에는 이미지 묶음 통합 실행 파일과 그 내부에서 사용하는 `process_images_two_stage.py`를 모두 포함해야 한다.

- 모델 계열: **YOLO26n 20 epoch, 전체 이미지 2중 추론**
- Binary YOLO: `models/active/two_stage_negative_rot4/binary/best.pt`
- 다중 클래스 YOLO: `models/active/two_stage_negative_rot4/multiclass/best.pt`
- 모델 설정 및 검증 정보: `models/active/two_stage_negative_rot4/manifest.json`

승격 검증값은 고정 validation 1,094장, IoU 0.30, confidence 0.0325 기준 TP 2,125 / FP 10,983 / FN 269 / 미탐률 11.24%다. 이전 YOLOv8n보다 FN은 10건 증가했지만 FP가 905건 감소해 2026-08-21 활성 모델로 교체했다.

## 4. 현재 AI 구현 파일

현재 이미지 묶음 기반 AI 처리는 다음 파일로 구성되어 있다.

- 공간 분류·안정화·YOLO 통합 실행: `training/process_image_batch_room_defect.py`
- Gemini 공간 분류: `training/gemini_room_classifier.py`
- 이미지 폴더 2중 추론 실험: `training/predict_dacon_two_stage.py`
- 서비스 이미지 처리·crop·JSON: `training/process_images_two_stage.py`
- 과거 영상 테스트 보존: `training/process_video_two_stage.py`
- 활성 모델: `models/active/two_stage_negative_rot4/`

이미지 모듈 실행 예시:

```powershell
python -m training.process_image_batch_room_defect `
  --input input/property_001/images `
  --job-id property-001 `
  --output reports/property-001-image-test
```

통합 모듈은 입력된 모든 이미지를 파일명 순서로 처리한다. 대표 하자 crop은 bbox 기준 사방 15% 여백을 포함해 생성한다. 샘플링·이미지 선별·중복 제거는 백엔드가 AI 모듈에 전달하기 전에 처리한다.

출력 구조:

```text
/server/output/{jobId}/
  result.json
  evidence/   # 하자가 탐지된 원본 이미지
  crops/      # 리포트에 사용할 하자 영역 crop
```

## 5. 백엔드 전달 출력 기준

기존에 백엔드에 전달한 문서를 현재 기준으로 우선한다. 따라서 좌표와 응답 형식은 다음 규칙을 사용한다.

- 좌표는 `xyxy` 순서의 원본 이미지 픽셀 좌표다.
- 좌표 원점은 이미지 왼쪽 위 `(0, 0)`이다.
- 응답에는 원본 이미지의 `width`, `height`를 포함한다.
- 탐지 결과 배열 이름은 `detections`다.
- 각 결과의 박스는 `box.left`, `box.top`, `box.right`, `box.bottom` 구조다.
- 결과가 없으면 `detections: []`을 반환한다.
- `unknown_defect`는 `classId: null`, `label: "unknown_defect"`로 반환할 수 있다.

```json
{
  "image": {
    "width": 1920,
    "height": 1080
  },
  "detections": [
    {
      "classId": 1,
      "label": "mold",
      "confidence": 0.87,
      "box": {
        "left": 120,
        "top": 80,
        "right": 640,
        "bottom": 410
      }
    }
  ]
}
```

이미지 처리에서는 입력 이미지마다 위 형식을 생성한다. `imageId`, `filename`, `jobId` 같은 작업 메타데이터는 이미지 결과의 별도 필드로 관리하고, 위 이미지 탐지 응답 형식 자체는 변경하지 않는다.

기존 `training/predict_dacon_two_stage.py`의 `predictions.json`은 이미지 실험용 결과다. 서비스 진입점 `training/process_image_batch_room_defect.py`의 최종 `result.json`은 공간 분류·공간 구간·이미지별 탐지·관찰 crop을 결합한다. 내부 `defect_analysis/result.json`은 기존 `image + detections` 형식을 유지한다.

리포트에 사용할 `observations[]` 항목에는 다음 필드가 추가된다.

```json
{
  "observationGroupId": "obs-0001",
  "label": "mold",
  "confidence": 0.87,
  "representativeFrameId": 42,
  "timestampSec": 21.0,
  "evidencePath": "/server/output/job-001/evidence/frame_00000042_21.000s.jpg",
  "cropPath": "/server/output/job-001/crops/obs-0001_mold.jpg",
  "cropImage": {
    "width": 620,
    "height": 390
  },
  "reviewStatus": "needs_review"
}
```

`evidencePath`는 원본 이미지 근거이고, `cropPath`는 하자 영역을 15% 여백과 함께 자른 리포트용 이미지다. 두 경로는 AI 서버 내부 경로이므로 백엔드는 저장소 URL로 변환해 사용자에게 제공한다.

## 6. 백엔드에서 구현할 API

백엔드는 이미지를 저장하고 AI 작업을 시작·조회한 뒤, AI 결과를 리포트에 연결해야 한다. 아래 API는 기능 테스트에 필요한 최소 범위다.

### 6-1. 백엔드 샘플링 완료 및 AI 작업 생성

```http
POST /api/v1/inspection-sessions/{sessionId}/image-jobs
Content-Type: multipart/form-data
```

요청:

- 백엔드는 사용자 영상을 받은 뒤 샘플링·품질 선별을 완료한다.
- 선별 이미지는 시간순 파일명으로 내부 폴더에 저장한다.
- 필요하면 `imageId`, `timestampSec`를 담은 manifest를 함께 생성한다.

응답 `202 Accepted`:

```json
{
  "jobId": "job-001",
  "sessionId": "session-001",
  "status": "queued"
}
```

백엔드는 이미지를 서버에 저장한 뒤 AI 모듈에 전달할 내부 입력 폴더 경로를 확보한다. 클라이언트에 서버 내부 파일 경로를 노출하지 않는다.

### 6-2. AI 작업 실행

```http
POST /internal/v1/image-jobs/{jobId}/run
```

백엔드는 작업에 저장된 이미지 폴더 경로를 사용해 다음 Python 명령을 실행한다.

```bash
python -m training.process_image_batch_room_defect \
  --input /server/input/{jobId}/images \
  --manifest /server/input/{jobId}/manifest.json \
  --job-id {jobId} \
  --output /server/output/{jobId}
```

작업 실행 방식은 백그라운드 worker 또는 queue를 사용한다. HTTP 요청을 이미지 분석이 끝날 때까지 붙잡아 두지 않는다.

### 6-3. 작업 상태 조회

```http
GET /api/v1/image-jobs/{jobId}
```

응답 예시:

```json
{
  "jobId": "job-001",
  "status": "processing",
  "progress": null,
  "error": null
}
```

`status`는 `queued`, `processing`, `completed`, `failed` 중 하나를 사용한다.

### 6-4. AI 결과 조회

```http
GET /api/v1/image-jobs/{jobId}/result
```

응답:

- AI 결과의 `images[]`를 저장·조회한다.
- 각 이미지 결과는 기존 백엔드 계약인 `image + detections[].box` 형식을 유지한다.
- `observations[]`는 이미지별 하자 관찰 결과로 리포트 생성에 사용한다.
- `evidencePath`와 `cropPath`는 인증된 이미지 조회 URL로 변환해 반환한다.

### 6-5. 근거 이미지 및 하자 crop 조회

```http
GET /api/v1/image-jobs/{jobId}/evidence/{imageId}
GET /api/v1/image-jobs/{jobId}/crops/{observationGroupId}
```

백엔드는 AI가 저장한 원본 이미지와 하자 crop을 인증된 사용자에게만 반환한다. 리포트에는 `cropUrl`을 기본 사진으로 사용하고, 사용자가 원본 근거를 확인할 수 있도록 `evidenceUrl`도 함께 제공한다.

### 6-6. 리포트 연결

```http
POST /api/v1/inspection-sessions/{sessionId}/report
GET  /api/v1/reports/{reportId}
```

백엔드는 `observations[]`를 리포트의 확인 필요 관찰·촬영 근거 사진·계약 전 확인 항목에 연결한다. `cropPath`는 리포트용 하자 사진으로, `evidencePath`는 원본 촬영 근거로 저장한다. 리포트 초안 생성·확정본 저장·목록/상세 조회·공유는 백엔드가 담당한다. AI 결과를 계약 확정이나 하자 확정으로 표현하지 않는다.

## 7. 백엔드 API 구현 체크리스트

- [ ] 영상에서 시간순 이미지 샘플링·품질 선별 및 파일 저장
- [ ] 선택적 manifest 생성 또는 시간순 파일명 보장
- [ ] `jobId` 생성 및 작업 상태 저장
- [ ] AI worker가 Python 모듈을 실행하도록 연결
- [ ] `queued → processing → completed/failed` 상태 갱신
- [ ] `result.json` 저장 및 결과 조회 API
- [ ] `images[].image`와 `detections` 저장
- [ ] `observations[]`와 crop을 리포트 초안에 연결
- [ ] `evidencePath`, `cropPath`를 저장소에 업로드하고 URL로 변환
- [ ] 원본 이미지 및 하자 crop 인증 조회 API
- [ ] 리포트에서 crop 사진과 원본 근거 사진 연결
- [ ] 실패 시 오류 메시지 저장 및 재시도/실패 응답
- [ ] AI 결과를 사용자별·매물별로 분리 조회

실제 이미지 묶음을 사용한 서버 smoke test는 백엔드 API가 준비된 뒤 실행한다. 정확도 개선, threshold 조정, 추가 데이터 수집, 성능 최적화, UI 개선은 이 기능 체크리스트에 포함하지 않는다.

## 8. 배포 테스트 이후 LangGraph 도입 계획

LangGraph는 현재 서버 설치 및 배포 대상에 포함하지 않는다. 먼저 다음 순서까지 완료한다.

1. `ai-video-defect/` 서버 배포
2. 백엔드 이미지 샘플링 결과와 AI worker 연결
3. 실제 이미지 묶음 공간 분류·2중 YOLO 추론·JSON 생성 테스트
4. 원본 이미지·하자 crop·리포트 연결 테스트
5. 실패 상태와 재실행 흐름 확인

위 배포 테스트가 완료된 뒤 LangGraph 도입을 검토한다. 도입 시에는 모델 자체를 대체하지 않고 다음과 같은 AI 작업 흐름의 상태와 분기를 관리하는 용도로 사용한다.

- 처리 단계별 상태 관리와 실패 단계 재실행
- 모델 confidence에 따른 사용자 확인 또는 재분석 분기
- 공간 분류·하자 탐지·리포트 생성 단계 연결
- 사용자 확인 결과를 반영한 리포트 재생성

따라서 현재 `requirements.txt`에는 LangGraph를 추가하지 않으며, 배포 테스트 완료 후 별도 브랜치 또는 모듈에서 도입한다.
