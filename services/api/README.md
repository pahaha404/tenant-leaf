# api

Kotlin + Spring Boot 기반 백엔드 API 서버 폴더입니다.

## 책임

- 사용자 인증
- 프로젝트·매물·점검 세션 관리
- 체크리스트와 음성 메모 저장
- 촬영 파일 업로드 URL 발급
- AI 분석 작업 요청과 결과 조회
- 상태 등급·매물 비교 결과 제공

## 예정 구조

- `src/main/kotlin/`: Kotlin 서버 코드
- `src/main/resources/`: Spring 설정·DB 마이그레이션
- `controller/`: REST API 주소별 컨트롤러
- `domain/`: 도메인 모델·서비스 규칙
- `repository/`: DB 조회·저장
- `dto/`: 요청·응답 데이터 형식
- `worker/`: AI 워커 작업 요청
- `src/test/kotlin/`: 서버 테스트

## 권장 서버 기술

- Kotlin
- Spring Boot
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Redis
