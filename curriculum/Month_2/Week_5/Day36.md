# 📅 Day 36: kubectl 고급 명령어

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 효율적인 kubectl 사용으로 운영 생산성을 높입니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 출력 포맷 | 1시간 | JSON, YAML, jsonpath |
| 필터링 | 1시간 | 라벨, 필드 셀렉터 |
| 리소스 관리 | 1시간 | dry-run, patch, diff |

---

## 📚 Part 1: 출력 포맷 제어

### 기본 출력 형식

| 옵션 | 설명 | 용도 |
|------|------|------|
| `-o wide` | 추가 정보 표시 | Node, IP 등 |
| `-o json` | JSON 출력 | 스크립트 파싱 |
| `-o yaml` | YAML 출력 | 설정 백업 |
| `-o name` | 이름만 출력 | 파이프라인 |
| `-o jsonpath` | 필드 추출 | 특정 값 추출 |
| `-o custom-columns` | 커스텀 테이블 | 대시보드 |

### 실습

```bash
# JSON 출력
kubectl get pod nginx -o json

# YAML 출력 (백업용)
kubectl get deployment myapp -o yaml > myapp-backup.yaml

# wide (노드, IP 포함)
kubectl get pods -o wide

# 이름만 출력
kubectl get pods -o name
# pod/nginx
# pod/api

# 여러 리소스 타입
kubectl get pods,svc,deployments -o wide
```

---

## 📚 Part 2: JSONPath 활용

### 기본 문법

```
{.items[*].metadata.name}    # 모든 항목의 이름
{.items[0].spec.containers}  # 첫 번째 항목의 컨테이너
{range .items[*]}{...}{end}  # 반복
```

### 실습

```bash
# 모든 Pod 이름
kubectl get pods -o jsonpath='{.items[*].metadata.name}'

# Pod별 이름과 상태 (줄바꿈 포함)
kubectl get pods -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.phase}{"\n"}{end}'

# 특정 필드만 추출
kubectl get pod nginx -o jsonpath='{.spec.containers[0].image}'

# 노드별 IP
kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.addresses[?(@.type=="InternalIP")].address}{"\n"}{end}'

# ConfigMap의 특정 키 값
kubectl get configmap myconfig -o jsonpath='{.data.app\.properties}'
```

### 자주 사용하는 JSONPath 예시

```bash
# Pod의 모든 컨테이너 이미지
kubectl get pods -o jsonpath='{.items[*].spec.containers[*].image}'

# 특정 라벨 값
kubectl get pods -o jsonpath='{.items[?(@.metadata.labels.app=="nginx")].metadata.name}'

# Ready 상태인 Pod만
kubectl get pods -o jsonpath='{.items[?(@.status.phase=="Running")].metadata.name}'

# 노드별 할당 가능 메모리
kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{": "}{.status.allocatable.memory}{"\n"}{end}'
```

---

## 📚 Part 3: Custom Columns

### 기본 사용법

```bash
kubectl get pods -o custom-columns=NAME:.metadata.name,STATUS:.status.phase,NODE:.spec.nodeName
```

### 실습

```bash
# Pod 상세 정보
kubectl get pods -o custom-columns=\
NAME:.metadata.name,\
STATUS:.status.phase,\
RESTARTS:.status.containerStatuses[0].restartCount,\
NODE:.spec.nodeName

# Deployment 정보
kubectl get deployments -o custom-columns=\
NAME:.metadata.name,\
REPLICAS:.spec.replicas,\
AVAILABLE:.status.availableReplicas,\
IMAGE:.spec.template.spec.containers[0].image

# Service 엔드포인트
kubectl get svc -o custom-columns=\
NAME:.metadata.name,\
TYPE:.spec.type,\
CLUSTER-IP:.spec.clusterIP,\
PORTS:.spec.ports[*].port
```

### 파일로 정의

```bash
# columns.txt
NAME          NAMESPACE          STATUS
metadata.name metadata.namespace status.phase

# 사용
kubectl get pods -o custom-columns-file=columns.txt
```

---

## 📚 Part 4: 필터링 및 검색

### 라벨 셀렉터

```bash
# 라벨로 필터
kubectl get pods -l app=nginx
kubectl get pods -l 'app in (nginx, api)'
kubectl get pods -l 'app!=nginx'
kubectl get pods -l 'app,env=prod'  # AND 조건

# 라벨 확인
kubectl get pods --show-labels

# 특정 라벨 컬럼 추가
kubectl get pods -L app,env
```

### 필드 셀렉터

```bash
# 상태별 필터
kubectl get pods --field-selector status.phase=Running
kubectl get pods --field-selector status.phase!=Running

# 노드별 필터
kubectl get pods --field-selector spec.nodeName=node01

# 메타데이터 필터
kubectl get pods --field-selector metadata.name=nginx
kubectl get pods --field-selector metadata.namespace=default

# 복합 필터
kubectl get pods --field-selector status.phase=Running,spec.restartPolicy=Always
```

### 모든 네임스페이스

```bash
kubectl get pods --all-namespaces
kubectl get pods -A

# 특정 네임스페이스들만
kubectl get pods -n ns1 & kubectl get pods -n ns2
```

---

## 📚 Part 5: 리소스 관리 명령어

### dry-run (테스트)

```bash
# 생성 테스트 (YAML 출력)
kubectl create deployment myapp --image=nginx --dry-run=client -o yaml

# 적용 테스트
kubectl apply -f deployment.yaml --dry-run=client

# 서버사이드 검증
kubectl apply -f deployment.yaml --dry-run=server
```

### diff (변경사항 확인)

```bash
# 적용 전 변경사항 미리보기
kubectl diff -f deployment.yaml
```

### patch (부분 수정)

```bash
# JSON Patch
kubectl patch deployment myapp -p '{"spec":{"replicas":3}}'

# 전략적 머지 패치
kubectl patch deployment myapp --type=merge -p '{"spec":{"template":{"spec":{"containers":[{"name":"app","image":"nginx:1.21"}]}}}}'

# JSON Patch (배열 조작)
kubectl patch deployment myapp --type=json -p '[{"op":"replace","path":"/spec/replicas","value":5}]'
```

### 라벨/어노테이션 관리

```bash
# 라벨 추가
kubectl label pod nginx env=dev

# 라벨 변경 (덮어쓰기)
kubectl label pod nginx env=prod --overwrite

# 라벨 삭제
kubectl label pod nginx env-

# 어노테이션 추가
kubectl annotate pod nginx description="Test pod"
kubectl annotate deployment myapp kubernetes.io/change-cause="Image updated to v2"
```

### 강제 삭제

```bash
# Grace Period 0 (즉시 삭제)
kubectl delete pod nginx --force --grace-period=0

# Finalizer 제거 후 삭제
kubectl patch pod stuck-pod -p '{"metadata":{"finalizers":null}}'
kubectl delete pod stuck-pod
```

---

## 📚 Part 6: 유용한 명령어

### API 리소스

```bash
# 지원 리소스 목록
kubectl api-resources

# 특정 API 그룹
kubectl api-resources --api-group=apps

# Namespaced 리소스만
kubectl api-resources --namespaced=true

# 특정 verb 지원 리소스
kubectl api-resources --verbs=list,get
```

### 설명 및 문서

```bash
# 리소스 설명
kubectl explain pod
kubectl explain pod.spec.containers
kubectl explain deployment.spec.strategy

# 재귀적 설명
kubectl explain pod --recursive
```

### 리소스 사용량

```bash
# 노드 사용량
kubectl top nodes

# Pod 사용량
kubectl top pods
kubectl top pods -A --sort-by=memory
kubectl top pods --containers

# 특정 Pod
kubectl top pod nginx
```

---

## ⚡ 생산성 팁

### 별칭 설정 (~/.bashrc 또는 ~/.zshrc)

```bash
# 기본 별칭
alias k='kubectl'
alias kgp='kubectl get pods'
alias kgpw='kubectl get pods -o wide'
alias kgpa='kubectl get pods -A'
alias kdp='kubectl describe pod'
alias kl='kubectl logs -f'
alias kex='kubectl exec -it'

# 네임스페이스 별칭
alias kn='kubectl config set-context --current --namespace'

# 자동완성 (bash)
source <(kubectl completion bash)
alias k='kubectl'
complete -F __start_kubectl k

# 자동완성 (zsh)
source <(kubectl completion zsh)
```

### 자주 쓰는 조합

```bash
# Pod 이름만 추출하여 반복
for pod in $(kubectl get pods -o name); do
  kubectl describe $pod | grep -i error
done

# 특정 상태 Pod 삭제
kubectl delete pods --field-selector status.phase=Failed

# 모든 Deployment 재시작
kubectl rollout restart deployment --all
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어 | 완료 |
|---|------|--------|------|
| 1 | JSON/YAML 출력 | `-o json/yaml` | ☐ |
| 2 | JSONPath 필드 추출 | `-o jsonpath='{...}'` | ☐ |
| 3 | Custom Columns | `-o custom-columns=...` | ☐ |
| 4 | 라벨 필터링 | `-l app=nginx` | ☐ |
| 5 | 필드 셀렉터 | `--field-selector` | ☐ |
| 6 | dry-run 테스트 | `--dry-run=client -o yaml` | ☐ |
| 7 | diff 확인 | `kubectl diff -f` | ☐ |
| 8 | patch 수정 | `kubectl patch` | ☐ |
| 9 | 리소스 사용량 | `kubectl top` | ☐ |
| 10 | API 리소스 확인 | `kubectl api-resources` | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 출력 포맷
kubectl get pods -o wide/json/yaml/name
kubectl get pods -o jsonpath='{.items[*].metadata.name}'
kubectl get pods -o custom-columns=NAME:.metadata.name,STATUS:.status.phase

# 필터링
kubectl get pods -l app=nginx
kubectl get pods --field-selector status.phase=Running
kubectl get pods -A

# 리소스 관리
kubectl create deployment myapp --image=nginx --dry-run=client -o yaml
kubectl diff -f deployment.yaml
kubectl patch deployment myapp -p '{"spec":{"replicas":3}}'
kubectl label/annotate pod nginx key=value

# 정보 확인
kubectl api-resources
kubectl explain pod.spec
kubectl top pods/nodes
```

---

## ➡️ 다음 학습: Day 37

**주제**: Week 5 복습

