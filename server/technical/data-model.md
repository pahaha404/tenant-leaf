# MVP 데이터 모델

공통 API 계약 1.2에서 확정한 핵심 데이터 단위입니다. 데이터베이스 테이블과 관계는 구현하면서 이 계약에 맞춰 구체화합니다.

## 핵심 엔티티

- User
- Property
- Inspection
- ChecklistItem
- Device
- Frame: 촬영 중 생성하거나 촬영 완료 영상에서 추출한 분석용 JPEG와 원본 영상 클라이언트 ID·영상 내 시점
- Analysis
- Detection

원본 임장 영상은 사용자의 휴대전화 갤러리에만 보관하며 서버 엔티티나 객체 저장소 파일로 만들지 않습니다.
