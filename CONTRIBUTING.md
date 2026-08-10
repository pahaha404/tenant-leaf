# 팀 개발 시작 안내

## 처음 한 번만 설치

- Git
- Android Studio와 JDK 17
- Docker Desktop
- Python 3.11 이상 (AI 학습·평가 담당자)
- Meta Wearables Device Access Toolkit과 테스트 기기 또는 Mock Device Kit (안경 담당자)

프로젝트를 받은 뒤 아래를 실행합니다.

```powershell
git clone https://github.com/pahaha404/tenant-leaf.git
cd tenant-leaf
git switch develop
git pull origin develop
```

## 매일 작업 순서

1. `develop`으로 이동해 최신 변경을 받습니다.
2. 담당 기능 브랜치를 새로 만듭니다.
3. 담당 폴더 안에서 작업하고, 변경을 작게 나누어 커밋합니다.
4. GitHub에 브랜치를 올린 뒤 `develop`을 대상으로 Pull Request를 만듭니다.
5. 리뷰와 테스트가 끝난 뒤에만 병합합니다.

```powershell
git switch develop
git pull origin develop
git switch -c feature/api-visit-session

# 작업 후
git status
git add services/api
git commit -m "feat(api): 방문 세션 생성 API 추가"
git push -u origin feature/api-visit-session
```

## 금지 사항

- `main`, `develop` 직접 푸시
- 비밀키, `.env`, 실제 현장 미디어, 원본 학습 데이터, 모델 가중치 커밋
- 동의 없이 개인식별 가능 이미지·음성·문서를 AI 학습에 사용
- 다른 팀원의 변경을 확인 없이 삭제하거나 강제 푸시

자세한 AI 데이터 규칙은 `ml/README.md`를 확인합니다.
