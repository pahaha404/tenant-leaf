# 서버 이미지 묶음 공간 분류 및 하자 분석

최종 갱신일: 2026-08-24

백엔드 요청·응답 변경사항과 구현 체크리스트는 `BACKEND_ROOM_CLASSIFICATION_API_CHANGES.md`를 함께 확인한다.

## 확정 처리 흐름

백엔드가 영상의 샘플링과 이미지 품질 선별을 완료한다. Python AI 모듈은 전달받은 이미지 묶음을 다시 필터링하지 않고 공간 분류와 하자 분석을 수행한다.

```text
백엔드 영상 처리
  → 프레임 샘플링·Blur/밝기/중복 필터
  → 시간순 이미지 묶음 저장
  → Python에 이미지 폴더 + jobId 전달
  → Gemini API 공간 분류
  → 연속 이미지 다수결로 공간 구간 안정화
  → Binary YOLO + 다중 클래스 YOLO 하자 추론
  → 공간·하자·근거 crop 통합 JSON 생성
  → 백엔드 저장 및 사용자 리포트 연결
```

공간 클래스는 `bathroom`, `kitchen`, `living_room`, `unknown` 네 가지다. `entrance`와 `window_frame`은 현재 테스트 범위에서 제외한다.

## 서버 설치와 API 키

```bash
pip install -r requirements.txt
```

Gemini 키는 코드나 Git에 저장하지 않고 서버 환경변수로만 등록한다.

```powershell
$env:GEMINI_API_KEY="발급받은_API_KEY"
```

```bash
export GEMINI_API_KEY="발급받은_API_KEY"
```

## 입력 규칙

`--input`에는 이미지 한 장 또는 이미지 폴더를 전달한다. 폴더 입력은 파일명 오름차순으로 처리하므로 백엔드는 시간 순서가 유지되도록 파일명 앞에 순번을 붙인다.

```text
input/property-001/
├── 000001.jpg
├── 000002.jpg
└── 000003.jpg
```

원본 `imageId`와 촬영 시각이 필요하면 선택적으로 manifest를 전달한다.

```json
{
  "images": [
    {
      "filename": "000001.jpg",
      "imageId": "server-image-101",
      "timestampSec": 0.0
    },
    {
      "filename": "000002.jpg",
      "imageId": "server-image-102",
      "timestampSec": 1.0
    }
  ]
}
```

manifest가 없으면 `image-0001` 형식의 ID를 자동 생성하고 `timestampSec`는 `null`로 기록한다.

## 실행

```bash
python -m training.process_image_batch_room_defect \
  --input input/property-001 \
  --manifest input/property-001-manifest.json \
  --job-id property-001 \
  --output output/property-001
```

Gemini 연결 전 전체 구조만 확인하려면 다음 옵션을 추가한다.

```bash
--room-provider disabled
```

이 경우 공간은 모두 `unknown`이지만 기존 2단계 YOLO와 crop·JSON 생성은 실행된다.

## 출력

```text
output/property-001/
├── result.json
└── defect_analysis/
    ├── result.json
    ├── evidence/
    └── crops/
```

최종 `result.json`에는 다음 항목이 포함된다.

- `roomClassification`: API 모델, 호출 횟수, 오류
- `roomSegments`: 연속 이미지 기반 공간 구간
- `images`: 이미지 크기, 공간 결과, 하자 박스
- `observations`: 공간 정보가 결합된 하자와 근거 crop

Gemini 호출이 최종 실패한 이미지는 임의 공간으로 분류하지 않고 `unknown`으로 처리하며 오류를 `roomClassification.errors`에 남긴다.

## 역할 구분

백엔드 담당:

- 영상 업로드·저장
- 프레임 샘플링과 이미지 품질 선별
- 시간순 파일명과 선택적 manifest 생성
- Python worker 실행과 작업 상태 관리
- 결과 및 crop 경로를 인증 URL로 변환
- 공간별 하자 결과를 사용자 리포트에 연결

AI Python 담당:

- 전달된 이미지 전체의 Gemini 공간 분류
- 연속 결과 안정화 및 공간 구간 생성
- 기존 Binary·다중 클래스 YOLO 추론
- 하자 crop과 통합 JSON 생성

## 기능 테스트 체크리스트

- [ ] 서버에 `GEMINI_API_KEY` 환경변수 설정
- [ ] 백엔드가 시간순 이미지 폴더 생성
- [ ] 이미지 묶음 명령 실행 성공
- [ ] `images[].room.raw`와 `stable` 결과 확인
- [ ] 공간 전환 구간과 `roomSegments` 육안 검수
- [ ] 모든 하자 observation에 `room`과 `roomSegmentId` 포함 확인
- [ ] 근거 원본과 하자 crop을 백엔드 URL로 변환
- [ ] Gemini 오류 시 `unknown` fallback과 오류 기록 확인
