# 하자 박스 좌표·한글 라벨 표시 가이드

## 1. 목적

AI가 반환한 하자 박스를 백엔드에 저장하고 안드로이드 화면에 동일한 위치로 표시하기 위한 기준이다.

현재 서비스 기준은 다음과 같다.

- 입력은 백엔드가 선별해 전달한 이미지다.
- 하자 박스는 crop 이미지가 아니라 원본 전체 이미지 위에 표시한다.
- 좌표는 원본 이미지 픽셀 기준 `xyxy`다.
- 백엔드·DB에서는 영문 `label`을 고정 식별자로 사용한다.
- 사용자 화면에는 `displayLabel` 한글명과 `displayColor`를 사용한다.

## 2. 현재 Python에서 박스를 만드는 방식

현재 구현 파일은 `training/process_images_two_stage.py`다.

처리 흐름은 다음과 같다.

```text
백엔드 전달 이미지
  ↓ OpenCV로 디코딩
원본 픽셀 크기 확인(width, height)
  ↓
YOLO 내부 letterbox resize 후 추론
  ↓ Ultralytics가 원본 이미지 좌표로 복원
result.boxes.xyxy
  ↓
Gemini 보조 검증 결과 반영
  ↓
최종 detections[] JSON + 원본 전체 이미지 박스 결과
```

YOLO 내부에서는 모델 입력 크기에 맞게 이미지가 조정되지만 `result.boxes.xyxy`는 Ultralytics가 입력 이미지 크기로 다시 변환한 좌표다. Python은 이 좌표를 별도로 정규화하지 않고 다음 형태로 반환한다.

```json
{
  "image": {
    "width": 1920,
    "height": 1080
  },
  "detections": [
    {
      "classId": 3,
      "label": "water_damage",
      "displayLabel": "누수",
      "displayColor": "#1E88E5",
      "confidence": 0.87,
      "box": {
        "left": 120.5,
        "top": 80.2,
        "right": 640.8,
        "bottom": 410.4
      }
    }
  ]
}
```

좌표 원점은 이미지 왼쪽 위 `(0, 0)`이며, `right`, `bottom`은 오른쪽 아래 좌표다.

```text
(0, 0) ──────────────────────> x
  │
  │       (left, top)
  │          ┌──────────┐
  │          │ 하자 영역 │
  │          └──────────┘ (right, bottom)
  ↓ y
```

Python이 미리 생성하는 `annotated/final` 이미지는 원본 이미지와 동일한 픽셀 캔버스에 이 좌표를 그대로 그린 결과다. 박스는 클래스별 색상, 텍스트는 `한글명 + confidence 백분율` 형식이다.

## 3. 백엔드에서 해야 할 처리

### 3-1. 반드시 저장할 값

이미지마다 다음 값을 하나의 묶음으로 저장한다.

- AI에 전달한 원본 이미지 또는 동일한 최종 저장 이미지
- `image.width`, `image.height`
- `detections[].box.left/top/right/bottom`
- `classId`, `label`, `displayLabel`, `displayColor`, `confidence`
- 최종 박스 이미지가 필요하면 `annotatedPath` 또는 백엔드가 발급한 URL

좌표는 정수로 강제 변환하지 말고 `Float` 또는 `Double`로 저장하는 것을 권장한다.

### 3-2. 백엔드에서 좌표를 바꾸지 않는 것이 기본

AI가 반환한 좌표는 원본 이미지 픽셀값이다. 백엔드는 다음 작업을 하지 않는다.

- `0~1` 범위로 임의 정규화
- 화면 크기를 가정한 좌표 변환
- 썸네일 크기 기준 좌표로 덮어쓰기
- `xywh` 또는 중심점 좌표로 변환해 원본 값을 유실

프론트엔드에 전달할 때도 AI 응답 좌표와 원본 이미지 크기를 유지한다.

### 3-3. 이미지 변형 시 주의

백엔드가 AI 처리 후 이미지를 resize, crop, 회전하면 기존 좌표와 이미지가 달라진다. 가장 안전한 방식은 AI에 전달한 것과 동일한 픽셀 방향·비율의 이미지를 프론트엔드에도 제공하는 것이다.

부득이하게 resize만 하는 경우:

```text
scaleX = 변환 이미지 width / AI 기준 image.width
scaleY = 변환 이미지 height / AI 기준 image.height

newLeft   = left   × scaleX
newTop    = top    × scaleY
newRight  = right  × scaleX
newBottom = bottom × scaleY
```

crop이나 회전은 별도 offset·회전 행렬이 필요하므로 MVP에서는 사용하지 않는 것을 권장한다.

### 3-4. EXIF 회전 방향 기준

스마트폰 JPEG는 픽셀을 실제로 회전하지 않고 EXIF Orientation만 기록할 수 있다. 안드로이드 이미지 라이브러리는 EXIF를 적용하지만 OpenCV가 읽은 방향과 다르면 박스가 90도 돌아가거나 반대 위치에 표시된다.

따라서 백엔드는 AI에 전달하기 전에 다음 규칙으로 정규화한다.

1. EXIF Orientation을 읽는다.
2. 픽셀 데이터를 실제 정방향으로 회전한다.
3. Orientation을 1로 변경하거나 메타데이터를 제거한다.
4. 정규화된 동일 이미지 파일을 AI와 안드로이드에 사용한다.
5. 정규화 이후의 width·height를 AI 결과 기준으로 저장한다.

안드로이드가 보는 이미지와 AI가 본 이미지가 동일한 방향인지 확인하는 것이 가장 중요하다.

### 3-5. 권장 API 응답

```json
{
  "imageId": "image-0001",
  "imageUrl": "https://.../images/image-0001.jpg",
  "annotatedImageUrl": "https://.../annotated/image-0001.jpg",
  "image": {
    "width": 1920,
    "height": 1080
  },
  "room": {
    "stable": "bathroom",
    "displayLabel": "욕실"
  },
  "detections": [
    {
      "classId": 3,
      "label": "water_damage",
      "displayLabel": "누수",
      "displayColor": "#1E88E5",
      "confidence": 0.87,
      "box": {
        "left": 120.5,
        "top": 80.2,
        "right": 640.8,
        "bottom": 410.4
      }
    }
  ]
}
```

`annotatedImageUrl`은 결과 확인용으로 사용할 수 있다. 박스 선택·확대·표시 숨김 같은 상호작용이 필요하면 `imageUrl + detections[]` 방식으로 안드로이드에서 직접 그린다.

## 4. 안드로이드에서 좌표를 화면에 표시하는 방법

원본 이미지 크기와 화면에 실제로 표시된 이미지 영역은 다르다. 따라서 박스 좌표에 이미지 표시 배율과 여백을 적용해야 한다.

### 4-1. `Fit` / `fitCenter` 방식

이미지 전체를 화면 안에 보이게 표시하는 경우다. 가로 또는 세로에 여백이 생길 수 있다.

```text
imageWidth  = API image.width
imageHeight = API image.height
viewWidth   = 이미지 표시 영역 width
viewHeight  = 이미지 표시 영역 height

scale = min(viewWidth / imageWidth, viewHeight / imageHeight)

drawnWidth  = imageWidth × scale
drawnHeight = imageHeight × scale
offsetX = (viewWidth - drawnWidth) / 2
offsetY = (viewHeight - drawnHeight) / 2

screenLeft   = offsetX + left   × scale
screenTop    = offsetY + top    × scale
screenRight  = offsetX + right  × scale
screenBottom = offsetY + bottom × scale
```

### 4-2. `Crop` / `centerCrop` 방식

화면을 가득 채우기 위해 이미지 일부가 잘리는 경우다.

```text
scale = max(viewWidth / imageWidth, viewHeight / imageHeight)
offsetX = (viewWidth - imageWidth × scale) / 2
offsetY = (viewHeight - imageHeight × scale) / 2
```

박스 변환 공식은 `Fit`과 같지만 `offsetX` 또는 `offsetY`가 음수가 될 수 있다. 화면 밖으로 나간 박스 부분은 Canvas clipping에 따라 잘린다.

MVP 리포트 화면은 위치 확인이 쉬운 `Fit` 방식을 권장한다.

### 4-3. Compose 좌표 변환 예시

```kotlin
data class ApiBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

fun ApiBox.toFitRect(
    imageWidth: Float,
    imageHeight: Float,
    viewWidth: Float,
    viewHeight: Float,
): Rect {
    val scale = minOf(viewWidth / imageWidth, viewHeight / imageHeight)
    val offsetX = (viewWidth - imageWidth * scale) / 2f
    val offsetY = (viewHeight - imageHeight * scale) / 2f

    return Rect(
        left = offsetX + left * scale,
        top = offsetY + top * scale,
        right = offsetX + right * scale,
        bottom = offsetY + bottom * scale,
    )
}
```

Canvas에서는 이미지와 박스를 반드시 같은 크기의 컨테이너 안에 겹쳐 그린다.

```kotlin
Box(Modifier.fillMaxWidth().aspectRatio(imageWidth / imageHeight)) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.matchParentSize(),
    )

    Canvas(Modifier.matchParentSize()) {
        detections.forEach { detection ->
            val rect = detection.box.toFitRect(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = size.width,
                viewHeight = size.height,
            )
            drawRect(
                color = Color(android.graphics.Color.parseColor(detection.displayColor)),
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}
```

컨테이너에 padding이 있다면 `viewWidth`, `viewHeight`는 padding을 제외한 실제 이미지 영역으로 계산하고, 최종 좌표에 컨테이너의 화면상 시작 위치를 더한다.

### 4-4. 안드로이드 검증 체크

- API의 `image.width/height`와 다운로드한 이미지 픽셀 크기가 같은가?
- 이미지와 Canvas가 동일한 레이아웃 크기를 사용하는가?
- `ContentScale.Fit`인데 `Crop` 계산식을 사용하지 않았는가?
- 상태바·툴바·padding 좌표를 이미지 내부 좌표와 섞지 않았는가?
- EXIF 회전이 AI 입력과 동일하게 적용됐는가?
- 박스 계산에 `Int` 나눗셈이 아니라 `Float`를 사용했는가?

## 5. 한글 라벨과 클래스별 색상

### 5-1. 필드 사용 원칙

| 필드 | 사용 위치 |
|---|---|
| `classId` | 내부 숫자 식별, 통계·DB |
| `label` | 백엔드 분기·로그·호환성 유지 |
| `displayLabel` | 사용자에게 표시할 한글명 |
| `displayColor` | 박스·라벨 배경색 |

안드로이드에서 `label == "water_damage"`를 다시 한글로 변환할 필요는 없다. AI가 반환한 `displayLabel`을 우선 사용한다. 구버전 응답에 `displayLabel`이 없을 때만 앱 내부 fallback 표를 사용한다.

### 5-2. 현재 표시명과 색상

| label | displayLabel | displayColor |
|---|---|---|
| `crack` | 균열 | `#E53935` |
| `mold` | 곰팡이 | `#2E7D32` |
| `peeling` | 들뜸·박리 | `#FB8C00` |
| `water_damage` | 누수 | `#1E88E5` |
| `tile_damage` | 타일 손상 | `#8E24AA` |
| `hole` | 구멍 | `#6D4C41` |
| `tile_crack` | 타일 균열 | `#D81B60` |
| `paint_drips` | 페인트 흘러내림 | `#5E35B1` |
| `pin_hole` | 미세 구멍 | `#00897B` |
| `surface_defect` | 표면 하자 | `#F9A825` |
| `stain` | 오염 | `#558B2F` |
| `trowel_mark` | 마감 자국 | `#546E7A` |
| `other` | 하자 의심 | `#F4511E` |

### 5-3. 텍스트 표시 권장 형식

```text
누수 87%
곰팡이 64%
하자 의심 41%
```

- 사용자 화면에는 후보 ID `D1`, `D2`를 표시하지 않는다.
- confidence는 `0.87 → 87%`처럼 표시한다.
- `other`는 특정 하자로 확정하지 않고 `하자 의심`으로 표시한다.
- 한글 라벨은 계약 또는 하자 확정이 아니라 AI 관찰 결과라는 안내 문구와 함께 사용한다.

Python이 생성한 박스 이미지에는 서버 한글 폰트가 필요하지만, 안드로이드가 JSON으로 직접 렌더링할 때는 안드로이드 시스템 한글 폰트를 사용하므로 별도 폰트 파일이 필요하지 않다.

## 6. 역할 분리

| 담당 | 해야 할 일 |
|---|---|
| AI Python | 원본 픽셀 기준 `xyxy`, 이미지 크기, 한글 표시명·색상, 최종 탐지 결과 반환 |
| 백엔드 | AI 입력과 동일한 이미지 보관, 좌표·이미지 크기 저장, URL 발급, EXIF 방향 정규화 |
| 안드로이드 | 실제 이미지 표시 배율·여백 계산, 좌표를 Canvas 좌표로 변환, 한글 라벨·색상 표시 |

## 7. 통합 테스트 기준

좌표 연동 완료 조건은 다음과 같다.

1. 테스트 이미지 네 모서리에 가까운 박스가 모두 정확히 표시된다.
2. 세로·가로 이미지에서 박스가 동일한 위치에 표시된다.
3. `Fit` 표시 시 상하·좌우 여백이 생겨도 박스가 밀리지 않는다.
4. 회전 정보가 있는 스마트폰 JPEG에서도 위치가 맞는다.
5. 확대 전·후에도 박스와 이미지가 함께 이동한다.
6. `누수`, `오염`, `곰팡이` 등 한글명과 클래스별 색상이 API 응답과 일치한다.
