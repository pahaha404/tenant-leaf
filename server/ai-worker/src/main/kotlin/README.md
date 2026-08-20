# Kotlin Worker 보류

MVP의 JPEG 추론 Worker는 상위 `server/ai-worker/tenant_leaf_worker/`의 Python 구현을 사용합니다. 이 폴더에는 실행 코드를 추가하지 않습니다.

API 서버는 계속 Kotlin + Spring Boot로 유지하며, 무거운 YOLO 추론은 API HTTP 요청 안에서 실행하지 않습니다.
