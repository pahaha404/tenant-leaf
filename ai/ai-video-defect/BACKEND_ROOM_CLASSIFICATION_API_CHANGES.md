# 백엔드 전달용 공간 분류 API 변경사항

최종 갱신일: 2026-08-24  
대상: 백엔드 개발자

## 1. 변경 요약

백엔드가 영상에서 샘플링·품질 선별한 이미지 묶음을 AI Python에 전달하면, AI가 이미지별 공간 분류와 하자 탐지를 함께 수행한다.

기존 하자 탐지 좌표·클래스·crop 계약은 유지하고 다음 데이터만 추가한다.

- 이미지별 공간 분류 결과
- 연속 이미지 기반 공간 구간
- 하자 observation과 공간 구간의 연결

```text
백엔드 영상 샘플링·품질 선별
  → 시간순 이미지 폴더와 manifest 생성
  → AI Python worker 실행
  → Gemini 공간 분류·구간 안정화
  → 기존 2단계 YOLO·하자 crop 생성
  → 통합 result.json
```

## 2. 역할 분리

백엔드:

1. 영상 샘플링
2. Blur·너무 어두운 이미지·중복 이미지 제거
3. 통과한 이미지를 촬영 시간순으로 저장
4. 이미지 폴더, manifest, `jobId`를 AI worker에 전달
5. AI 결과와 근거 이미지를 저장하고 인증 URL로 변환

AI Python:

1. 전달된 이미지의 Gemini 공간 분류
2. 연속 이미지 결과 안정화와 공간 구간 생성
3. 기존 Binary YOLO·다중 클래스 YOLO 하자 추론
4. 공간·하자·근거 crop 통합 JSON 생성

AI Python은 영상 열기·프레임 추출·이미지 품질 필터링을 수행하지 않는다.

## 3. 공간 값

| 값 | 의미 |
|---|---|
| `bathroom` | 욕실 |
| `kitchen` | 주방 |
| `living_room` | 거실·침실형 주생활 공간 |
| `unknown` | 공간 전환, 근접 촬영, 불명확하거나 대상 외 공간 |

`entrance`, `window_frame`은 현재 범위에서 제외한다.

공간의 `unknown`과 하자의 `unknown_defect`는 다른 값이다.

- `unknown`: 공간을 확정하지 못함
- `unknown_defect`: 하자 후보는 있으나 하자 종류를 확정하지 못함

## 4. AI 입력 폴더 규칙

AI는 이미지 파일명을 오름차순으로 처리한다. 백엔드는 시간 순서가 유지되도록 파일명 앞에 0으로 채운 순번을 붙인다.

```text
/server/input/{jobId}/images/
├── 000001.jpg
├── 000002.jpg
└── 000003.jpg
```

지원 확장자는 `.jpg`, `.jpeg`, `.png`, `.bmp`, `.webp`다.

## 5. 권장 manifest

manifest는 선택 입력이지만, 백엔드 DB 이미지 ID 및 영상 시각과 결과를 정확히 연결하려면 전달을 권장한다.

```json
{
  "images": [
    {
      "filename": "000001.jpg",
      "imageId": "inspection-image-101",
      "timestampSec": 0.0
    },
    {
      "filename": "000002.jpg",
      "imageId": "inspection-image-102",
      "timestampSec": 1.0
    }
  ]
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `filename` | 필수 | 이미지 폴더 안의 실제 파일명, 중복 불가 |
| `imageId` | 권장 | 백엔드 DB의 이미지 식별자 |
| `timestampSec` | 권장 | 원본 영상 시작부터의 초 단위 시각 |

manifest가 없으면 AI가 `image-0001` 형식의 ID를 만들고 `timestampSec`는 `null`로 반환한다. manifest가 존재하지만 실제 이미지가 없거나 파일명이 중복되면 작업 전체를 실패 처리한다.

## 6. worker 실행 명령 변경

기존 하자 전용 이미지 실행 대신 공간 분류 통합 모듈을 실행한다.

```bash
python -m training.process_image_batch_room_defect \
  --input /server/input/{jobId}/images \
  --manifest /server/input/{jobId}/manifest.json \
  --job-id {jobId} \
  --output /server/output/{jobId}
```

manifest를 생성하지 않는 경우 `--manifest` 옵션을 제외한다.

AI worker 실행 환경에는 `GEMINI_API_KEY` 비밀 환경변수가 필요하다. API 키는 요청 JSON, DB, 로그 또는 Git 저장소에 넣지 않는다.

## 7. 백엔드 API 변경 범위

Python은 별도 HTTP 서버가 아니라 worker가 실행하는 모듈이다. 공개 API를 새로 만들 필요는 없으며 기존 AI 작업 생성·상태 조회·결과 조회 흐름을 확장하면 된다.

```text
영상 업로드 완료
  → 백엔드 샘플링·품질 선별
  → 이미지 폴더와 manifest 생성
  → queued
  → worker가 Python 명령 실행
  → processing
  → result.json 저장
  → completed 또는 failed
```

기존 상태 값 `queued`, `processing`, `completed`, `failed`는 유지한다. 결과 조회 API에 기존 하자 결과와 함께 `roomClassification`, `roomSegments`, `images[].room`을 추가한다.

## 8. 추가되는 결과 필드

### 8-1. 공간 분류 실행 정보

```json
{
  "roomClassification": {
    "provider": "gemini",
    "model": "gemini-2.5-flash-lite",
    "batchSize": 10,
    "imageSize": 384,
    "windowSize": 5,
    "minVotes": 3,
    "apiCalls": 2,
    "errors": []
  }
}
```

### 8-2. 이미지별 공간 정보

기존 `images[]` 항목에 다음 필드가 추가된다.

```json
{
  "imageId": "inspection-image-101",
  "filename": "000001.jpg",
  "sequenceIndex": 0,
  "timestampSec": 0.0,
  "image": {
    "width": 1920,
    "height": 1080
  },
  "room": {
    "raw": "bathroom",
    "stable": "bathroom",
    "uncertain": false,
    "provider": "gemini",
    "model": "gemini-2.5-flash-lite",
    "roomSegmentId": "room-seg-0001"
  },
  "detections": []
}
```

- `raw`: 해당 이미지의 API 직접 분류 결과
- `stable`: 앞뒤 이미지 결과를 반영한 최종 공간
- `uncertain`: API가 공간 근거가 약하다고 판단했는지 여부
- 화면·리포트·그룹화에는 `raw`가 아닌 `stable`을 사용한다.

### 8-3. 공간 구간

```json
{
  "roomSegments": [
    {
      "roomSegmentId": "room-seg-0001",
      "room": "bathroom",
      "startImageId": "inspection-image-101",
      "endImageId": "inspection-image-108",
      "startSequenceIndex": 0,
      "endSequenceIndex": 7,
      "startTimestampSec": 0.0,
      "endTimestampSec": 7.0,
      "frameCount": 8
    }
  ]
}
```

`frameCount`는 해당 공간 구간에 포함된 샘플 이미지 수를 의미한다.

### 8-4. 하자 observation 공간 연결

기존 `observations[]`에 다음 필드가 추가된다.

```json
{
  "observationGroupId": "obs-0001-001",
  "imageId": "inspection-image-101",
  "label": "mold",
  "confidence": 0.87,
  "room": "bathroom",
  "roomSegmentId": "room-seg-0001",
  "sequenceIndex": 0,
  "timestampSec": 0.0,
  "evidencePath": "output/job-001/evidence/000001.jpg",
  "cropPath": "output/job-001/crops/obs-0001-001_mold.jpg"
}
```

백엔드는 `room`과 `roomSegmentId`를 리포트의 공간별 하자 그룹에 사용한다.

## 9. 변경되지 않는 기존 계약

다음 항목은 기존 `BACKEND_MODEL_OUTPUT_CONTRACT.md` 기준을 유지한다.

- bbox는 원본 이미지 픽셀 기준 `xyxy`
- `box.left`, `box.top`, `box.right`, `box.bottom`
- `image.width`, `image.height`
- `classId`, `label`, `confidence`
- 탐지 결과가 없으면 `detections: []`
- `unknown_defect`는 `classId: null`
- `evidencePath`, `cropPath`를 백엔드가 인증 URL로 변환
- AI 결과는 하자 확정이 아니라 `needs_review` 관찰

## 10. 오류 처리

Gemini 일부 또는 전체 요청 실패:

- 해당 이미지 공간을 `unknown`으로 반환한다.
- 오류를 `roomClassification.errors[]`에 기록한다.
- YOLO 하자 분석은 계속 수행한다.
- 공간 API 오류만으로 작업 전체를 `failed`로 바꾸지 않는다.

입력·manifest·모델 실행 실패:

- 최종 `result.json`의 `status`를 `failed`로 기록한다.
- `error.type`, `error.message`를 저장한다.
- 백엔드는 작업 상태를 `failed`로 갱신한다.
- 사용자 응답에 내부 경로나 API 키를 노출하지 않는다.

## 11. 백엔드 구현 체크리스트

- [ ] 샘플링·Blur/밝기·중복 필터가 끝난 이미지만 AI 폴더에 저장
- [ ] 이미지 파일명이 시간순 오름차순이 되도록 저장
- [ ] manifest의 `filename`과 실제 파일명 일치
- [ ] manifest에 백엔드 `imageId`와 `timestampSec` 포함
- [ ] worker 실행 명령을 `process_image_batch_room_defect`로 변경
- [ ] AI worker 환경에 `GEMINI_API_KEY` 비밀변수 설정
- [ ] `roomClassification.errors` 저장
- [ ] `images[].room.stable` 저장 및 화면 안내에 사용
- [ ] `roomSegments[]` 저장 또는 JSON 보존
- [ ] `observations[].room`, `roomSegmentId`를 리포트에 연결
- [ ] evidence·crop 경로를 인증 URL로 변환
- [ ] `unknown` 공간과 `unknown_defect` 하자를 별도 값으로 처리
- [ ] AI job 상태를 `queued`, `processing`, `completed`, `failed`로 관리

## 12. 연동 완료 기준

1. 실제 샘플 이미지 묶음으로 Python 작업이 `completed` 상태로 종료된다.
2. 모든 이미지에 `room.raw`, `room.stable`, `roomSegmentId`가 존재한다.
3. 모든 하자 observation에 공간과 근거 crop이 연결된다.
4. 공간 API 실패 시 `unknown`으로 반환되면서 하자 분석은 완료된다.
5. 백엔드 결과 조회 API에서 공간 구간과 공간별 하자를 확인할 수 있다.

