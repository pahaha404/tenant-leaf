# 백엔드 모델 출력 계약

## 목적

YOLO 객체탐지 모델의 결과를 백엔드와 프론트엔드에서 동일하게 해석하기 위한 API 출력 형식을 정의한다.

## 1. 좌표 기준

탐지 영역은 `xyxy` 형식을 사용한다.

```text
[left, top, right, bottom]
```

- `left`, `top`: bounding box의 왼쪽 위 좌표
- `right`, `bottom`: bounding box의 오른쪽 아래 좌표
- 좌표 원점 `(0, 0)`: 이미지의 왼쪽 위
- 중심점 기준 좌표는 사용하지 않는다.

현재 추론 코드인 `inference/predict_yolo.py`도 `xyxy` 형식으로 결과를 생성한다.

## 2. 좌표 단위

좌표는 원본 이미지 기준 픽셀값으로 반환한다.

예를 들어 원본 이미지가 `1920 x 1080`이면 다음 범위를 사용한다.

```text
0 <= left < right <= 1920
0 <= top < bottom <= 1080
```

YOLO 내부에서 이미지가 resize되더라도 백엔드 응답은 resize된 이미지 기준이 아니라 원본 이미지 기준이어야 한다. 그래야 프론트엔드가 별도 변환 없이 bounding box를 그릴 수 있다.

## 3. 이미지 크기

응답에 모델이 처리한 이미지의 실제 `width`, `height`를 포함한다.

```json
{
  "image": {
    "width": 1920,
    "height": 1080
  }
}
```

`width`, `height`는 원본 이미지의 픽셀 크기이며, bounding box 좌표의 기준이 된다.

## 4. classId와 label

`classId`와 `label`을 모두 반환한다.

| 필드 | 용도 |
|---|---|
| `classId` | 모델·백엔드 간 고정된 숫자 식별자 |
| `label` | 화면 표시, 로그, 사용자에게 보여줄 클래스 이름 |

`classId`는 내부 처리와 데이터 저장에 사용하고, `label`은 표시용으로 사용한다. label 이름만으로 처리하면 추후 이름 변경이나 다국어 지원 시 호환성 문제가 생길 수 있다.

현재 주요 class ID는 다음과 같다.

| classId | label | 상태 |
|---:|---|---|
| 0 | `crack` | 활성 |
| 1 | `mold` | 활성 |
| 2 | `peeling` | 활성 |
| 3 | `water_damage` | 활성 |
| 4 | `tile_damage` | 활성 |
| 5 | `hole` | 활성 |
| 6 | `tile_crack` | 활성 |
| 7 | `paint_drips` | 비활성 |
| 8 | `pin_hole` | 활성 |
| 9 | `surface_defect` | 활성 |
| 10 | `stain` | 활성 |
| 11 | `trowel_mark` | 활성 |

### 현재 모델 개선 우선순위

API class ID와 호환성은 변경하지 않는다. 다만 현재 모델 측정·개선·승격 판단은 아래 6종을 우선 대상으로 한다.

- `hole`, `mold`, `peeling`, `stain`, `surface_defect`, `water_damage`

`crack`, `tile_damage`, `tile_crack`, `pin_hole`, `trowel_mark`는 API ID와 데이터는 유지하지만 우선 6종이 안정될 때까지 개선 KPI에서 보류한다. `paint_drips`는 기존과 같이 비활성 상태다. 이는 API에서 클래스를 삭제하거나 ID를 재배정하는 변경이 아니다.

비활성 class인 `paint_drips`는 ID 체계의 호환성을 위해 ID 7을 유지하지만, 현재 API 탐지 결과에는 반환하지 않는다.

## 5. 여러 탐지 결과

한 이미지에서 여러 하자가 탐지될 수 있으므로 `detections` JSON 배열로 반환한다.

- confidence 내림차순으로 정렬한다.
- 탐지 결과가 없으면 빈 배열 `[]`을 반환한다.
- 각 탐지는 하나의 bounding box와 하나의 class를 갖는다.

## 권장 응답 형식

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
    },
    {
      "classId": 3,
      "label": "water_damage",
      "confidence": 0.64,
      "box": {
        "left": 720,
        "top": 300,
        "right": 1200,
        "bottom": 760
      }
    }
  ]
}
```

## 탐지 결과가 없는 경우

```json
{
  "image": {
    "width": 1920,
    "height": 1080
  },
  "detections": []
}
```

## 6. 분류되지 않은 하자 후보

Binary detector가 하자 후보를 찾았지만 후단 classifier가 하자 유형을 확정하지 못한 경우, `unknown_defect`를 학습용 class ID로 취급하지 않는다.

- `classId`: `null`
- `label`: `unknown_defect`
- `classificationStatus`: `unclassified`
- `reviewStatus`: `needs_review`
- `defectConfidence`: binary detector의 하자 존재 confidence
- `classConfidence`: classifier confidence. 분류 실패 시 `null`

`unknown_defect`는 “하자가 아니다”라는 뜻이 아니라 “하자 후보는 있으나 종류를 아직 모른다”는 뜻이다. 백엔드는 이 결과를 삭제하거나 정상 이미지로 처리하지 않고, 사용자 확인 필요 관찰로 저장해야 한다.

`classificationStatus`와 `reviewStatus`는 분리한다.

| 필드 | 예시 값 | 의미 |
|---|---|---|
| `classificationStatus` | `classified` / `unclassified` | 하자 유형 분류가 끝났는지 여부 |
| `reviewStatus` | `needs_review` / `confirmed` / `dismissed` | 사용자가 확인한 상태 |

예시:

```json
{
  "classId": null,
  "label": "unknown_defect",
  "classificationStatus": "unclassified",
  "reviewStatus": "needs_review",
  "defectConfidence": 0.78,
  "classConfidence": null,
  "box": {
    "left": 120,
    "top": 80,
    "right": 640,
    "bottom": 410
  }
}
```

분류가 성공한 경우에는 기존처럼 고정된 `classId`와 `label`을 사용한다.

```json
{
  "classId": 1,
  "label": "mold",
  "classificationStatus": "classified",
  "reviewStatus": "needs_review",
  "defectConfidence": 0.78,
  "classConfidence": 0.86
}
```

## 백엔드 구현 시 확인할 사항

- [ ] 좌표 순서가 `[left, top, right, bottom]`인지 확인
- [ ] 좌표가 원본 이미지 픽셀 기준인지 확인
- [ ] 응답에 실제 이미지 `width`, `height` 포함
- [ ] `classId`와 `label`을 모두 포함
- [ ] 여러 결과를 `detections` 배열로 반환
- [ ] 탐지 결과가 없을 때 빈 배열 반환
- [ ] confidence 내림차순 정렬
- [ ] 좌표가 이미지 경계를 벗어나지 않도록 보정
- [ ] `paint_drips` 등 비활성 class가 API에 노출되지 않는지 확인
- [ ] `unknown_defect`를 고정 class ID로 저장하지 않고 `classId: null`로 처리
- [ ] binary 하자 존재 confidence와 후단 class confidence를 별도 저장
- [ ] 분류 실패 후보도 `needs_review` 관찰로 보존

## 현재 구현과의 차이

현재 `inference/predict_yolo.py`는 `xyxy`, confidence, class ID, class name을 반환한다. 다음 구현 작업에서 아래 항목을 schema에 맞게 보완한다.

- `image.width`, `image.height` 추가
- `class_id`를 API 표준인 `classId`로 변경
- `class_name`을 API 표준인 `label`로 변경
- 배열 내부의 `xyxy`를 `box.left/top/right/bottom` 구조로 변경

`training/process_video_two_stage.py`는 위 변환을 적용한 `frame_results[]`를 생성한다. 기존 `inference/predict_yolo.py`는 단일 모델 실험용이므로 서비스 영상 흐름에서는 사용하지 않는다.

## 영상 관찰 결과의 리포트 이미지

영상 파이프라인은 여러 프레임에서 같은 하자를 하나의 `observations[]` 항목으로 묶는다. 각 observation에는 리포트 연결을 위한 다음 이미지 경로가 포함된다.

- `evidencePath`: 하자가 탐지된 대표 원본 프레임
- `cropPath`: bbox에 15% 여백을 적용한 하자 영역 crop
- `cropImage.width`, `cropImage.height`: 생성된 crop 이미지 크기

AI가 반환하는 값은 서버 내부 파일 경로다. 백엔드는 파일을 저장소에 보관하고 인증된 `evidenceUrl`, `cropUrl`로 변환해 리포트에 연결한다. 리포트 초안·확정본·목록·상세·공유 기능은 백엔드 책임이다.
