# 📅 Day 99: Helm Chart 작성 - 커스텀 애플리케이션 패키징

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "배포 자동화 파이프라인 운영, CI/CD 환경 개선"

커스텀 Helm Chart를 작성하여 애플리케이션을 효율적으로 패키징합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | Chart 구조 이해 |
| 실습 | 2시간 | 커스텀 Chart 작성 |
| 심화 | 1시간 | 고급 템플릿 기법 |
| 테스트 | 15분 | 검증 및 배포 |

---

## 📚 Part 1: Helm Chart 구조 (45분)

### Chart 디렉토리 구조

```
┌─────────────────────────────────────────────────────────────┐
│  Helm Chart Structure                                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  myapp/                                                     │
│  ├── Chart.yaml          # Chart 메타데이터               │
│  ├── Chart.lock          # 의존성 Lock 파일               │
│  ├── values.yaml         # 기본 설정 값                   │
│  ├── values.schema.json  # 값 검증 스키마 (선택)          │
│  ├── .helmignore         # 패키징 제외 파일               │
│  │                                                         │
│  ├── templates/          # Kubernetes 매니페스트 템플릿   │
│  │   ├── _helpers.tpl    # 재사용 템플릿 함수            │
│  │   ├── deployment.yaml                                  │
│  │   ├── service.yaml                                     │
│  │   ├── ingress.yaml                                     │
│  │   ├── configmap.yaml                                   │
│  │   ├── secret.yaml                                      │
│  │   ├── hpa.yaml                                         │
│  │   ├── serviceaccount.yaml                              │
│  │   ├── NOTES.txt       # 설치 후 표시 메시지           │
│  │   └── tests/          # Helm Test 정의                │
│  │       └── test-connection.yaml                         │
│  │                                                         │
│  └── charts/             # 의존성 Sub-charts              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 템플릿 문법 기초

```yaml
# 기본 문법
{{ .Values.key }}              # values.yaml에서 값 참조
{{ .Release.Name }}            # Release 이름
{{ .Chart.Name }}              # Chart 이름
{{ .Chart.Version }}           # Chart 버전
{{ .Namespace }}               # 배포 네임스페이스

# 조건문
{{ if .Values.enabled }}
...
{{ end }}

# 반복문
{{ range .Values.ports }}
- port: {{ .port }}
{{ end }}

# 파이프라인
{{ .Values.name | quote }}     # 따옴표 추가
{{ .Values.name | upper }}     # 대문자 변환
{{ .Values.name | default "myapp" }}  # 기본값

# 함수 호출
{{ include "myapp.fullname" . }}  # 헬퍼 함수 호출
```

---

## 🛠️ Part 2: 실습 - 커스텀 Chart 작성 (2시간)

### 실습 1: Chart 생성

```bash
# 새 Chart 생성
helm create myapp

# 생성된 파일 확인
ls -la myapp/
cat myapp/Chart.yaml
cat myapp/values.yaml

# 불필요한 파일 정리
cd myapp
rm -rf templates/tests
rm templates/ingress.yaml templates/serviceaccount.yaml
```

### 실습 2: Chart.yaml 작성

```yaml
# Chart.yaml
apiVersion: v2
name: myapp
description: A Helm chart for MyApp - Payment Service
type: application

# Chart 버전 (Chart 자체의 버전)
version: 0.1.0

# 애플리케이션 버전 (배포하는 앱의 버전)
appVersion: "1.0.0"

# 유지보수자 정보
maintainers:
  - name: DevOps Team
    email: devops@toss.im

# 키워드 (검색용)
keywords:
  - payment
  - api
  - microservice

# 홈페이지, 소스 저장소
home: https://github.com/toss/myapp
sources:
  - https://github.com/toss/myapp

# 의존성 (다른 Chart)
dependencies:
  - name: redis
    version: "17.x.x"
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
```

### 실습 3: values.yaml 작성

```yaml
# values.yaml
replicaCount: 2

image:
  repository: myapp
  pullPolicy: IfNotPresent
  tag: ""  # Chart.appVersion 사용

imagePullSecrets: []
nameOverride: ""
fullnameOverride: ""

# 서비스 계정
serviceAccount:
  create: true
  annotations: {}
  name: ""

# Pod 보안 설정
podSecurityContext:
  fsGroup: 1000

securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false

# 서비스 설정
service:
  type: ClusterIP
  port: 80
  targetPort: 8080

# Ingress 설정
ingress:
  enabled: false
  className: "nginx"
  annotations: {}
  hosts:
    - host: myapp.local
      paths:
        - path: /
          pathType: Prefix
  tls: []

# 리소스 설정
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 128Mi

# HPA 설정
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

# Probe 설정
livenessProbe:
  httpGet:
    path: /health
    port: http
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /ready
    port: http
  initialDelaySeconds: 5
  periodSeconds: 5
  failureThreshold: 3

# 환경 변수
env:
  - name: APP_ENV
    value: "production"

# ConfigMap 데이터
config:
  LOG_LEVEL: "info"
  REDIS_HOST: "redis"

# Secret 데이터 (실제로는 외부에서 주입)
secrets: {}

# 노드 선택
nodeSelector: {}

# Tolerations
tolerations: []

# Affinity
affinity: {}

# Redis 의존성
redis:
  enabled: true
  auth:
    enabled: false
  replica:
    replicaCount: 0
```

### 실습 4: _helpers.tpl 작성

```yaml
# templates/_helpers.tpl
{{/*
Chart 이름 확장
*/}}
{{- define "myapp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
전체 이름 생성 (release name 포함)
*/}}
{{- define "myapp.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Chart 버전/앱 버전 라벨
*/}}
{{- define "myapp.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
공통 라벨
*/}}
{{- define "myapp.labels" -}}
helm.sh/chart: {{ include "myapp.chart" . }}
{{ include "myapp.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector 라벨
*/}}
{{- define "myapp.selectorLabels" -}}
app.kubernetes.io/name: {{ include "myapp.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
ServiceAccount 이름
*/}}
{{- define "myapp.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "myapp.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
이미지 전체 경로
*/}}
{{- define "myapp.image" -}}
{{- $tag := default .Chart.AppVersion .Values.image.tag }}
{{- printf "%s:%s" .Values.image.repository $tag }}
{{- end }}
```

### 실습 5: deployment.yaml 작성

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      annotations:
        checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
      labels:
        {{- include "myapp.selectorLabels" . | nindent 8 }}
    spec:
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      serviceAccountName: {{ include "myapp.serviceAccountName" . }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
        - name: {{ .Chart.Name }}
          securityContext:
            {{- toYaml .Values.securityContext | nindent 12 }}
          image: {{ include "myapp.image" . }}
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - name: http
              containerPort: {{ .Values.service.targetPort }}
              protocol: TCP
          {{- with .Values.livenessProbe }}
          livenessProbe:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          {{- with .Values.readinessProbe }}
          readinessProbe:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          envFrom:
            - configMapRef:
                name: {{ include "myapp.fullname" . }}
          {{- with .Values.env }}
          env:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
```

### 실습 6: 기타 템플릿

```yaml
# templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  type: {{ .Values.service.type }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: http
      protocol: TCP
      name: http
  selector:
    {{- include "myapp.selectorLabels" . | nindent 4 }}
---
# templates/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
data:
  {{- range $key, $val := .Values.config }}
  {{ $key }}: {{ $val | quote }}
  {{- end }}
---
# templates/hpa.yaml
{{- if .Values.autoscaling.enabled }}
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ include "myapp.fullname" . }}
  minReplicas: {{ .Values.autoscaling.minReplicas }}
  maxReplicas: {{ .Values.autoscaling.maxReplicas }}
  metrics:
    {{- if .Values.autoscaling.targetCPUUtilizationPercentage }}
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
    {{- end }}
    {{- if .Values.autoscaling.targetMemoryUtilizationPercentage }}
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
    {{- end }}
{{- end }}
```

---

## 🛠️ Part 3: 테스트 및 배포 (15분)

### Chart 검증

```bash
# 문법 검사
helm lint ./myapp

# 템플릿 렌더링 테스트
helm template myapp ./myapp

# 특정 값으로 렌더링
helm template myapp ./myapp \
  --set replicaCount=3 \
  --set image.tag=v2.0.0

# 실제 배포 없이 시뮬레이션
helm install myapp ./myapp --dry-run --debug

# 설치
helm install myapp ./myapp -n default

# 확인
helm list
kubectl get all -l app.kubernetes.io/instance=myapp

# 업그레이드
helm upgrade myapp ./myapp --set image.tag=v2.0.0

# 삭제
helm uninstall myapp
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Chart 구조 이해 | ☐ |
| 2 | Chart.yaml 작성 | ☐ |
| 3 | values.yaml 작성 | ☐ |
| 4 | _helpers.tpl 작성 | ☐ |
| 5 | deployment.yaml 템플릿 작성 | ☐ |
| 6 | service, configmap, hpa 작성 | ☐ |
| 7 | helm lint 테스트 | ☐ |
| 8 | 로컬 배포 테스트 | ☐ |

---

## 📝 면접 대비 질문

### Q1: Helm Chart의 장점은 무엇인가요?
> "첫째, 템플릿화로 환경별 설정을 분리할 수 있습니다. 둘째, 버전 관리로 롤백이 용이합니다. 셋째, 의존성 관리로 복잡한 애플리케이션을 쉽게 배포합니다. 넷째, 재사용 가능한 패키지로 일관된 배포가 가능합니다."

---

## ➡️ 다음 학습: Day 100

**주제**: ArgoCD + Helm 연동
