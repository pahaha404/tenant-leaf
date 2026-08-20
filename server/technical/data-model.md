# MVP 데이터 모델

공통 API 계약 검토 초안 2.0과 `server/backendmds/도메인 규칙.md`에서 확정한 핵심 데이터 단위입니다. 정확한 테이블과 제약조건은 남은 P0 계약을 확정한 뒤 Flyway 마이그레이션으로 구현합니다.

## 핵심 관계

```text
User
 └─ Property
     └─ Inspection
         ├─ Media(PHOTO) ─ MediaAnalysis
         │              ├─ Zone 분류
         │              └─ Observation 후보
         ├─ Observation ─ EvidenceMedia
         ├─ UserMemo (API 미확정)
         └─ Report (상세 계약 미확정)
```

- `Inspection`은 `IN_PROGRESS → ENDED` 또는 `CANCELLED`로만 전환하며 보관은 `archivedAt`으로 분리합니다.
- `Media`는 객체 저장소 JPEG의 메타데이터만 PostgreSQL에 저장합니다. `(inspectionId, clientMediaId)`는 유일해야 합니다.
- 전체·구역별 분석 상태는 미디어 원본 상태에서 서버가 계산합니다.
- `Observation`은 같은 임장의 업로드 완료 JPEG 1~3장을 근거로 사용하고 대표 근거를 정확히 한 장 둡니다.
- 체크리스트 항목, 완료율과 `accepted/rejected` 탐지 검토 모델은 사용하지 않습니다.
- 원본 임장 영상과 갤러리 URI·로컬 경로는 휴대전화에만 두며 서버 엔티티나 객체 저장소 파일로 만들지 않습니다.
