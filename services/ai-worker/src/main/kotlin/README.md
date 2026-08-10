# Kotlin AI 워커 코드

Redis 분석 작업을 받아 외부 AI API 또는 모델 서버를 호출하는 Kotlin 코드를 둡니다.

## 예정 패키지

- `consumer`: Redis 작업 수신
- `pipeline`: 음성·이미지 분석 순서
- `client`: STT·TTS·비전 AI API 호출
- `result`: 분석 결과 저장
