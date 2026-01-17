# 📅 Day 38: ConfigMap - 설정 관리

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 애플리케이션 설정을 ConfigMap으로 분리하여 환경별 배포를 유연하게 관리합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 30분 | ConfigMap 이해 |
| 생성 방법 | 1시간 | 다양한 생성 방식 |
| 사용 방법 | 1시간 | 환경변수, 볼륨 |
| 운영 | 30분 | 업데이트, 베스트 프랙티스 |

---

## 📚 Part 1: ConfigMap 개념

### ConfigMap이란?

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     ConfigMap 개념                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   컨테이너 이미지                    ConfigMap                               │
│   ┌─────────────────────┐          ┌─────────────────────┐                  │
│   │ • 애플리케이션 코드  │          │ • 환경 변수         │                  │
│   │ • 라이브러리        │   분리   │ • 설정 파일         │                  │
│   │ • 런타임           │ ◀──────▶ │ • 명령줄 인수       │                  │
│   └─────────────────────┘          └─────────────────────┘                  │
│                                                                              │
│   장점:                                                                      │
│   • 이미지 재빌드 없이 설정 변경                                              │
│   • 환경별 (Dev, Staging, Prod) 다른 설정 사용                               │
│   • Git으로 설정 버전 관리                                                    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### ConfigMap vs Secret

| 특징 | ConfigMap | Secret |
|------|-----------|--------|
| 용도 | 일반 설정 | 민감한 데이터 |
| 인코딩 | 평문 | Base64 |
| 크기 제한 | 1MB | 1MB |
| etcd 저장 | 평문 | 암호화 가능 |

---

## 🛠️ Part 2: ConfigMap 생성 방법

### 방법 1: 명령어 (--from-literal)

```bash
# 개별 키-값
kubectl create configmap app-config \
  --from-literal=APP_ENV=production \
  --from-literal=LOG_LEVEL=info \
  --from-literal=DB_HOST=mysql.default.svc.cluster.local

# 확인
kubectl get configmap app-config -o yaml
```

### 방법 2: 파일에서 생성 (--from-file)

```bash
# 설정 파일 준비
cat << 'EOF' > app.properties
database.url=jdbc:mysql://mysql:3306/mydb
database.pool.size=10
cache.enabled=true
EOF

# ConfigMap 생성
kubectl create configmap app-config --from-file=app.properties

# 특정 키 이름으로 생성
kubectl create configmap app-config --from-file=config.properties=app.properties

# 디렉토리 전체
kubectl create configmap app-config --from-file=./config/
```

### 방법 3: YAML 매니페스트

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: default
data:
  # 단순 키-값
  APP_ENV: production
  LOG_LEVEL: info
  
  # 설정 파일 내용
  app.properties: |
    database.url=jdbc:mysql://mysql:3306/mydb
    database.pool.size=10
    cache.enabled=true
  
  # JSON 설정
  config.json: |
    {
      "server": {
        "port": 8080,
        "host": "0.0.0.0"
      },
      "logging": {
        "level": "INFO"
      }
    }
  
  # nginx 설정
  nginx.conf: |
    server {
        listen 80;
        location / {
            proxy_pass http://backend:8080;
        }
    }
```

### 방법 4: Kustomize와 함께

```yaml
# kustomization.yaml
configMapGenerator:
- name: app-config
  literals:
  - APP_ENV=production
  - LOG_LEVEL=info
- name: nginx-config
  files:
  - nginx.conf
```

---

## 🛠️ Part 3: ConfigMap 사용 방법

### 방법 1: 환경 변수로 사용

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    
    # 전체 ConfigMap을 환경 변수로
    envFrom:
    - configMapRef:
        name: app-config
    
    # 특정 키만 환경 변수로
    env:
    - name: DATABASE_URL
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: database.url
    - name: CACHE_ENABLED
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: cache.enabled
          optional: true  # 키가 없어도 Pod 시작
```

### 방법 2: 볼륨으로 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
spec:
  containers:
  - name: nginx
    image: nginx:1.21
    volumeMounts:
    - name: config
      mountPath: /etc/nginx/conf.d
      readOnly: true
  volumes:
  - name: config
    configMap:
      name: nginx-config
```

### 방법 3: 특정 키만 파일로 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: config
      mountPath: /app/config
      readOnly: true
  volumes:
  - name: config
    configMap:
      name: app-config
      items:
      - key: app.properties
        path: application.properties  # 마운트될 파일명
      - key: config.json
        path: config.json
```

### 방법 4: subPath로 기존 디렉토리에 추가

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    # 기존 /etc 디렉토리를 덮어쓰지 않고 파일만 추가
    - name: config
      mountPath: /etc/myapp.conf
      subPath: myapp.conf
  volumes:
  - name: config
    configMap:
      name: app-config
```

---

## 🛠️ Part 4: 환경별 ConfigMap 관리

### Kustomize Overlay 활용

```
config/
├── base/
│   ├── kustomization.yaml
│   └── configmap.yaml
└── overlays/
    ├── dev/
    │   ├── kustomization.yaml
    │   └── configmap-patch.yaml
    ├── staging/
    └── prod/
```

```yaml
# base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  LOG_LEVEL: info
  CACHE_TTL: "300"

# overlays/prod/configmap-patch.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  LOG_LEVEL: warn      # 프로덕션은 warn
  CACHE_TTL: "3600"    # 캐시 더 오래
  REPLICAS: "5"        # 추가 설정
```

---

## 🛠️ Part 5: ConfigMap 업데이트

### 자동 업데이트 (볼륨 마운트)

```bash
# ConfigMap 수정
kubectl edit configmap app-config

# 또는 patch
kubectl patch configmap app-config --type merge -p '{"data":{"LOG_LEVEL":"debug"}}'

# 볼륨으로 마운트된 ConfigMap은 약 1분 내에 자동 업데이트
# (kubelet의 syncFrequency에 따라)
```

### 수동 롤아웃 (환경 변수 사용 시)

```bash
# 환경 변수로 사용하는 경우 Pod 재시작 필요
kubectl rollout restart deployment myapp

# 또는 ConfigMap 해시를 Pod 템플릿에 추가하여 자동 롤아웃
```

### ConfigMap 해시로 자동 롤아웃

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    metadata:
      annotations:
        # ConfigMap 변경 시 자동으로 해시가 변경되어 롤아웃 트리거
        checksum/config: "{{ sha256sum .Values.configData }}"
    spec:
      containers:
      - name: app
        envFrom:
        - configMapRef:
            name: app-config
```

---

## 📋 트러블슈팅

### ConfigMap이 마운트되지 않을 때

```bash
# ConfigMap 존재 확인
kubectl get configmap app-config

# Pod 이벤트 확인
kubectl describe pod <pod-name>

# 볼륨 마운트 확인
kubectl exec <pod-name> -- ls -la /path/to/config
```

### 환경 변수가 적용되지 않을 때

```bash
# Pod 환경 변수 확인
kubectl exec <pod-name> -- env | grep APP

# ConfigMap 키 확인
kubectl get configmap app-config -o jsonpath='{.data}'
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어/방법 | 완료 |
|---|------|-----------|------|
| 1 | ConfigMap 생성 (literal) | `--from-literal` | ☐ |
| 2 | ConfigMap 생성 (file) | `--from-file` | ☐ |
| 3 | ConfigMap 생성 (YAML) | `kubectl apply -f` | ☐ |
| 4 | 환경 변수로 사용 | `envFrom`, `env.valueFrom` | ☐ |
| 5 | 볼륨 마운트 | `volumes.configMap` | ☐ |
| 6 | 특정 키만 마운트 | `items` | ☐ |
| 7 | ConfigMap 업데이트 | `kubectl patch/edit` | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 생성
kubectl create configmap app-config --from-literal=KEY=value
kubectl create configmap app-config --from-file=config.properties
kubectl create configmap app-config --from-file=./config/

# 확인
kubectl get configmap
kubectl describe configmap app-config
kubectl get configmap app-config -o yaml

# 수정
kubectl edit configmap app-config
kubectl patch configmap app-config --type merge -p '{"data":{"KEY":"new-value"}}'

# 삭제
kubectl delete configmap app-config
```

---

## ➡️ 다음 학습: Day 39

**주제**: Secret - 민감 데이터 관리

