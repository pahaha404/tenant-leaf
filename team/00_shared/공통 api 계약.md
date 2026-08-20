# 세입세잎 공통 API 계약

> 상태: 검토 초안 2.1
>
> 기준일: 2026-08-19
>
> 기준 용어: [`공통용어집.md`](../../server/backendmds/공통용어집.md)
>
> 도메인 기준: [`도메인 규칙.md`](../../server/backendmds/도메인%20규칙.md)

Android 앱, Kotlin API 서버와 AI 작업자가 같은 이름과 데이터 의미를 사용하기 위한 MVP 계약이다. 2.0은 현장 체크리스트 중심의 1.2 계약을 **구역·관찰·근거 미디어 중심**으로 교체했고, 2.1은 JPEG 미디어 등록·직접 업로드·완료·재시도·조회 HTTP 계약을 확정한 검토 초안이다. AI 판별 정확도를 우선하는 MVP 방침에 따라 분석용 JPEG의 최대 크기는 사진당 2MiB로 확정한다.

이 문서에서 `확정`으로 표시한 의미와 상태값만 구현 기준으로 사용한다. 요청·응답 형식이 아직 합의되지 않은 API는 설명만 남기고 OpenAPI에서 제외한다.

## 1. 반드시 지키는 서비스 흐름

```text
AI 글래스 기본 고화질 영상 촬영
→ Meta AI 동반 앱을 통해 휴대전화 갤러리로 가져오기
→ Android가 영상을 선택하고 고정 3초 구간마다 JPEG 후보 생성
→ 선명한 JPEG만 API를 거쳐 객체 저장소에 업로드
→ AI Worker가 비동기로 구역 분류와 하자 의심 관찰 후보 생성
→ 사용자가 구역별 관찰과 근거 사진 확인
→ 분석 종료 후 리포트 생성
```

- AI 글래스는 API 서버나 AI 제공자를 직접 호출하지 않는다.
- Android 앱은 AI 제공자 API 키를 저장하거나 AI 제공자를 직접 호출하지 않는다.
- 원본 영상은 사용자의 휴대전화 갤러리에만 보관하며 API, PostgreSQL, 객체 저장소와 AI Worker에 업로드하지 않는다.
- 휴대전화 갤러리 URI와 로컬 파일 경로는 API 요청과 로그에 포함하지 않는다.
- MVP에서는 촬영 중 실시간 스트리밍 분석을 실행하지 않는다.
- 촬영 중 JPEG 생성은 실기기에서 녹화 중단·화질 저하·시점 오차가 없음을 확인한 경우에만 선택적으로 사용한다.
- AI 결과는 실제 하자·위험·안전·수리 필요·계약 가능 여부를 확정하지 않는 `확인 필요 관찰`이다.
- 현장 체크리스트 상태, 체크리스트 완료율과 AI의 체크리스트 자동 판정은 사용하지 않는다.

## 2. 공통 HTTP 규칙

| 항목 | 규칙 |
|---|---|
| API 기본 경로 | `/api/v1` |
| 본문 형식 | `application/json` |
| JSON 필드명 | `camelCase` |
| ID | UUID 문자열 |
| 날짜와 시간 | ISO 8601 UTC 문자열. 예: `2026-08-18T07:30:00Z` |
| 선택값 | 값이 없으면 필드를 생략한다. 값을 지우는 PATCH 요청에만 명시적인 `null`을 사용한다. |
| 페이지 조회 | `page`, `size`를 사용하며 `page`는 0부터 시작한다. |
| enum | OpenAPI에 정의된 대소문자를 그대로 사용한다. 현재 상태·구역 값은 대문자, AI 원본 라벨은 소문자이며 `Bearer` 같은 프로토콜 값도 명세 표기를 바꾸지 않는다. |
| 언어 | 코드와 API는 영문 표준명, 사용자 화면은 한글 표준명을 사용한다. |

성공 응답은 불필요한 공통 포장 객체 없이 리소스 또는 페이지 객체를 그대로 반환한다.

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

### 2.1 인증과 데이터 접근

MVP에서는 실제 소셜 로그인 대신 데모 사용자를 사용한다. 데모 로그인 성공 후 사용자용 API에는 다음 헤더를 사용한다.

```http
Authorization: Bearer <access-token>
```

- 서버는 토큰의 사용자 ID로 매물과 그 하위 임장·미디어·관찰·리포트의 접근 권한을 검사한다.
- 여기서 사용자가 `소유`한 매물은 부동산의 법적 소유권이 아니라 사용자가 앱에 등록해 접근 권한을 가진 기록을 뜻한다.
- 다른 사용자의 리소스 존재를 숨겨야 할 때는 `404`를 반환할 수 있다.
- 실제 소셜 인증, 토큰 재발급과 장기 로그인은 이 계약의 범위가 아니다.

### 2.2 오류 응답

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
| `403` | `FORBIDDEN` | 인증된 사용자가 수행할 수 없는 동작 |
| `404` | `PROPERTY_NOT_FOUND` | 요청한 리소스가 없거나 접근할 수 없음 |
| `404` | `INSPECTION_NOT_FOUND` | 요청한 임장 기록이 없거나 접근할 수 없음 |
| `404` | `MEDIA_NOT_FOUND` | 요청한 미디어가 없거나 접근할 수 없음 |
| `409` | `INVALID_STATE_TRANSITION` | 현재 상태에서는 요청을 수행할 수 없음 |
| `409` | `IDEMPOTENCY_KEY_CONFLICT` | 같은 멱등성 키를 이전 요청과 다른 내용으로 재사용함 |
| `409` | `CLIENT_MEDIA_ID_CONFLICT` | 같은 임장의 `clientMediaId`를 다른 사진 메타데이터로 재사용함 |
| `413` | `FILE_TOO_LARGE` | JPEG 업로드 허용 크기 초과 |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | JPEG가 아닌 파일 형식 |
| `500` | `INTERNAL_ERROR` | 서버 내부 오류 |

클라이언트는 사용자 문구인 `message`가 아니라 안정된 식별자인 `code`로 분기한다. 내부 예외 메시지와 스택 트레이스는 응답에 포함하지 않는다.

### 2.3 멱등성 원칙

미디어 등록·완료·재시도 API는 필수 `Idempotency-Key` 헤더를 사용한다.

- Android는 같은 논리 사진에 재시도해도 바뀌지 않는 `clientMediaId` UUID를 사용한다.
- `(inspectionId, clientMediaId)`는 중복될 수 없다.
- 같은 `(inspectionId, clientMediaId)`와 같은 메타데이터를 다시 등록하면 기존 `mediaId`를 재사용하고, 메타데이터가 다르면 `409 CLIENT_MEDIA_ID_CONFLICT`를 반환한다.
- 같은 키와 같은 요청은 기존 결과를 반환하고 DB 행이나 저장소 객체를 중복 생성하지 않는다.
- 업로드 실패 시 같은 `mediaId`, `clientMediaId`와 저장소 키를 유지하고 새 서명 URL만 발급한다. 자동 재시도는 최대 3회이며 이후에는 사용자가 직접 재시도한다.
- Android가 분석 대상 미디어 등록을 끝냈다고 확정한 뒤에는 신규 미디어를 추가하지 않고 이미 등록된 실패 미디어만 재시도한다.
- 같은 사용자·HTTP 메서드·경로에서 같은 키를 다른 요청 내용에 재사용하면 `409 IDEMPOTENCY_KEY_CONFLICT`를 반환한다.

## 3. 확정 상태값

### 3.1 임장 상태 `InspectionStatus`

```text
IN_PROGRESS → ENDED
      └────→ CANCELLED
```

| 값 | 의미 |
|---|---|
| `IN_PROGRESS` | 사용자가 안경으로 현장 원본 영상을 촬영 중 |
| `ENDED` | 촬영을 정상 종료했으며 영상 가져오기·JPEG 생성·업로드·분석 가능 |
| `CANCELLED` | 촬영을 중단해 신규 업로드와 분석 대상에서 제외됨 |

- 실제 촬영 시작 시 `IN_PROGRESS` 임장을 생성한다.
- `ENDED`와 `CANCELLED`는 되돌릴 수 없다. 다시 점검하면 새 임장을 만든다.
- 보관은 상태값이 아니라 `archivedAt`으로 관리한다.
- 미디어 업로드와 분석은 `ENDED` 임장에서만 허용한다.

### 3.2 임장 전체 분석 상태 `InspectionAnalysisStatus`

| 값 | 의미 |
|---|---|
| `NOT_STARTED` | 등록된 분석 미디어가 없음 |
| `UPLOADING` | 미디어 등록·업로드·객체 확인 또는 업로드 재시도 단계 |
| `QUEUED` | 업로드 완료 후 분석 요청 준비 또는 대기열 처리 중 |
| `ANALYZING` | 하나 이상의 미디어가 분석 중 |
| `PARTIAL_COMPLETED` | 일부 성공 결과가 있고 나머지가 진행 중 또는 실패 |
| `COMPLETED` | 미디어 집합 확정 후 모든 분석 대상 처리가 성공함 |
| `FAILED` | 미디어 집합 확정 후 성공한 분석 없이 모든 대상이 최종 실패함 |

Android가 이 값을 확정해 보내지 않는다. 서버가 삭제되지 않은 등록 미디어 상태와 분석 대상 미디어 집합 확정 여부를 보고 다음 우선순위로 계산한다.

1. 등록 미디어 없음 → `NOT_STARTED`
2. 성공 분석 있음 + 집합 미확정 또는 다른 작업 진행·최종 실패 → `PARTIAL_COMPLETED`
3. 분석 중 있음 → `ANALYZING`
4. `analysisStatus=QUEUED` 또는 `uploadStatus=UPLOADED` + `analysisStatus=NOT_REQUESTED` 있음 → 집합 확정 여부와 관계없이 `QUEUED`
5. `PENDING`, 업로드 중, 재시도 가능한 업로드 실패가 있거나, 집합 미확정 상태에서 업로드·분석 최종 실패만 남음 → `UPLOADING`
6. 집합 확정 + 전체 분석 성공 → `COMPLETED`
7. 집합 확정 + 일부 분석 성공 + 나머지 최종 실패 → `PARTIAL_COMPLETED`
8. 집합 확정 + 성공 없음 + 전체 업로드·분석 최종 실패 → `FAILED`

`COMPLETED`와 `FAILED`는 미디어 집합 확정 전에는 반환하지 않는다. 집합 확정 사실은 서버가 저장하지만 정확한 필드와 확정 API는 P0 합의 전까지 OpenAPI에 추가하지 않는다. 분석 결과가 관찰 0건이어도 분석 성공으로 집계한다.

### 3.3 미디어 상태

`MediaUploadStatus`:

| 값 | 의미 |
|---|---|
| `PENDING` | 업로드 등록 후 객체 확인 전 |
| `UPLOADING` | 업로드 진행 중 |
| `UPLOADED` | 서버가 객체의 존재·크기·형식을 확인함 |
| `FAILED` | 업로드 실패 |

`MediaAnalysisStatus`:

| 값 | 의미 |
|---|---|
| `NOT_REQUESTED` | 분석 요청 전 |
| `QUEUED` | 분석 대기열 등록됨 |
| `ANALYZING` | AI Worker가 분석 중 |
| `COMPLETED` | 분석이 성공적으로 끝남. 관찰 0건도 포함 |
| `FAILED` | 재시도 후 최종 실패 |

삭제 여부는 업로드 상태에 섞지 않고 별도의 삭제 시각으로 관리한다.

### 3.4 구역 `Zone`

| API 값 | 앱 표시 |
|---|---|
| `ENTRANCE_COMMON` | 현관·공용 |
| `KITCHEN` | 주방 |
| `WINDOW_VENTILATION` | 창틀·환기 |
| `LIVING_ROOM` | 거실·방 |
| `BATHROOM` | 화장실 |
| `UNKNOWN` | 구역 확인 필요 |

- `UNKNOWN` 미디어를 임의의 정규 구역으로 넣지 않는다.
- `UNKNOWN`은 다섯 개 정규 구역 완료 수에 포함하지 않는다.
- AI 원본 구역과 `zoneConfidence`는 사용자가 보정하더라도 덮어쓰지 않는다.

### 3.5 구역별 분석 상태 `ZoneAnalysisStatus`

| 값 | 의미 |
|---|---|
| `NO_MEDIA` | 해당 구역에 분류된 분석 사진이 없음 |
| `UPLOADING` | 미디어 업로드 중 |
| `QUEUED` | 분석 대기 중 |
| `ANALYZING` | 분석 중 |
| `COMPLETED` | 분석 성공 결과를 조회할 수 있음 |
| `PARTIAL_FAILED` | 성공 결과와 최종 실패 미디어가 함께 있음 |
| `FAILED` | 성공 결과 없이 해당 구역 분석이 최종 실패 |

`NO_MEDIA`는 하자가 발견되지 않았다는 뜻이 아니다. 진행 상태가 섞여 있으면 `ANALYZING > QUEUED > UPLOADING` 순으로 표시하고, 완료 결과가 이미 있는지는 별도 `hasResults`로 제공한다.

### 3.6 관찰 검토 상태 `ObservationStatus`

```text
ACTIVE → VIEWED → DISMISSED
ACTIVE ───────────→ DISMISSED
DISMISSED ────────→ VIEWED
```

| 값 | 의미 |
|---|---|
| `ACTIVE` | 아직 사용자가 열람하지 않은 활성 관찰 |
| `VIEWED` | 사용자가 관찰과 근거를 열람함 |
| `DISMISSED` | 사용자가 기본 리포트에서 제외함 |

`DISMISSED`는 `하자 아님`이나 오탐 확정 판정이 아니다. 복원하면 이미 확인한 상태인 `VIEWED`로 돌아가고 AI 원본 결과는 유지한다.

### 3.7 리포트 상태 `ReportStatus`

| 값 | 의미 |
|---|---|
| `NOT_REQUESTED` | 생성 요청 또는 자동 생성 조건 충족 전 |
| `WAITING_FOR_ANALYSIS` | 분석 종료 대기 중 |
| `GENERATING` | 리포트 생성 중 |
| `COMPLETED` | 성공 미디어가 하나 이상이고 최종 실패가 없음 |
| `PARTIAL_COMPLETED` | 성공 미디어가 하나 이상이고 일부가 최종 실패 |
| `FAILED` | 성공 분석 미디어가 없음 |

성공 분석의 관찰이 0건이어도 `COMPLETED`다. 이때 화면에는 `하자 없음`이 아니라 `현재 촬영 근거에서 확인 필요 관찰이 생성되지 않음`으로 표시한다.

## 4. 공통 데이터 의미

### 4.1 사용자 `User`

```json
{
  "id": "2fb2b214-523f-4fcf-bce7-72881a5ccb32",
  "displayName": "데모 사용자",
  "createdAt": "2026-08-18T07:30:00Z"
}
```

MVP의 사용자는 실제 소셜 계정과 연결되지 않은 데모 사용자다.

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
  "createdAt": "2026-08-18T07:30:00Z",
  "updatedAt": "2026-08-18T07:30:00Z"
}
```

- `name`만 생성 필수이며 나머지는 사용자 선택 입력이다.
- 금액은 원(KRW) 단위의 0 이상 정수로 저장한다.
- 면적은 ㎡ 단위의 0보다 큰 값만 저장한다. 평 값은 저장하지 않고 Android에서 `1평 = 3.305785㎡`로 변환한다.
- 계산에는 반올림하지 않은 값을 사용하고 화면에 표시할 때만 반올림한다.
- `floor`, `options`, `brokerContact`는 사용자가 작성한 참고 정보이며 서버가 정확성을 보증하지 않는다.

### 4.3 임장 `Inspection`

```json
{
  "id": "44d25c46-8391-43cf-903b-b5b580023861",
  "propertyId": "b5df53ee-d948-4c16-879f-8a4ec0ca64cd",
  "status": "IN_PROGRESS",
  "analysisStatus": "NOT_STARTED",
  "startedAt": "2026-08-18T08:00:00Z",
  "endedAt": null,
  "cancelledAt": null,
  "archivedAt": null,
  "createdAt": "2026-08-18T08:00:00Z"
}
```

- `analysisStatus`와 집계 수치는 서버가 원본 미디어 상태에서 계산한다.
- `endedAt`은 `ENDED`, `cancelledAt`은 `CANCELLED`로 전환할 때 기록한다.
- `archivedAt`은 상태 전이와 별개이며 설정·해제 API는 아직 확정하지 않았다.

### 4.4 휴대전화 원본 영상 `LocalRecordedVideo`

`LocalRecordedVideo`는 Android 로컬 개념이며 서버 리소스가 아니다.

- Android가 각 영상 클립에 불투명 UUID인 `sourceVideoId`를 부여할 수 있다.
- `sourceVideoId`는 서버 영상 파일 ID가 아니며 원본 영상 파일을 찾는 URL도 아니다.
- 원본 영상 파일, 갤러리 URI, 로컬 경로를 받는 API는 만들지 않는다.

### 4.5 분석 사진 `Media(PHOTO)`

다음은 확정된 도메인 의미다. HTTP 요청·응답은 아래 4.6의 전용 DTO를 사용하고 이 조회 모델을 등록 요청으로 재사용하지 않는다.

| 필드 | 의미 |
|---|---|
| `mediaId` | 서버가 부여한 UUID |
| `clientMediaId` | Android가 생성하며 재시도에도 유지하는 UUID |
| `inspectionId` | 사진이 속한 임장 |
| `mediaType` | MVP에서는 `PHOTO`만 허용 |
| `captureSource` | `META_GLASS` 또는 `ANDROID_CAMERA` |
| `frameOrigin` | `DURING_RECORDING_CAPTURE` 또는 `POST_RECORDING_EXTRACTION` |
| `sourceVideoId` | Android가 영상 클립에 부여한 UUID |
| `sourceVideoOffsetMs` | 영상 시작 후 실제 프레임 시점(ms) |
| `capturedAt` | 실제 촬영 시각 또는 영상 시점으로 계산한 시각 |
| `uploadStatus` | 서버가 관리하는 업로드 상태 |
| `analysisStatus` | 서버가 관리하는 분석 상태 |
| `zone`, `zoneConfidence` | AI의 원본 구역 분류와 신뢰도 |
| `width`, `height`, `contentLength` | 검증한 실제 JPEG 정보 |

객체 저장소 내부 키는 서버 내부 전용이다. Android에는 요청 시 발급한 짧은 만료 시간의 서명 URL만 제공한다.

### 4.6 JPEG 미디어 업로드 HTTP 계약

업로드는 `등록 → 서명 URL로 객체 저장소에 직접 PUT → 완료 확인 → 조회` 순서다. 서버 API가 JPEG 바이트를 중계하거나 PostgreSQL에 저장하지 않는다.

| 메서드와 경로 | 의미 |
|---|---|
| `POST /inspections/{inspectionId}/media/upload-requests` | 1~20장의 미디어를 등록하고 각 미디어의 15분 유효 서명 업로드 URL 발급 |
| `POST /media/{mediaId}/upload-complete` | 객체 존재·형식·크기를 확인한 뒤 업로드 완료 확정 |
| `POST /media/{mediaId}/upload-retry` | 같은 미디어 ID와 저장소 키를 유지하며 새 15분 유효 URL 발급 |
| `GET /inspections/{inspectionId}/media` | 임장의 미디어 페이지 조회 |
| `GET /media/{mediaId}` | 미디어 상태와 메타데이터 조회 |

- 등록·완료·재시도에는 `Idempotency-Key`가 필수다.
- 등록 요청 본문은 `items` 배열이며 최소 1장, 최대 20장이다. 20장은 요청 한 번의 묶음 한도이지 임장 전체 한도가 아니다.
- 배치는 원자적으로 처리한다. 한 항목이라도 형식·권한·상태 검증에 실패하면 새 항목을 하나도 등록하지 않고 요청 전체를 오류로 반환한다.
- 예를 들어 10분 영상을 고정 3초 간격으로 처리해 약 200장을 선택하면 20장씩 약 10번에 나눠 등록할 수 있다.
- 각 등록 항목에는 `clientMediaId`, `zone`, `contentType`, `fileSize`, `width`, `height`, `sourceVideoId`, `sourceVideoOffsetMs`, `frameOrigin`, `captureSource`, `capturedAt`을 보낸다.
- MVP 등록 요청의 `contentType`은 `image/jpeg`, `frameOrigin`은 `POST_RECORDING_EXTRACTION`만 허용한다.
- `sourceVideoId`는 앱이 만든 불투명 UUID일 뿐이며 원본 영상, 갤러리 URI와 로컬 경로를 서버에서 찾을 수 있는 값이 아니다.
- 등록 성공 응답은 항목마다 `mediaId`, `clientMediaId`, `uploadUrl`, `expiresAt`, `uploadStatus=PENDING`을 반환한다.
- Android는 서명 URL에 `Content-Type: image/jpeg`로 JPEG를 직접 `PUT`하고 URL·서명 값을 로그나 영구 저장소에 남기지 않는다.
- 완료 요청에서 서버는 객체 저장소의 파일 존재 여부, 실제 JPEG 형식과 최대 `2,097,152 bytes`를 확인한 뒤에만 `UPLOADED`로 바꾼다.
- 완료 확인 전에는 분석을 시작하지 않는다. 같은 완료 요청의 재전송은 기존 미디어를 그대로 반환한다.
- 자동 재시도는 같은 `mediaId`, `clientMediaId`, 저장소 키를 유지한 채 최대 3회 수행하며 그 이후의 재시도는 사용자 행동으로 시작한다.
- 목록은 공통 `page`, `size` 규칙을 따르며 다른 사용자의 미디어는 존재 여부를 숨기기 위해 `404`로 응답할 수 있다.
- 등록·재시도는 `ENDED` 임장에서만 허용한다. `IN_PROGRESS` 또는 `CANCELLED`이면 `409 INVALID_STATE_TRANSITION`을 반환한다.
- 임장당 전체 미디어 상한과 분석 대상 미디어 집합 확정 API는 별도 P0 합의 전까지 클라이언트와 서버가 임의로 가정하지 않는다.

### 4.7 JPEG 생성·검증·보관 규칙

- 기본 생성 방식은 촬영 완료 후 원본 영상의 고정 3초 구간마다 앞·중간·뒤 후보를 비교해 최대 한 장을 고르는 것이다.
- 모든 후보가 흐리거나 어두워도 가장 나은 한 장을 남기고 화질 확인 필요 상태를 표시한다.
- `sourceVideoOffsetMs`는 디코더가 제공한 실제 영상 시점을 사용한다.
- JPEG 품질은 `90 → 85 → 80` 순으로 낮추고, 그래도 2MiB를 넘을 때만 픽셀 크기를 단계적으로 줄인다.
- 서버는 JPEG(`.jpg`, `.jpeg`, MIME `image/jpeg`)와 사진당 최대 `2,097,152 bytes`만 허용한다.
- 업로드 실패·미완료 파일은 최대 24시간, 관찰 없는 자동 사진은 분석 후 최대 7일, 관찰 근거·중요 사진은 임장 종료 후 최대 30일 보관한다.
- 임장 삭제 요청 미디어는 즉시 사용을 막고 최대 7일 안에 완전히 삭제한다.
- 이 보관 규칙은 실제 업로드 기능을 열기 전에 저장소 수명 주기와 서버 정리 작업으로 검증해야 한다.

### 4.8 AI 원본 라벨과 바운딩 박스

클래스 ID와 영문 라벨은 고정 쌍이다.

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
| `12` | `other` | 기타 |

`other`는 하자 후보 기준에는 부합하지만 기존 12개 유형으로 구체적으로 분류하지 못했을 때만 사용한다. 배경, 관찰 없음, 낮은 신뢰도 또는 분석 실패를 대신하지 않는다. `other`만 `OTHER_CHECK_NEEDED` 관찰 유형으로 변환하는 규칙이 확정됐으며 나머지 전체 변환표는 미확정이다.

`bbox`는 이미지 왼쪽 위를 원점으로 하는 정규화 좌표다.

```json
{
  "x": 0.125,
  "y": 0.2,
  "width": 0.3,
  "height": 0.25
}
```

네 값은 `0.0` 이상 `1.0` 이하이고 `x + width <= 1.0`, `y + height <= 1.0`이어야 한다.

### 4.8 관찰 `Observation`과 근거 사진

- 관찰은 근거 JPEG가 있는 하자 의심 흔적이며 실제 하자 판정이 아니다.
- 관찰 하나에는 같은 임장의 `UPLOADED` JPEG를 최소 1장, 최대 3장 연결한다.
- 대표 근거 사진은 정확히 한 장 지정한다.
- 한 사진을 여러 관찰의 근거로 사용하는 것은 허용한다.
- 근거 사진이 삭제돼도 관찰과 연결 기록은 유지하고 `근거 사진 사용 불가`로 표시한다.
- 최종 `ObservationType`과 AI 라벨 전체 변환표가 미확정이므로 Observation 요청·응답 스키마는 이번 OpenAPI에서 제외한다.

### 4.9 리포트 `Report`

리포트는 임장이 `ENDED`이고 분석 대상 미디어 집합이 확정됐으며 모든 분석이 성공 또는 최종 실패 상태가 된 뒤에만 생성할 수 있다.

- 성공 미디어가 하나 이상이고 실패가 없으면 `COMPLETED`다.
- 성공 미디어가 하나 이상이고 일부가 실패하면 `PARTIAL_COMPLETED`다.
- 성공 미디어가 하나도 없으면 `FAILED`이며 `NO_ANALYZABLE_MEDIA` 사유를 제공한다.
- `DISMISSED` 관찰은 기본 리포트에서 제외한다.
- 사용자 메모는 `내 메모`, AI 관찰 설명은 `확인 필요 관찰`로 구분한다.
- 상세 응답과 생성·갱신 정책이 미확정이므로 Report API는 이번 OpenAPI에서 제외한다.

### 4.10 사용자 메모·안심 가이드·알림

- MVP 사용자 메모는 텍스트만 지원하고 임장 전체 또는 선택한 구역에 연결한다.
- 메모는 화면과 리포트에서 `내 메모`로 표시해 AI 설명과 구분한다.
- 메모의 최대 길이와 수정·삭제 API가 미확정이므로 이번 OpenAPI에는 포함하지 않는다.
- 음성·STT는 MVP 이후로 미룬다. 도입하더라도 STT 결과가 관찰 유형·구역·검토 상태를 자동 변경해서는 안 된다.
- 방문 전 안심 가이드는 버전이 있는 Android 정적 콘텐츠로 제공하고 사용자가 건너뛸 수 있게 한다. 마지막 확인 버전은 Android 로컬에만 저장한다.
- MVP는 푸시 대신 앱 내부 상태 조회와 새로고침을 사용한다.

## 5. 이번 OpenAPI에 포함하는 Android API

이번 OpenAPI에는 요청·응답 구조가 확정된 인증, 매물과 임장 생명주기만 포함한다.

### 5.1 데모 로그인

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/demo` | 실제 소셜 연동 없는 데모 접근 토큰 발급 |

### 5.2 매물

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/properties` | 매물 생성 |
| `GET` | `/api/v1/properties` | 내 매물 목록 조회 |
| `GET` | `/api/v1/properties/{propertyId}` | 매물 상세 조회 |
| `PATCH` | `/api/v1/properties/{propertyId}` | 매물 정보 수정 |
| `DELETE` | `/api/v1/properties/{propertyId}` | 매물 삭제 |

매물 PATCH에서 생략한 필드는 유지하고 명시적인 `null`은 선택값 삭제를 뜻한다.

현재 삭제 계약은 하위 임장이 없는 매물에만 확정 적용한다. 임장이 존재하는 매물의 삭제를 거부할지, 보관할지 또는 하위 데이터를 함께 처리할지는 P0 결정 전까지 구현하거나 클라이언트가 가정하지 않는다.

### 5.3 임장

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/properties/{propertyId}/inspections` | 실제 촬영 시작과 함께 `IN_PROGRESS` 임장 생성 |
| `GET` | `/api/v1/properties/{propertyId}/inspections` | 매물의 임장 목록 조회 |
| `GET` | `/api/v1/inspections/{inspectionId}` | 임장 상세 조회 |
| `PATCH` | `/api/v1/inspections/{inspectionId}/status` | 임장을 `ENDED` 또는 `CANCELLED`로 전환 |

상태 변경 요청:

```json
{
  "status": "ENDED"
}
```

임장 삭제와 보관 설정·해제는 의미가 아직 완전히 합의되지 않아 이번 OpenAPI에서 제외한다.

## 6. API 서버와 AI Worker 사이의 의미 계약

정확한 큐 메시지 스키마는 분석 재시도·실패 코드와 관찰 병합 규칙을 결정한 뒤 확정한다. 현재 반드시 지킬 의미는 다음과 같다.

- 작업 입력은 서버에 `UPLOADED`로 확정된 JPEG의 `mediaId`와 내부 객체 키를 사용한다.
- AI Worker는 휴대전화 원본 영상, 갤러리 URI 또는 로컬 경로를 요청하지 않는다.
- 결과에는 `mediaId`, 원본 `classId`, `label`, `confidence`, `bbox`, `zone`, `zoneConfidence`, `modelVersion`을 보존한다.
- 서버는 라벨 쌍, 신뢰도, bbox와 ID를 검증한다.
- 같은 결과가 재전송돼도 관찰을 중복 생성하지 않는다.
- 성공 결과는 다른 미디어의 실패 때문에 폐기하지 않는다.
- MVP AI Worker에는 음성·STT 작업을 포함하지 않는다.

## 7. OpenAPI에서 제외한 미확정 항목

아래 항목은 설명이나 방향만 합의됐고 정확한 HTTP 계약은 아직 없다. 담당자 합의 전에는 경로, DTO, enum 또는 오류 코드를 임의로 추가하지 않는다.

| 우선순위 | 미확정 항목 |
|---:|---|
| P0 | 최종 `ObservationType`과 AI 13개 라벨 전체 변환표. `other → OTHER_CHECK_NEEDED`만 확정 |
| P0 | 분석 대상 미디어 집합 확정 API와 임장당 전체 미디어 상한 |
| P0 | 인접 프레임의 같은 흔적 병합, 분석 자동 재시도·백오프와 `failureCode` |
| P0 | 촬영 중 JPEG 동시 생성과 영상 시점 동기화의 실기기 검증 |
| P0 | 선명도·밝기·중복 임계값과 2MiB 제한에서의 AI 정확도·저장 용량 검증 기준 |
| P1 | `UNKNOWN` 사용자 보정 API와 이력 조회 범위 |
| P1 | 텍스트 메모 API와 이후 음성·STT 동의·보관 범위 |
| P1 | `DISMISSED` 사유 enum |
| P1 | 리포트 상세·자동 생성·갱신·편집·공유 API |
| P1 | 푸시 토큰과 알림 API |
| P2 | 서버형 안심 가이드와 열람 이력 API |

## 8. 1.2 계약에서 제거한 항목

다음 계약은 새 도메인과 충돌하므로 2.0 활성 계약과 OpenAPI에서 제거한다.

- `ChecklistStatus`, `ChecklistItem`, `confirmedByUser`와 체크리스트 API
- 체크리스트 완료율과 AI의 체크리스트 자동 변경
- `Frame.checklistItemId`와 `Device` 중심 프레임 계약
- `/frames/*`, `/analyses/*`, `/detections/*` 기존 경로
- `DetectionReviewStatus`의 `needs_review`, `accepted`, `rejected`
- 분석 결과를 외부 `Detection`으로 직접 노출하는 방식
- `comparison`과 상세 `report` API

아직 이 API를 구현한 클라이언트가 있다면 병합 전에 영향도를 확인해야 한다. 현재 저장소의 서버와 Android 구현은 매물 API만 사용하므로 이 개정에서 매물 계약은 유지한다.

## 9. 계약 변경 규칙

1. 용어의 이름이나 의미를 바꾸면 공통 용어집과 변경 이력을 먼저 반영한다.
2. 필드 삭제, 타입 변경과 enum 값 변경은 Android·API·AI 담당자가 함께 검토한다.
3. 확정한 변경은 이 문서, OpenAPI, 생성 코드 사용처와 계약 테스트를 같은 변경에서 수정한다.
4. 미확정 항목은 OpenAPI나 구현에서 먼저 결정하지 않는다.
5. 자동 생성된 `build/generated` 코드는 직접 수정하거나 Git에 커밋하지 않는다.
6. OpenAPI 문법 검사, Kotlin 코드 생성, 컴파일과 관련 테스트까지 통과한 항목만 완료 처리한다.

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|---|---|---|
| 2026-08-12 | Draft 0.1 | 공통 용어집을 기준으로 MVP 리소스, 상태값, API와 AI 결과 계약 초안 작성 |
| 2026-08-12 | 1.0 | 데모 로그인, 체크리스트 상태, AI 라벨·bbox, 프레임 규격·보관 정책 확정 |
| 2026-08-13 | 1.1 | 매물의 금액·면적·층·옵션·연락처와 ㎡·평 변환 규칙 추가 |
| 2026-08-14 | 1.2 | 실시간 스트리밍을 제외하고 휴대전화 원본 영상과 JPEG 분석 방식으로 전환 |
| 2026-08-18 | 검토 초안 2.0 | 체크리스트·Frame·Detection 계약을 제거하고 세션·구역·Media·Observation·근거 중심 계약으로 전환. 미확정 Media·Observation·Report HTTP API는 OpenAPI에서 제외 |
| 2026-08-19 | 검토 초안 2.1 | JPEG 미디어 배치 등록(요청당 20장), 15분 서명 URL, 완료·재시도·조회와 멱등성 충돌 계약 확정 |
