# 📅 Day 94: ArgoCD Rollback & 히스토리 관리

## 🎯 오늘의 목표

> ArgoCD의 배포 히스토리를 관리하고, 장애 상황에서 신속하게 롤백하는 방법을 완벽하게 익힙니다.

안정적인 운영을 위한 롤백 전략은 DevOps 엔지니어의 필수 역량입니다.

---

## ⏰ 예상 학습 시간: 3.5시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 히스토리 관리 | 1시간 | Revision 이해 |
| 롤백 전략 | 1시간 30분 | 다양한 롤백 방법 |
| 실습 | 1시간 | 장애 시나리오 |

---

## 📚 Part 1: ArgoCD 배포 히스토리

### Revision 개념

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    ArgoCD Deployment History                                │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   ID   DATE                    REVISION    STATUS                          │
│   ─────────────────────────────────────────────────────────────────────   │
│   0    2026-01-05 10:00:00    abc1234     Deployed                        │
│   1    2026-01-06 14:30:00    def5678     Deployed                        │
│   2    2026-01-07 09:15:00    ghi9012     Deployed (Current)              │
│   3    2026-01-08 11:00:00    jkl3456     Syncing...                      │
│                                                                            │
│   각 배포마다 Git Commit SHA와 함께 기록                                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 히스토리 조회

```bash
# 배포 히스토리 확인
argocd app history myapp

# 예상 출력:
# ID  DATE                           REVISION
# 0   2026-01-05 10:00:00 +0000 UTC  abc1234
# 1   2026-01-06 14:30:00 +0000 UTC  def5678
# 2   2026-01-07 09:15:00 +0000 UTC  ghi9012 (Current)

# 특정 Revision 상세 정보
argocd app manifests myapp --revision 1

# 현재 상태와 이전 상태 비교
argocd app diff myapp --revision 1
```

### kubectl로 확인

```bash
# Application 리소스에서 히스토리 확인
kubectl get application myapp -n argocd -o jsonpath='{.status.history}' | jq

# ReplicaSet 히스토리로 확인
kubectl rollout history deployment/myapp -n default
```

---

## 🛠️ Part 2: 롤백 방법

### 방법 1: ArgoCD CLI 롤백

```bash
# 특정 ID로 롤백
argocd app rollback myapp 1

# 확인
argocd app get myapp

# 예상 출력:
# Name:               argocd/myapp
# Sync Status:        OutOfSync  # Git과 다르므로 OutOfSync
# Health Status:      Healthy
# Revision:           def5678    # 롤백된 Revision
```

### 방법 2: 특정 Git Revision으로 Sync

```bash
# 특정 커밋으로 동기화
argocd app sync myapp --revision abc1234

# 특정 태그로 동기화
argocd app sync myapp --revision v1.0.0

# 특정 브랜치의 최신으로 동기화
argocd app sync myapp --revision release/v1
```

### 방법 3: Git Revert (GitOps 권장)

```bash
# GitOps 방식 롤백 (권장)
cd infra-repo
git revert HEAD
git push

# ArgoCD가 자동으로 Sync (Auto Sync 설정 시)
argocd app get myapp
```

### 방법 4: ArgoCD UI에서 롤백

```
ArgoCD UI:
1. Application 선택
2. History and Rollback 탭
3. 원하는 Revision 선택
4. Rollback 클릭
```

---

## 📚 Part 3: 롤백 시 주의사항

### Auto Sync와 롤백

```
⚠️ Auto Sync가 켜져 있으면:

1. argocd app rollback 실행
2. 이전 버전으로 복원됨
3. 하지만 Git은 최신 상태
4. Auto Sync가 다시 최신으로 동기화!

해결책:
1. Auto Sync 비활성화 후 롤백
2. 또는 Git에서 revert 후 push (권장)
```

### 안전한 롤백 절차

```bash
# 1. Auto Sync 비활성화
argocd app set myapp --sync-policy none

# 2. 롤백 실행
argocd app rollback myapp 1

# 3. 상태 확인
argocd app get myapp
kubectl get pods -n default

# 4. 안정화 확인 후 Git 업데이트
cd infra-repo
git revert HEAD
git push

# 5. Auto Sync 재활성화
argocd app set myapp --sync-policy automated
```

### Database Migration 고려

```
⚠️ 롤백 시 DB 스키마 변경이 있었다면:

1. 애플리케이션만 롤백으로는 불충분
2. DB Migration 롤백도 필요할 수 있음
3. Forward-only migration 전략 권장

권장 방법:
- Backward Compatible 스키마 변경만 적용
- 기능 플래그로 새 기능 비활성화
```

---

## 🛠️ Part 4: 롤백 시나리오 실습

### 시나리오 1: 새 버전 배포 후 에러 발생

```bash
# 상황: 새 버전 배포 후 500 에러 급증

# 1. 에러 확인
kubectl logs -l app=myapp -n default --tail=100

# 2. 현재 이미지 확인
kubectl get deployment myapp -n default -o jsonpath='{.spec.template.spec.containers[0].image}'

# 3. 이전 버전으로 롤백
argocd app rollback myapp 1

# 4. 롤백 확인
kubectl get pods -n default -w
kubectl logs -l app=myapp -n default --tail=20

# 5. 서비스 정상 확인
curl http://myapp.default.svc/health
```

### 시나리오 2: 잘못된 ConfigMap 배포

```bash
# 상황: ConfigMap 오타로 앱 시작 실패

# 1. 이전 ConfigMap 확인
argocd app manifests myapp --revision 1 | grep -A 20 "kind: ConfigMap"

# 2. 롤백
argocd app rollback myapp 1

# 3. Git에서 수정 후 재배포
cd infra-repo
# 오타 수정
git add . && git commit -m "fix: correct config typo"
git push

# 4. 최신 버전으로 재동기화
argocd app sync myapp
```

### 시나리오 3: 긴급 롤백 (kubectl)

```bash
# ArgoCD 없이 kubectl로 긴급 롤백

# Deployment 롤백
kubectl rollout undo deployment/myapp -n default

# 특정 Revision으로 롤백
kubectl rollout undo deployment/myapp -n default --to-revision=2

# 상태 확인
kubectl rollout status deployment/myapp -n default

# ⚠️ 주의: ArgoCD와 불일치 발생
# ArgoCD에서 OutOfSync 상태가 됨
# Auto Sync가 다시 최신으로 덮어쓸 수 있음
```

---

## 📊 롤백 방법 비교

| 방법 | 속도 | Git 반영 | Auto Sync 영향 | 권장 상황 |
|------|------|---------|---------------|----------|
| `argocd app rollback` | 빠름 | ❌ | 재동기화됨 | 긴급, 임시 |
| `git revert + push` | 중간 | ✅ | 안전 | **프로덕션 권장** |
| `argocd app sync --revision` | 빠름 | ❌ | 재동기화됨 | 특정 버전 테스트 |
| `kubectl rollout undo` | 매우 빠름 | ❌ | 재동기화됨 | 긴급, ArgoCD 없이 |

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 배포 히스토리 조회 | ☐ |
| 2 | argocd app rollback 실행 | ☐ |
| 3 | Git revision으로 Sync | ☐ |
| 4 | Git revert 롤백 (권장) | ☐ |
| 5 | Auto Sync와 롤백 관계 이해 | ☐ |
| 6 | 긴급 롤백 시나리오 실습 | ☐ |

---

## 🔑 핵심 명령어

```bash
# 히스토리
argocd app history myapp
argocd app diff myapp --revision 1

# 롤백
argocd app rollback myapp <ID>
argocd app sync myapp --revision <commit>

# Auto Sync 제어
argocd app set myapp --sync-policy none
argocd app set myapp --sync-policy automated

# kubectl 롤백 (긴급)
kubectl rollout undo deployment/myapp -n default
```

---

## ➡️ 다음 학습: Day 95

**주제**: ArgoCD App of Apps 패턴

