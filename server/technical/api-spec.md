# 세입세잎 공통 API 계약

> 상태: 확정 1.2
>
> 기준일: 2026-08-14
>
> 기준 용어: [`../backendmds/공통용어집.md`](../backendmds/공통용어집.md)

Android 앱, Kotlin API 서버, AI 작업자가 같은 이름과 데이터 형식을 사용하기 위한 MVP 계약이다. 이 문서의 확정 항목을 구현 기준으로 사용하며, 아직 확정되지 않은 항목은 임의로 채우지 않는다.

## 1. 반드시 지키는 흐름

```text
스마트 글래스 기본 영상 촬영 -> 휴대전화 갤러리
                                  -> Android JPEG 생성 -> API 서버 -> AI 작업자
                                                         -> 파일 저장소
```

- 스마트 글래스는 API 서버나 AI 제공자를 직접 호출하지 않는다.
- Android 앱은 AI 제공자 API 키를 저장하거나 AI 제공자를 직접 호출하지 않는다.
- 원본 영상은 사용자의 휴대전화 갤러리에만 보관하며 API 서버와 파일 저장소에 업로드하지 않는다.
- Android가 촬영 중 생성하거나 촬영 완료 영상에서 추출한 분석용 JPEG만 접근 제어된 파일 저장소에 보관한다.
- MVP에서는 촬영 중 실시간 스트리밍 분석을 실행하지 않는다.
- AI `detection`은 하자 후보인 **확인 필요 관찰 결과**이며 실제 하자, 위험 또는 안전 상태를 확정하지 않는다.
- 체크리스트의 최종 상태는 사용자가 Android 앱에서 확인한 값만 반영한다.

## 2. 공통 규칙

| 항목 | 규칙 |
|---|---|
| API 기본 경로 | `/api/v1` |
| 본문 형식 | `application/json` |
| JSON 필드명 | `camelCase` |
| ID | UUID 문자열 |
| 날짜와 시간 | ISO 8601 UTC 문자열. 예: `2026-08-12T07:30:00Z` |
| 날짜 | ISO 8601 날짜. 예: `2026-08-12` |
| 선택값 | 값이 없으면 필드를 생략한다. 값을 지우는 요청에만 명시적으로 `null`을 사용한다. |
| 페이지 조회 | `page`, `size`를 사용하며 `page`는 0부터 시작한다. |
| 언어 | 코드와 API는 영문 표준명, 사용자 화면은 한글 표준명을 사용한다. |

### 2.1 인증

MVP에서는 실제 소셜 로그인을 연동하지 않고 데모 계정으로 게스트 로그인한다. UI의 소셜 로그인 버튼을 누르면 데모 로그인 안내 후 동일한 게스트 로그인 API를 호출한다. 해당 버튼을 실제 소셜 계정 인증이나 연동 완료로 표현하지 않는다.

데모 로그인 성공 후 모든 사용자용 API에 아래 형식을 적용한다.

```http
Authorization: Bearer <access-token>
```

서버는 데모 사용자를 식별할 수 있는 접근 토큰을 발급한다. 실제 소셜 인증, 토큰 재발급 및 장기 로그인 유지는 MVP 범위에서 제외한다.

### 2.2 성공 응답

성공 응답은 불필요한 공통 포장 객체 없이 리소스 또는 페이지 객체를 그대로 반환한다.

```json
{
  "id": "b5df53ee-d948-4c16-879f-8a4ec0ca64cd",
  "name": "역세권 원룸"
}
```

목록 응답은 다음 형식을 사용한다.

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### 2.3 오류 응답

```json
{
  "code": "VALIDATION_ERROR",
  "message": "요청값을 확인해 주세요.",
  "fieldErrors": [
    {
      "field": "name",
      "reason": "mustNotBeBlank"
    }
  ],
  "traceId": "7ccf5b3f44d94fa0"
}
```

| HTTP 상태 | `code` 예시 | 의미 |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | 요청값 형식 또는 범위 오류 |
| `401` | `UNAUTHENTICATED` | 로그인이 필요함 |
| `403` | `FORBIDDEN` | 다른 사용자의 리소스 등 접근 권한 없음 |
| `404` | `PROPERTY_NOT_FOUND` | 요청한 리소스가 없음 |
| `409` | `INVALID_STATE_TRANSITION` | 현재 상태에서는 요청을 수행할 수 없음 |
| `413` | `FILE_TOO_LARGE` | 업로드 허용 크기 초과 |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 파일 형식 |
| `500` | `INTERNAL_ERROR` | 서버 내부 오류 |

클라이언트는 사용자 문구인 `message`가 아니라 안정된 식별자인 `code`로 분기한다.

## 3. 공통 상태값

### 3.1 임장 상태 `InspectionStatus`

| 값 | 의미 |
|---|---|
| `in_progress` | 사용자가 임장을 진행 중 |
| `completed` | 사용자가 임장을 종료함 |
| `cancelled` | 사용자가 임장을 취소함 |

허용 전이는 `in_progress -> completed` 또는 `in_progress -> cancelled`이다. 같은 매물을 다시 점검하면 기존 임장을 되돌리지 않고 새 `inspection`을 만든다.

### 3.2 체크리스트 상태 `ChecklistStatus`

| 값 | 의미 |
|---|---|
| `unchecked` | 아직 사용자가 확인하지 않음 |
| `normal` | 사용자가 특이사항 없음을 확인함 |
| `needs_review` | 사용자가 추가 확인이 필요하다고 표시함 |
| `not_applicable` | 해당 매물에는 적용되지 않는 항목 |

AI 결과만으로 체크리스트 상태를 변경하지 않는다. 모든 변경에는 `confirmedByUser: true`가 필요하다.

### 3.3 업로드 상태 `FrameUploadStatus`

| 값 | 의미 |
|---|---|
| `pending` | 업로드 URL을 발급했으나 완료 확인 전 |
| `uploaded` | 파일 저장소 업로드 완료를 확인함 |
| `failed` | 업로드가 실패함 |
| `deleted` | 보관 정책 또는 사용자 요청에 따라 삭제됨 |

### 3.4 AI 분석 상태 `AnalysisStatus`

| 값 | 의미 |
|---|---|
| `queued` | 분석 요청이 대기열에 등록됨 |
| `processing` | AI 작업자가 분석 중 |
| `completed` | 탐지 결과 저장 완료 |
| `failed` | 재시도 후에도 분석 실패 |

### 3.5 AI 탐지 검토 상태 `DetectionReviewStatus`

| 값 | 의미 |
|---|---|
| `needs_review` | 사용자가 아직 확인하지 않음 |
| `accepted` | 사용자가 해당 후보를 관찰 결과로 인정함 |
| `rejected` | 사용자가 오탐 등으로 반려함 |

`accepted`도 안전 진단이나 법적 하자 확정이 아니라 사용자가 현장에서 확인한 관찰 기록이라는 뜻이다.

## 4. 공통 데이터 형식

### 4.1 사용자 `User`

```json
{
  "id": "2fb2b214-523f-4fcf-bce7-72881a5ccb32",
  "displayName": "잎새",
  "createdAt": "2026-08-12T07:30:00Z"
}
```

계정 인증 정보와 개인정보 필드는 인증 정책이 확정된 뒤 별도 계약으로 추가한다.

MVP의 `User`는 데모 사용자이며 실제 소셜 계정 정보와 연결하지 않는다.

### 4.2 매물 `Property`

```json
{
  "id": "b5df53ee-d948-4c16-879f-8a4ec0ca64cd",
  "name": "역세권 원룸",
  "addressSummary": "서울시 ○○구",
  "depositAmount": 10000000,
  "monthlyRentAmount": 650000,
  "maintenanceFeeAmount": 70000,
  "areaSquareMeters": 18.5,
  "floor": "2층",
  "options": ["에어컨", "냉장고", "세탁기"],
  "brokerContact": "○○부동산 02-0000-0000",
  "note": "채광 확인 필요",
  "createdAt": "2026-08-12T07:30:00Z",
  "updatedAt": "2026-08-12T07:30:00Z"
}
```

- `addressSummary`에는 비교에 필요한 최소 지역 정보만 저장한다.
- 금액 필드는 모두 원(KRW) 단위의 0 이상 정수로 저장한다.
- `areaSquareMeters`는 ㎡ 단위의 0보다 큰 숫자로 저장한다.
- 앱은 면적 표시를 `㎡`와 `평` 사이에서 전환할 수 있다. `1평 = 3.305785㎡`를 사용하며, `평 = ㎡ / 3.305785`, `㎡ = 평 × 3.305785`로 계산한다.
- 사용자가 평 단위로 입력해도 앱이 ㎡로 변환하여 `areaSquareMeters`에 전송한다. 서버와 데이터베이스에는 평 값을 별도 저장하지 않는다.
- 계산에는 반올림하지 않은 값을 사용하고 화면에 표시할 때만 소수점 둘째 자리까지 반올림한다. 선택한 표시 단위는 앱의 로컬 설정으로 관리한다.
- `floor`, `options`, `brokerContact`는 사용자가 확인한 표현을 그대로 기록하며 서버가 사실 여부를 보증하지 않는다.
- `brokerContact`에는 매물 확인에 필요한 최소 연락 정보만 입력하고 사용자 본인이나 제3자의 불필요한 개인정보를 기록하지 않는다.
- 상세 주소가 필요해지면 접근 권한, 암호화, 마스킹, 삭제 기한을 먼저 정한다.

### 4.3 임장 `Inspection`

```json
{
  "id": "44d25c46-8391-43cf-903b-b5b580023861",
  "propertyId": "b5df53ee-d948-4c16-879f-8a4ec0ca64cd",
  "status": "in_progress",
  "startedAt": "2026-08-12T08:00:00Z",
  "completedAt": null,
  "createdAt": "2026-08-12T08:00:00Z"
}
```

### 4.4 체크리스트 항목 `ChecklistItem`

```json
{
  "id": "31157362-8faa-4f90-af64-68b563ee9e52",
  "inspectionId": "44d25c46-8391-43cf-903b-b5b580023861",
  "key": "bathroom.wall.surface",
  "title": "욕실 벽면 상태",
  "status": "needs_review",
  "note": "창가 쪽 얼룩 확인",
  "confirmedByUser": true,
  "updatedAt": "2026-08-12T08:10:00Z"
}
```

`key`와 `title`의 기준 데이터는 체크리스트 설정 담당자가 별도 계약으로 관리한다.

### 4.5 기기 `Device`

```json
{
  "id": "7df76d24-9669-45c7-aec5-86426153ec55",
  "type": "smartphone",
  "modelName": "Android device"
}
```

`type`의 MVP 허용값은 `smartphone`, `smart_glasses`다. 서버에는 기기 식별에 꼭 필요한 최소 정보만 보낸다.

### 4.6 분석 프레임 `Frame`

```json
{
  "id": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
  "inspectionId": "44d25c46-8391-43cf-903b-b5b580023861",
  "checklistItemId": "31157362-8faa-4f90-af64-68b563ee9e52",
  "deviceId": "7df76d24-9669-45c7-aec5-86426153ec55",
  "frameOrigin": "post_recording_extraction",
  "sourceVideoId": "db515c1d-a0d0-4f43-85bf-1a66ca164c82",
  "sourceVideoOffsetMs": 252000,
  "contentType": "image/jpeg",
  "width": 720,
  "height": 1280,
  "contentLength": 286412,
  "importantByUser": false,
  "capturedAt": "2026-08-12T08:09:30Z",
  "uploadStatus": "uploaded",
  "createdAt": "2026-08-12T08:09:35Z"
}
```

- API는 저장소의 실제 경로나 비공개 원본 URL을 영구 공개하지 않는다.
- 화면 표시가 필요하면 만료 시간이 짧은 조회 URL을 별도로 발급한다.
- 실제 이미지, 영상, 음성은 Git에 올리지 않는다.
- `sourceVideoId`는 Android가 영상 클립마다 생성한 UUID이며 서버에 저장된 영상 파일 ID가 아니다.
- 휴대전화 갤러리 URI와 로컬 파일 경로는 API에 보내지 않는다.
- `sourceVideoOffsetMs`는 프레임이 원본 영상 시작 후 몇 밀리초 지점인지 나타낸다.
- `capturedAt`은 촬영 중 동시 생성 사진이면 실제 캡처 시각, 사후 추출 사진이면 원본 영상 시작 시각과 `sourceVideoOffsetMs`로 계산한 시각이다.

#### 자동 분석 프레임 기준

| 항목 | 확정값 |
|---|---|
| 원본 | Meta 안경 기본 고화질 영상 촬영본 |
| 우선 생성 방식 | 기본 영상 녹화를 유지하면서 촬영 중 정지 사진 동시 생성. 공식 SDK·실기기 지원 시에만 사용 |
| 보장되는 대체 방식 | 촬영 완료 후 Android에서 영상 프레임 추출 |
| 분석 프레임 선택 | 원본 영상의 2~3초 구간마다 최대 1장 |
| 해상도 | 선택한 원본 영상 프레임의 실제 해상도. 고정 스트림 해상도를 계약값으로 사용하지 않음 |
| 저장 형식 | JPEG |
| 앱 JPEG 품질 | `90` |
| 예상 파일 크기 | 일반적으로 150~400KB. 강제 목표값은 아님 |
| 서버 최대 허용 크기 | 프레임당 `1,048,576 bytes`(1MiB) |

- 원본 영상은 Meta AI 동반 앱의 가져오기 기능을 통해 휴대전화 갤러리에 저장하며 서버에 업로드하지 않는다.
- 촬영 중 정지 사진 동시 생성은 기본 영상 녹화 중단·화질 저하가 없는지 실기기에서 검증하기 전까지 보장 기능으로 표시하지 않는다.
- 동시 생성이 지원되지 않거나 불안정하면 촬영 완료 후 추출 방식을 자동으로 사용한다.
- 앱은 각 2~3초 구간 주변 프레임 중 심하게 흔들리거나 어둡거나 가려졌거나 직전 사진과 사실상 동일한 프레임을 가능한 범위에서 제외한다.
- 앱은 실제 `width`, `height`, `contentLength`를 전송한다.
- 1MiB를 초과하면 픽셀 크기를 줄이기 전에 JPEG 품질을 조정하며 세부 흔적 손실 여부를 실기기로 검증한다.
- 서버는 JPEG 품질 숫자를 직접 검증하지 않고 `contentType`, 실제 파일 크기와 이미지 크기를 검증한다.
- 1MiB를 초과하면 `413 FILE_TOO_LARGE`, JPEG가 아니면 `415 UNSUPPORTED_MEDIA_TYPE`을 반환한다.

#### 프레임 보관 및 삭제 기준

| 사진 종류 | 보관 기간 |
|---|---:|
| 업로드 실패·미완료 파일 | 생성 후 최대 24시간 |
| 탐지 결과가 없는 자동 프레임 | 분석 완료 후 최대 7일 |
| AI 탐지가 있는 근거 프레임 | 임장 종료 후 최대 30일 |
| 사용자가 중요 표시한 사진 | 임장 종료 후 최대 30일 |
| 사용자가 삭제한 임장의 사진 | 삭제 요청 즉시 사용을 중단하고 최대 7일 이내 완전 삭제 |
| AI 탐지 JSON·사용자 확인 결과 | 사진과 분리하여 MVP 기간 동안만 보관 |

- 사용자에게 촬영 목적과 최대 30일 보관 정책을 촬영 전에 알린다.
- 30일은 MVP 운영 정책이며 법률이 일률적으로 정한 기간이 아니다. 처리 목적을 먼저 달성하면 [개인정보 보호법 제21조](https://www.law.go.kr/LSW/lsLawLinkInfo.do?chrClsCd=010202&lsJoLnkSeq=900078981)에 따라 지체 없이 파기한다.
- 보관기간이 지나거나 처리 목적이 먼저 달성되면 원본, 파생 이미지와 썸네일을 복구할 수 없도록 삭제한다.
- AI 탐지 JSON과 사용자 확인 결과도 MVP 종료 시 재검토하며, 별도 보관 근거가 없으면 삭제한다.
- 저장소 수명 주기와 정기 삭제 작업으로 위 기한을 집행하고 삭제 실패를 기록한다.
- 휴대전화 갤러리의 원본 영상 보관·삭제는 사용자가 관리한다. 원본 영상이 삭제되어도 서버의 근거 사진은 위 정책에 따라 별도로 삭제된다.

### 4.7 바운딩 박스 `Bbox`

```json
{
  "x": 0.125,
  "y": 0.2,
  "width": 0.3,
  "height": 0.25
}
```

- 좌표 원점은 이미지의 왼쪽 위다.
- 네 값은 원본 이미지 크기에 대한 `0.0` 이상 `1.0` 이하의 정규화 좌표다.
- `x + width <= 1.0`, `y + height <= 1.0`이어야 한다.
- 화면은 `Frame.width`, `Frame.height`를 이용해 픽셀 좌표로 변환한다.

### 4.8 AI 탐지 `Detection`

```json
{
  "id": "0ae532a0-7388-4c1c-a35e-5110f7833adc",
  "analysisId": "e0942c43-a149-4f8c-b8eb-ac1ec01c34b7",
  "frameId": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
  "classId": 1,
  "label": "mold",
  "confidence": 0.87,
  "bbox": {
    "x": 0.125,
    "y": 0.2,
    "width": 0.3,
    "height": 0.25
  },
  "reviewStatus": "needs_review",
  "reviewedAt": null
}
```

- `confidence`는 `0.0` 이상 `1.0` 이하이며 위험도나 안전 점수가 아니다.
- `classId`와 `label`은 아래 고정 쌍과 일치해야 한다.
- 서버는 일치하지 않는 AI 결과를 `400 INVALID_DETECTION_LABEL`로 거절한다.
- `riskLevel`은 산정 기준이 확정되기 전까지 탐지 응답에 포함하지 않는다.
- 모델 내부의 `bbox`가 중심점 또는 픽셀 좌표여도 AI 작업자가 API 계약의 왼쪽 위 기준 `0.0`~`1.0` 정규화 좌표로 변환한다.

| `classId` | `label` | 한글 표준명 |
|---:|---|---|
| `0` | `crack` | 균열 |
| `1` | `mold` | 곰팡이 |
| `2` | `peeling` | 도장·벽지 박리 |
| `3` | `waterdamage` | 누수·침수 흔적 |
| `4` | `tiledamage` | 타일 파손 |
| `5` | `hole` | 타공·구멍 |
| `6` | `tilecrack` | 타일 균열 |
| `7` | `paintdrips` | 페인트 흘러내림 |
| `8` | `pinhole` | 핀홀 |
| `9` | `surfacedefect` | 면불량 |
| `10` | `stain` | 오염·얼룩 |
| `11` | `trowelmark` | 흙손 자국 |

### 4.9 AI 분석 `Analysis`

```json
{
  "id": "e0942c43-a149-4f8c-b8eb-ac1ec01c34b7",
  "frameId": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
  "status": "completed",
  "modelVersion": "defect-detector-2026-08",
  "detections": [],
  "failureCode": null,
  "createdAt": "2026-08-12T08:09:40Z",
  "completedAt": "2026-08-12T08:09:44Z"
}
```

모델 결과에는 `modelVersion`, 근거 `frameId`, `confidence`, 사용자 검토 상태를 남긴다.

## 5. Android 앱용 API

### 5.1 데모 로그인

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/demo` | MVP용 게스트 로그인과 접근 토큰 발급 |

요청 본문은 없다. 소셜 로그인 버튼의 종류를 서버에 보내거나 소셜 계정으로 가장하지 않는다.

응답:

```json
{
  "accessToken": "<issued-demo-access-token>",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-12T18:00:00Z",
  "user": {
    "id": "2fb2b214-523f-4fcf-bce7-72881a5ccb32",
    "displayName": "데모 사용자",
    "createdAt": "2026-08-12T07:30:00Z"
  }
}
```

### 5.2 매물

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/properties` | 매물 생성 |
| `GET` | `/api/v1/properties` | 내 매물 목록 조회 |
| `GET` | `/api/v1/properties/{propertyId}` | 매물 상세 조회 |
| `PATCH` | `/api/v1/properties/{propertyId}` | 사용자가 입력한 매물 정보 수정 |
| `DELETE` | `/api/v1/properties/{propertyId}` | 매물 삭제 요청 |

매물 생성 요청:

```json
{
  "name": "역세권 원룸",
  "addressSummary": "서울시 ○○구",
  "depositAmount": 10000000,
  "monthlyRentAmount": 650000,
  "maintenanceFeeAmount": 70000,
  "areaSquareMeters": 18.5,
  "floor": "2층",
  "options": ["에어컨", "냉장고", "세탁기"],
  "brokerContact": "○○부동산 02-0000-0000",
  "note": "채광 확인 필요"
}
```

### 5.3 임장

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/properties/{propertyId}/inspections` | 새 임장 시작 |
| `GET` | `/api/v1/properties/{propertyId}/inspections` | 매물의 임장 목록 조회 |
| `GET` | `/api/v1/inspections/{inspectionId}` | 임장 상세 조회 |
| `PATCH` | `/api/v1/inspections/{inspectionId}/status` | 임장 완료 또는 취소 |
| `DELETE` | `/api/v1/inspections/{inspectionId}` | 임장과 연결된 사진 삭제 요청 |

상태 변경 요청:

```json
{
  "status": "completed"
}
```

### 5.4 체크리스트

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/v1/inspections/{inspectionId}/checklist` | 임장 체크리스트 조회 |
| `PATCH` | `/api/v1/inspections/{inspectionId}/checklist/{itemId}` | 사용자 확인 상태와 메모 저장 |

체크리스트 수정 요청:

```json
{
  "status": "needs_review",
  "note": "창가 쪽 얼룩 확인",
  "confirmedByUser": true
}
```

`confirmedByUser`가 `false`이거나 없으면 서버는 `400 USER_CONFIRMATION_REQUIRED`를 반환한다.

### 5.5 분석 프레임 업로드

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/inspections/{inspectionId}/frames/upload-requests` | 촬영 종료 후 분석 프레임 메타데이터 등록 및 업로드 URL 발급 |
| `POST` | `/api/v1/frames/{frameId}/upload-complete` | 저장소 업로드 완료 알림 |
| `GET` | `/api/v1/frames/{frameId}` | 프레임 메타데이터 조회 |
| `PATCH` | `/api/v1/frames/{frameId}` | 사용자 중요 표시 변경 |
| `POST` | `/api/v1/frames/{frameId}/view-url` | 만료 시간이 짧은 조회 URL 발급 |

업로드 URL 발급 요청:

```json
{
  "checklistItemId": "31157362-8faa-4f90-af64-68b563ee9e52",
  "deviceId": "7df76d24-9669-45c7-aec5-86426153ec55",
  "frameOrigin": "post_recording_extraction",
  "sourceVideoId": "db515c1d-a0d0-4f43-85bf-1a66ca164c82",
  "sourceVideoOffsetMs": 252000,
  "fileName": "frame-20260812-080930.jpg",
  "contentType": "image/jpeg",
  "contentLength": 286412,
  "width": 720,
  "height": 1280,
  "importantByUser": false,
  "capturedAt": "2026-08-12T08:09:30Z"
}
```

응답:

```json
{
  "frame": {
    "id": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
    "inspectionId": "44d25c46-8391-43cf-903b-b5b580023861",
    "deviceId": "7df76d24-9669-45c7-aec5-86426153ec55",
    "frameOrigin": "post_recording_extraction",
    "sourceVideoId": "db515c1d-a0d0-4f43-85bf-1a66ca164c82",
    "sourceVideoOffsetMs": 252000,
    "contentType": "image/jpeg",
    "width": 720,
    "height": 1280,
    "contentLength": 286412,
    "importantByUser": false,
    "capturedAt": "2026-08-12T08:09:30Z",
    "uploadStatus": "pending",
    "createdAt": "2026-08-12T08:09:35Z"
  },
  "upload": {
    "method": "PUT",
    "url": "https://storage.example.invalid/signed-upload",
    "headers": {
      "Content-Type": "image/jpeg"
    },
    "expiresAt": "2026-08-12T08:19:35Z"
  }
}
```

앱은 현장 영상 촬영을 종료한 뒤 발급받은 URL에 JPEG를 올리고 `upload-complete`를 호출한다. 계약 1.2에서는 임장 상태가 `completed`일 때만 업로드 요청과 분석 요청을 허용하며, 이는 최신 도메인 규칙의 `ENDED`에 임시 대응한다. 촬영 중인 `in_progress` 임장에는 `409 Conflict`를 반환한다. 서버는 실제 저장소 객체의 존재와 크기를 확인한 후에만 상태를 `uploaded`로 바꾼다. 원본 영상 파일, 갤러리 URI와 로컬 경로를 받는 API는 MVP에 만들지 않는다.

중요 표시 변경 요청:

```json
{
  "importantByUser": true
}
```

### 5.6 AI 분석과 사용자 검토

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/frames/{frameId}/analyses` | 업로드된 프레임 분석 요청 |
| `GET` | `/api/v1/analyses/{analysisId}` | 분석 상태와 탐지 목록 조회 |
| `PATCH` | `/api/v1/detections/{detectionId}/review` | 사용자가 탐지 후보를 수락 또는 반려 |

분석 요청 응답은 `202 Accepted`와 다음 본문을 반환한다.

```json
{
  "id": "e0942c43-a149-4f8c-b8eb-ac1ec01c34b7",
  "frameId": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
  "status": "queued",
  "createdAt": "2026-08-12T08:09:40Z"
}
```

탐지 검토 요청:

```json
{
  "reviewStatus": "accepted"
}
```

### 5.7 임장 리포트와 매물 비교

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/v1/inspections/{inspectionId}/report` | 임장 리포트 조회 |
| `POST` | `/api/v1/comparisons` | 둘 이상의 완료된 임장 비교 |

비교 요청:

```json
{
  "inspectionIds": [
    "44d25c46-8391-43cf-903b-b5b580023861",
    "0709c4dc-ab61-428f-a50e-1242e75d9a07"
  ]
}
```

리포트와 비교의 `riskLevel`은 산정 규칙, 단계 이름, 근거 표시 방법이 확정된 뒤 추가한다.

## 6. API 서버와 AI 작업자 사이의 계약

이 계약은 내부 큐 메시지 또는 내부 API에만 사용한다. Android 앱에 AI 제공자 정보나 저장소 내부 경로를 노출하지 않는다.

분석 작업 요청 `AnalysisJob`:

```json
{
  "analysisId": "e0942c43-a149-4f8c-b8eb-ac1ec01c34b7",
  "frameId": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
  "objectKey": "private/frames/13077d2f-7d31-42a9-89f4-a25feb68f4d5.jpg",
  "requestedAt": "2026-08-12T08:09:40Z"
}
```

분석 결과 `AnalysisResult`:

```json
{
  "analysisId": "e0942c43-a149-4f8c-b8eb-ac1ec01c34b7",
  "frameId": "13077d2f-7d31-42a9-89f4-a25feb68f4d5",
  "modelVersion": "defect-detector-2026-08",
  "detections": [
    {
      "classId": 1,
      "label": "mold",
      "confidence": 0.87,
      "bbox": {
        "x": 0.125,
        "y": 0.2,
        "width": 0.3,
        "height": 0.25
      }
    }
  ],
  "completedAt": "2026-08-12T08:09:44Z"
}
```

- AI 작업자는 사용자 체크리스트나 사용자 검토 상태를 직접 변경할 수 없다.
- 서버는 결과의 `analysisId`, `frameId`, 라벨 쌍, 신뢰도와 좌표 범위를 검증한다.
- AI 작업자는 모델의 `bbox`를 API 공통 형식으로 변환한 뒤 결과를 전달한다.
- 같은 `analysisId` 결과가 재전송되어도 중복 탐지가 생기지 않도록 멱등하게 처리한다.
- 실패 결과에는 자유 형식 오류 대신 합의된 `failureCode`를 사용하며 상세 내부 오류는 로그에만 남긴다.

## 7. 아직 확정하지 않은 항목

아래 항목은 담당자 합의와 관련 문서 갱신 전까지 구현 계약이 아니다.

- 데모 접근 토큰의 만료 시간
- 실제 소셜 인증 제공자, 토큰 재발급 방식과 장기 로그인 정책
- 체크리스트 전체 항목, 중요도, 질문 템플릿
- 업로드 URL 만료 시간
- AI 분석 재시도 횟수와 `failureCode` 목록
- `risk_level` 단계 이름과 계산 기준
- `report`와 `comparison`의 상세 응답 필드
- 매물과 사용자 계정 메타데이터를 즉시 삭제할지 복구 가능한 소프트 삭제로 처리할지 여부

## 8. 계약 변경 규칙

1. 용어의 이름이나 의미를 바꾸면 공통 용어집의 변경 규칙과 이력을 먼저 반영한다.
2. 필드 삭제, 타입 변경, enum 값 변경은 호환성 검토와 적용 날짜 없이 진행하지 않는다.
3. 공통 요청·응답 또는 상태값 변경은 Android, API, AI 작업자 담당자가 함께 검토한다.
4. 현재 확정된 계약을 기준으로 OpenAPI 명세를 작성하고 `server/shared-types`의 타입을 생성하거나 구현한다.
5. 실제 구현과 계약 테스트까지 끝난 항목만 작업 체크리스트에서 완료 처리한다.

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|---|---|---|
| 2026-08-12 | Draft 0.1 | 공통 용어집을 기준으로 MVP 리소스, 상태값, API와 AI 결과 계약 초안 작성 |
| 2026-08-12 | 1.0 | 데모 로그인, `project` 제외, 체크리스트 상태, AI 라벨·bbox 변환, 프레임 규격·보관 정책을 확정하여 MVP 계약 승인 |
| 2026-08-13 | 1.1 | 사용자가 직접 입력하는 매물의 보증금, 월세, 관리비, 전용면적, 층수, 옵션, 부동산 연락처를 추가 |
| 2026-08-13 | 1.1 | 면적은 ㎡를 기준값으로 유지하고 앱에서 평 단위로 변환해 입력·표시하는 규칙을 명확히 함 |
| 2026-08-14 | 1.2 | 실시간 스트리밍 분석을 제외하고 안경 기본 영상 촬영, 휴대전화 원본 보관, 촬영 중 또는 촬영 후 생성한 JPEG의 출처·영상 내 시점 계약을 확정함 |
