# 📅 Day 39: Secret - 민감 데이터 관리

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 민감한 정보를 안전하게 관리하여 보안 컴플라이언스를 준수합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 30분 | Secret 이해 |
| 생성/사용 | 1시간 | 다양한 방법 |
| 타입 | 1시간 | TLS, Docker Registry |
| 보안 | 30분 | 베스트 프랙티스 |

---

## 📚 Part 1: Secret 개념

### Secret vs ConfigMap

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Secret vs ConfigMap                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ConfigMap                          Secret                                  │
│   ┌─────────────────────────┐       ┌─────────────────────────┐             │
│   │ • 일반 설정             │       │ • 민감 데이터           │             │
│   │ • 평문 저장             │       │ • Base64 인코딩         │             │
│   │ • 앱 설정, 환경 변수    │       │ • 비밀번호, API 키      │             │
│   └─────────────────────────┘       └─────────────────────────┘             │
│                                                                              │
│   ⚠️ 주의: Base64는 암호화가 아님!                                          │
│   → etcd 암호화 또는 Vault 사용 권장                                         │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Secret 타입

| 타입 | 용도 | 자동 생성 키 |
|------|------|------------|
| `Opaque` | 일반 (기본값) | 사용자 정의 |
| `kubernetes.io/tls` | TLS 인증서 | tls.crt, tls.key |
| `kubernetes.io/dockerconfigjson` | Docker Registry 인증 | .dockerconfigjson |
| `kubernetes.io/basic-auth` | 기본 인증 | username, password |
| `kubernetes.io/ssh-auth` | SSH 키 | ssh-privatekey |
| `kubernetes.io/service-account-token` | SA 토큰 | 자동 생성 |

---

## 🛠️ Part 2: Secret 생성

### 방법 1: 명령어 (--from-literal)

```bash
# 개별 키-값
kubectl create secret generic db-secret \
  --from-literal=username=admin \
  --from-literal=password=s3cr3tP@ssw0rd!

# 확인 (Base64 인코딩됨)
kubectl get secret db-secret -o yaml

# 디코딩
kubectl get secret db-secret -o jsonpath='{.data.password}' | base64 -d
```

### 방법 2: 파일에서 생성 (--from-file)

```bash
# 인증서 파일
kubectl create secret generic api-key \
  --from-file=api-key=./api-key.txt

# 여러 파일
kubectl create secret generic certs \
  --from-file=cert.pem \
  --from-file=key.pem
```

### 방법 3: YAML 매니페스트

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
  namespace: default
type: Opaque
data:
  # Base64 인코딩 필요: echo -n 'admin' | base64
  username: YWRtaW4=
  password: czNjcjN0UEBzc3cwcmQh
---
# stringData는 평문으로 작성 가능 (저장 시 자동 인코딩)
apiVersion: v1
kind: Secret
metadata:
  name: db-secret-2
type: Opaque
stringData:
  username: admin
  password: s3cr3tP@ssw0rd!
```

---

## 🛠️ Part 3: Secret 사용

### 방법 1: 환경 변수로 주입

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    
    # 전체 Secret을 환경 변수로
    envFrom:
    - secretRef:
        name: db-secret
    
    # 특정 키만 환경 변수로
    env:
    - name: DATABASE_USER
      valueFrom:
        secretKeyRef:
          name: db-secret
          key: username
    - name: DATABASE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-secret
          key: password
          optional: false  # Secret이 없으면 Pod 시작 실패
```

### 방법 2: 볼륨으로 마운트

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
    - name: secret-volume
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secret-volume
    secret:
      secretName: db-secret
      defaultMode: 0400  # 파일 권한 설정
```

### 방법 3: 특정 키만 마운트

```yaml
volumes:
- name: secret-volume
  secret:
    secretName: db-secret
    items:
    - key: username
      path: db-user.txt      # 마운트될 파일명
    - key: password
      path: db-pass.txt
```

---

## 🛠️ Part 4: 특수 타입 Secret

### TLS Secret

```bash
# TLS Secret 생성
kubectl create secret tls my-tls \
  --cert=path/to/cert.pem \
  --key=path/to/key.pem

# Ingress에서 사용
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-ingress
spec:
  tls:
  - hosts:
    - myapp.example.com
    secretName: my-tls
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 80
EOF
```

### Docker Registry Secret

```bash
# Docker Registry 인증
kubectl create secret docker-registry regcred \
  --docker-server=registry.example.com \
  --docker-username=admin \
  --docker-password=secret \
  --docker-email=admin@example.com

# Pod에서 사용
apiVersion: v1
kind: Pod
metadata:
  name: private-app
spec:
  imagePullSecrets:
  - name: regcred
  containers:
  - name: app
    image: registry.example.com/myapp:1.0
```

### ServiceAccount에 연결

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: myapp-sa
imagePullSecrets:
- name: regcred
```

---

## 🛠️ Part 5: 보안 베스트 프랙티스

### Secret 보안 강화

```yaml
# 1. etcd 암호화 설정 (kube-apiserver)
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
- resources:
  - secrets
  providers:
  - aescbc:
      keys:
      - name: key1
        secret: <base64-encoded-32-byte-key>
  - identity: {}
```

### External Secrets 사용 (권장)

```yaml
# Vault와 연동
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    kind: SecretStore
    name: vault-backend
  target:
    name: db-secret
  data:
  - secretKey: password
    remoteRef:
      key: secret/data/database
      property: password
```

### RBAC로 Secret 접근 제한

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
  namespace: production
rules:
- apiGroups: [""]
  resources: ["secrets"]
  resourceNames: ["db-secret"]  # 특정 Secret만
  verbs: ["get"]
```

---

## 📋 트러블슈팅

### Secret이 적용되지 않을 때

```bash
# Secret 존재 확인
kubectl get secret db-secret

# Secret 내용 확인 (디코딩)
kubectl get secret db-secret -o jsonpath='{.data.password}' | base64 -d

# Pod 환경 변수 확인
kubectl exec <pod> -- env | grep DATABASE

# 볼륨 마운트 확인
kubectl exec <pod> -- cat /etc/secrets/password
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어/방법 | 완료 |
|---|------|-----------|------|
| 1 | Secret 생성 (literal) | `--from-literal` | ☐ |
| 2 | Secret 생성 (file) | `--from-file` | ☐ |
| 3 | Secret 생성 (YAML) | `stringData` | ☐ |
| 4 | 환경 변수로 사용 | `secretKeyRef` | ☐ |
| 5 | 볼륨으로 마운트 | `volumes.secret` | ☐ |
| 6 | TLS Secret | `kubectl create secret tls` | ☐ |
| 7 | Docker Registry | `kubectl create secret docker-registry` | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 생성
kubectl create secret generic db-secret --from-literal=username=admin --from-literal=password=secret
kubectl create secret tls my-tls --cert=cert.pem --key=key.pem
kubectl create secret docker-registry regcred --docker-server=... --docker-username=...

# 확인
kubectl get secret db-secret -o yaml
kubectl get secret db-secret -o jsonpath='{.data.password}' | base64 -d

# 삭제
kubectl delete secret db-secret
```

---

## ➡️ 다음 학습: Day 40

**주제**: Service (ClusterIP, NodePort)

