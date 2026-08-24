# Gemini 하자 보조 검증 연동

작성일: 2026-08-24

## 확정 흐름

공간 분류와 하자 보조 검증은 같은 `GEMINI_API_KEY`와 `gemini-3.5-flash-lite`를 사용한다.

```text
백엔드가 선별한 이미지 묶음
  → Gemini 공간 분류
  → Binary YOLO + 다중 클래스 YOLO
  → 전체 이미지에 D1, D2... 후보 박스 표시
  → 이미지당 Gemini 1회 보조 검증
  → 확신도 0.90 이상의 not_defect 후보 제거
  → 최종 박스 이미지와 result.json 생성
```

하자 영역 crop은 생성하지 않는다. Gemini에는 깨끗한 원본 이미지와 모든 YOLO 후보 박스·식별자가 그려진 이미지를 함께 전달한다. YOLO confidence는 Gemini에 전달하지 않는다. 후보가 여러 개여도 이미지당 API 호출은 한 번이다.

## 삭제 정책

- `defect`: 최종 `detections[]`, `observations[]`에 유지
- `uncertain`: 미탐 방지를 위해 유지
- `not_defect`이고 판단 확신도 0.90 이상: 최종 결과에서 제외
- `not_defect`이지만 판단 확신도 0.90 미만: 유지
- API 오류 또는 응답 누락: `uncertain`으로 처리하여 유지

제외된 후보는 사용자 리포트용 결과에서는 삭제되지만, 테스트 감사와 정책 조정을 위해 `images[].rejectedDetections[]`에 기록한다.

## 실행

```bash
export GEMINI_API_KEY="발급받은_API_KEY"

python -m training.process_image_batch_room_defect \
  --input /server/input/{jobId}/images \
  --manifest /server/input/{jobId}/manifest.json \
  --job-id {jobId} \
  --output /server/output/{jobId}
```

통합 진입점은 하자 Gemini 검증이 기본 활성화된다. 일시적으로 끄려면 `--defect-verifier disabled`를 사용한다.

## 결과 필드

```json
{
  "candidateId": "D1",
  "label": "mold",
  "confidence": 0.42,
  "box": {"left": 120, "top": 80, "right": 640, "bottom": 410},
  "verification": {
    "provider": "gemini",
    "model": "gemini-3.5-flash-lite",
    "verdict": "defect",
    "label": "mold",
    "confidence": 0.91,
    "reason": "벽면에 검은 반점 형태의 흔적이 확인됨",
    "status": "completed"
  },
  "annotatedPath": "output/job-001/annotated/final/000001.jpg"
}
```

이미지별 추가 필드:

- `candidateAnnotatedPath`: Gemini에 전달한 전체 후보 박스 이미지
- `annotatedPath`: `not_defect` 제거 후 최종 박스 이미지
- `rejectedDetections`: Gemini가 `not_defect`로 제외한 후보 감사 기록

백엔드는 `detections[]`와 `observations[]`만 사용자 리포트에 사용하고 `rejectedDetections[]`는 테스트 로그로만 보관한다.

## 2026-08-24 테스트 결과

`defect_test/input` 이미지 16장으로 실행했다.

- YOLO 후보: 121개
- 최종 유지: 14개 (`defect` 13개, `uncertain` 1개)
- 제외: 107개 (`not_defect`)
- Gemini 호출: 16회
- API 오류: 0회
- 총 처리 시간: 약 90.4초

제외율이 약 88.4%로 매우 높다. 정답 라벨이 없는 테스트이므로 정확도 향상으로 해석할 수 없고, 후보/최종 박스 이미지를 사람이 비교해 실제 하자가 삭제됐는지 확인해야 한다.

## 2026-08-24 DACON 50장 테스트

`Data/DACON/open/test` 792장에서 파일명 순서 전체 구간에 걸쳐 50장을 균등 선택했다.

- YOLO 후보: 298개
- 최종 유지: 56개 (`defect` 50개, `uncertain` 6개)
- 제외: 242개 (`not_defect`)
- Gemini 호출: 50회
- API 오류: 0회
- 총 처리 시간: 약 221.6초

무료 등급의 분당 15회 제한에 맞춰 Gemini 호출 간격을 4.2초 이상으로 조절한다. 429 또는 응답 파싱 오류는 최대 3회 재시도하며, 최종 실패 시 해당 후보를 삭제하지 않고 `uncertain`으로 유지한다.

## 개선 정책 동일 50장 재검증

원본+박스 이미지 동시 전달, YOLO confidence 미전달, `not_defect` 확신도 0.90 이상만 제거하는 정책으로 동일한 DACON 50장을 다시 검증했다.

- 동일 YOLO 후보: 298개
- 기존 최종 유지: 56개
- 개선 최종 유지: 124개
- 개선 최종 제외: 174개
- 추가 보존: 68개
- 개선 결과 구성: `defect` 89개, `uncertain` 16개, 낮은 확신도의 `not_defect` 19개
- API 오류: 0회
- 총 처리 시간: 약 225.3초
