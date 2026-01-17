# 📅 Day 57: ConfigMap과 Secret - 설정과 민감 정보 분리

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 인프라 설계/운영"

애플리케이션 설정과 민감 정보를 코드에서 분리하여 안전하게 관리합니다.

---

## ⏰ 예상 소요 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | ConfigMap/Secret 이해 |
| 실습 | 1.5시간 | 다양한 사용 패턴 |
| 심화 | 45분 | 보안 베스트 프랙티스 |

---

## 📚 Part 1: 개념 이해 (45분)

### 왜 설정을 분리해야 하나?

```
┌─────────────────────────────────────────────────────────────┐
│  설정 분리의 필요성                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  잘못된 방식 ❌                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ # 코드에 하드코딩                               │       │
│  │ DB_HOST = "prod-db.toss.im"                     │       │
│  │ DB_PASSWORD = "supersecret123"  ← 보안 위험!    │       │
│  │                                                  │       │
│  │ 문제점:                                          │       │
│  │ - 환경별로 코드 수정 필요                        │       │
│  │ - Git에 비밀번호 노출                            │       │
│  │ - 설정 변경 시 재빌드 필요                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  올바른 방식 ✅                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ # 환경 변수에서 읽기                            │       │
│  │ DB_HOST = os.getenv("DB_HOST")                  │       │
│  │ DB_PASSWORD = os.getenv("DB_PASSWORD")          │       │
│  │                                                  │       │
│  │ Kubernetes에서:                                  │       │
│  │ - ConfigMap: 일반 설정                          │       │
│  │ - Secret: 민감 정보 (암호화 저장)               │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### ConfigMap vs Secret

| 구분 | ConfigMap | Secret |
|------|-----------|--------|
| **용도** | 일반 설정 | 민감 정보 |
| **저장** | 평문 | Base64 인코딩 |
| **암호화** | 없음 | etcd 암호화 가능 |
| **예시** | 로그 레벨, URL | 비밀번호, API 키, 인증서 |
| **RBAC** | 일반 | 엄격히 제한 권장 |

### 사용 방식

```
┌─────────────────────────────────────────────────────────────┐
│  ConfigMap/Secret 사용 방식                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 환경 변수로 주입                                        │
│  ┌─────────────────────────────────────────────────┐       │
│  │ env:                                            │       │
│  │ - name: DB_HOST                                 │       │
│  │   valueFrom:                                    │       │
│  │     configMapKeyRef:                            │       │
│  │       name: app-config                          │       │
│  │       key: db_host                              │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. 전체 ConfigMap을 환경 변수로                            │
│  ┌─────────────────────────────────────────────────┐       │
│  │ envFrom:                                        │       │
│  │ - configMapRef:                                 │       │
│  │     name: app-config                            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. 볼륨으로 마운트 (파일)                                  │
│  ┌─────────────────────────────────────────────────┐       │
│  │ volumeMounts:                                   │       │
│  │ - name: config-volume                           │       │
│  │   mountPath: /etc/config                        │       │
│  │ volumes:                                        │       │
│  │ - name: config-volume                           │       │
│  │   configMap:                                    │       │
│  │     name: app-config                            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: ConfigMap 생성

```bash
# 방법 1: 명령형으로 생성
kubectl create configmap app-config \
  --from-literal=APP_ENV=production \
  --from-literal=LOG_LEVEL=info \
  --from-literal=REDIS_HOST=redis

# 방법 2: 파일에서 생성
echo "server {
    listen 80;
    location / {
        proxy_pass http://backend:8080;
    }
}" > nginx.conf

kubectl create configmap nginx-config --from-file=nginx.conf

# 방법 3: 선언형 YAML
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: k8s-portfolio
data:
  # 단순 키-값
  APP_ENV: "production"
  LOG_LEVEL: "info"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  
  # 멀티라인 설정 파일
  application.yaml: |
    server:
      port: 8080
    logging:
      level: INFO
    redis:
      host: redis
      port: 6379
EOF

# 확인
kubectl get configmap
kubectl describe configmap app-config
kubectl get configmap app-config -o yaml
```

### 실습 2: Secret 생성

```bash
# 방법 1: 명령형으로 생성
kubectl create secret generic app-secret \
  --from-literal=DB_PASSWORD=mysecretpassword \
  --from-literal=API_KEY=api-key-12345

# 방법 2: 파일에서 생성
echo -n 'admin' > username.txt
echo -n 'supersecret' > password.txt
kubectl create secret generic db-credentials \
  --from-file=username=username.txt \
  --from-file=password=password.txt

# 방법 3: 선언형 YAML (base64 인코딩 필요)
# echo -n 'mysecretpassword' | base64
# bXlzZWNyZXRwYXNzd29yZA==

kubectl apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: k8s-portfolio
type: Opaque
data:
  DB_PASSWORD: bXlzZWNyZXRwYXNzd29yZA==
  API_KEY: YXBpLWtleS0xMjM0NQ==
EOF

# 확인 (base64 디코딩)
kubectl get secret app-secret -o jsonpath='{.data.DB_PASSWORD}' | base64 -d
```

### 실습 3: Pod에서 환경 변수로 사용

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: config-demo
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["sh", "-c", "env | sort && sleep 3600"]
    
    # 개별 키 참조
    env:
    - name: DATABASE_HOST
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: REDIS_HOST
    - name: DATABASE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: app-secret
          key: DB_PASSWORD
    
    # ConfigMap 전체를 환경 변수로
    envFrom:
    - configMapRef:
        name: app-config
        prefix: CONFIG_  # 선택: 접두사 추가
EOF

# 환경 변수 확인
kubectl logs config-demo | grep -E "DATABASE_|CONFIG_|APP_|LOG_"
```

### 실습 4: 볼륨으로 마운트

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: config-volume-demo
spec:
  containers:
  - name: app
    image: nginx:1.24
    volumeMounts:
    # ConfigMap을 디렉토리로 마운트
    - name: config-volume
      mountPath: /etc/config
      readOnly: true
    # Secret을 디렉토리로 마운트
    - name: secret-volume
      mountPath: /etc/secrets
      readOnly: true
    # 특정 키만 파일로 마운트
    - name: nginx-config
      mountPath: /etc/nginx/conf.d
  volumes:
  - name: config-volume
    configMap:
      name: app-config
  - name: secret-volume
    secret:
      secretName: app-secret
      defaultMode: 0400  # 파일 권한 설정
  - name: nginx-config
    configMap:
      name: nginx-config
      items:
      - key: nginx.conf
        path: default.conf
EOF

# 마운트된 파일 확인
kubectl exec config-volume-demo -- ls -la /etc/config/
kubectl exec config-volume-demo -- cat /etc/config/APP_ENV
kubectl exec config-volume-demo -- ls -la /etc/secrets/
kubectl exec config-volume-demo -- cat /etc/nginx/conf.d/default.conf
```

### 실습 5: Secret 유형

```bash
# Docker Registry Secret
kubectl create secret docker-registry my-registry-secret \
  --docker-server=registry.example.com \
  --docker-username=user \
  --docker-password=password

# TLS Secret
kubectl create secret tls my-tls-secret \
  --cert=tls.crt \
  --key=tls.key

# 기본 인증
kubectl create secret generic basic-auth \
  --from-literal=username=admin \
  --from-literal=password=secret \
  --type=kubernetes.io/basic-auth

# SSH 키
kubectl create secret generic ssh-key \
  --from-file=ssh-privatekey=~/.ssh/id_rsa \
  --type=kubernetes.io/ssh-auth
```

---

## 📚 Part 3: 보안 베스트 프랙티스 (45분)

### Secret 보안 강화

```yaml
# etcd 암호화 설정 (EncryptionConfiguration)
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
  - resources:
    - secrets
    providers:
    - aescbc:
        keys:
        - name: key1
          secret: <base64-encoded-secret>
    - identity: {}
```

### 외부 Secret 관리 (Vault 연동 예시)

```yaml
# External Secrets Operator 사용
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: vault-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: app-secret
  data:
  - secretKey: DB_PASSWORD
    remoteRef:
      key: secret/data/myapp
      property: password
```

### RBAC으로 Secret 접근 제한

```yaml
# Secret 읽기 전용 Role
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
  namespace: k8s-portfolio
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get"]
  resourceNames: ["app-secret"]  # 특정 Secret만 허용
```

### 토스플레이스 패턴

```yaml
# 결제 서비스 설정 분리
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-config
  namespace: payment
data:
  APP_NAME: "payment-service"
  LOG_FORMAT: "json"
  REDIS_HOST: "redis.payment.svc.cluster.local"
  KAFKA_BROKERS: "kafka-0:9092,kafka-1:9092"
---
apiVersion: v1
kind: Secret
metadata:
  name: payment-secret
  namespace: payment
type: Opaque
data:
  DB_PASSWORD: <base64>
  ENCRYPTION_KEY: <base64>
  JWT_SECRET: <base64>
---
# Deployment에서 사용
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-api
spec:
  template:
    spec:
      containers:
      - name: api
        envFrom:
        - configMapRef:
            name: payment-config
        - secretRef:
            name: payment-secret
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | ConfigMap 생성 (명령형/선언형) | ☐ |
| 2 | Secret 생성 (base64 인코딩) | ☐ |
| 3 | 환경 변수로 주입 | ☐ |
| 4 | envFrom으로 전체 주입 | ☐ |
| 5 | 볼륨으로 파일 마운트 | ☐ |
| 6 | Secret 보안 고려사항 이해 | ☐ |

---

## 🔑 핵심 명령어

```bash
# ConfigMap
kubectl create configmap <name> --from-literal=key=value
kubectl create configmap <name> --from-file=filename
kubectl get configmap <name> -o yaml

# Secret
kubectl create secret generic <name> --from-literal=key=value
kubectl get secret <name> -o jsonpath='{.data.key}' | base64 -d
```

---

## 📝 면접 대비 질문

### Q1: ConfigMap과 Secret의 차이점은?
> "둘 다 설정을 Pod에 주입하지만, Secret은 민감 정보용으로 base64 인코딩되어 저장되고, etcd 암호화를 적용할 수 있습니다. 또한 Secret은 tmpfs에 마운트되어 디스크에 기록되지 않습니다. RBAC으로 접근을 엄격히 제한해야 합니다."

### Q2: Secret이 안전하지 않다는 말이 있는데?
> "base64는 인코딩일 뿐 암호화가 아닙니다. 하지만 Kubernetes는 etcd 암호화, RBAC 접근 제어, 감사 로깅을 지원합니다. 프로덕션에서는 HashiCorp Vault나 AWS Secrets Manager와 External Secrets Operator를 연동하여 더 강력한 보안을 구현합니다."

---

## ➡️ 다음 학습: Day 58

**주제**: Ingress, HPA 설정
