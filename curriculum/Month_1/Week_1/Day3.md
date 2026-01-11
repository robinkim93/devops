# 📅 Day 3: 메모리 분석 (free, vmstat, /proc/meminfo)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Container가 OOMKilled" 장애 분석 역량

시스템 메모리 상태를 분석하고, OOM(Out of Memory) 상황을 진단할 수 있어야 합니다.

---

## ⏰ 예상 학습 시간: 3시간

---

## 📚 Part 1: 핵심 개념 (30분)

### 리눅스 메모리 구성

```
┌─────────────────────────────────────────────────────────────┐
│  리눅스 메모리 = Used + Free + Buffers + Cached             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Used: 프로세스가 실제 사용 중인 메모리                      │
│  Free: 완전히 비어있는 메모리                                │
│  Buffers: 디스크 메타데이터 캐시                            │
│  Cached: 파일 내용 캐시 (Page Cache)                        │
│                                                             │
│  ⚠️ 중요: Buffers + Cached는 필요시 즉시 반환 가능!         │
│                                                             │
│  실제 가용 메모리 = Free + Buffers + Cached                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### OOM Killer란?

```
메모리가 부족하면 → 커널이 프로세스를 강제 종료
→ 이것이 OOM Killer
→ Kubernetes에서 "OOMKilled" 상태의 원인
```

---

## 🛠️ Part 2: 실습 (2시간)

### 실습 1: free 명령어 (20분)

```bash
# 기본 사용 (KB 단위)
free

# 사람이 읽기 쉬운 형태 (GB, MB)
free -h

# 예상 출력:
#               total        used        free      shared  buff/cache   available
# Mem:          7.7Gi       2.1Gi       3.2Gi       256Mi       2.4Gi       5.1Gi
# Swap:         2.0Gi          0B       2.0Gi
```

**핵심 필드**:
- `available`: 실제로 프로세스가 사용 가능한 메모리 (이걸 봐야 함!)
- `buff/cache`: 버퍼+캐시 (필요시 반환 가능)
- `Swap used`: 0이 아니면 메모리 부족 징후

---

### 실습 2: /proc/meminfo 상세 분석 (30분)

```bash
cat /proc/meminfo
```

**핵심 필드**:
```bash
# 중요한 필드만 추출
cat /proc/meminfo | grep -E "MemTotal|MemFree|MemAvailable|Buffers|^Cached|SwapTotal|SwapFree"

# MemTotal:        8000000 kB   ← 전체 메모리
# MemFree:         3000000 kB   ← 빈 메모리
# MemAvailable:    5000000 kB   ← 실제 가용 (이걸 봐야 함!)
# Buffers:          200000 kB   ← 디스크 메타데이터 캐시
# Cached:          2000000 kB   ← 파일 캐시
# SwapTotal:       2000000 kB   ← Swap 전체
# SwapFree:        2000000 kB   ← Swap 여유 (줄어들면 위험)
```

---

### 실습 3: vmstat으로 실시간 모니터링 (30분)

```bash
# 1초 간격으로 5번 출력
vmstat 1 5

# 출력 예시:
# procs -----------memory---------- ---swap-- -----io----
#  r  b   swpd   free   buff  cache   si   so    bi    bo
#  1  0      0 3200000 200000 2000000   0    0     0     0
```

**핵심 필드**:
- `r`: 실행 대기 중인 프로세스 (CPU 대기)
- `b`: I/O 대기 중인 프로세스
- `swpd`: 사용 중인 Swap (0이 아니면 메모리 부족)
- `si/so`: Swap In/Out (0이 아니면 심각한 메모리 부족!)

**장애 판단 기준**:
```
si/so > 0 → Swap 사용 중 → 메모리 부족 → 성능 저하
```

---

### 실습 4: 메모리 압박 상황 시뮬레이션 (30분)

```bash
# 1. 현재 메모리 상태 확인
free -h

# 2. 메모리를 소비하는 프로세스 생성 (500MB)
stress --vm 1 --vm-bytes 500M --timeout 30s &

# 3. 메모리 변화 모니터링
watch -n 1 free -h

# 4. vmstat으로도 확인
vmstat 1 30
```

**stress 설치**:
```bash
sudo apt install -y stress
```

---

### 실습 5: OOM 점수 확인 (20분)

```bash
# 모든 프로세스의 OOM 점수 확인
for pid in $(ls /proc | grep -E '^[0-9]+$'); do
    if [ -f /proc/$pid/oom_score ]; then
        name=$(cat /proc/$pid/comm 2>/dev/null)
        score=$(cat /proc/$pid/oom_score 2>/dev/null)
        if [ -n "$score" ] && [ "$score" -gt 0 ]; then
            echo "$score $pid $name"
        fi
    fi
done | sort -rn | head -10
```

**OOM 점수**:
- 높을수록 OOM 시 먼저 죽음
- 메모리를 많이 쓰는 프로세스가 높은 점수

---

## 📊 Part 3: 장애 시나리오 (선택)

### 시나리오: OOMKilled 원인 분석

```bash
# dmesg에서 OOM 로그 확인
sudo dmesg | grep -i "killed process"

# 출력 예시:
# Out of memory: Killed process 12345 (java) total-vm:4000000kB, 
# anon-rss:2000000kB, file-rss:10000kB, shmem-rss:0kB
```

**분석 포인트**:
- `total-vm`: 가상 메모리 크기
- `anon-rss`: 실제 사용 중인 메모리 (Anonymous)
- 어떤 프로세스가 죽었는지, 얼마나 메모리를 썼는지 확인

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | `free -h`로 메모리 상태 확인 | ☐ |
| 2 | `available` 필드의 의미 이해 | ☐ |
| 3 | `/proc/meminfo` 분석 | ☐ |
| 4 | `vmstat`으로 si/so 확인 | ☐ |
| 5 | OOM 점수 확인 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
free -h                              # 메모리 상태 (간단)
cat /proc/meminfo                    # 메모리 상태 (상세)
vmstat 1                             # 실시간 모니터링
dmesg | grep -i "killed process"     # OOM 로그 확인
cat /proc/[pid]/oom_score            # OOM 점수
```

---

## 📝 학습 기록

```
학습일: 2026년 01월 11일
실제 소요 시간: 1시간
```
[블로그 정리 링크](https://dev-robinkim-93.tistory.com/36)

---

## ➡️ 다음 학습: Day 4

**주제**: 디스크 I/O 분석 (iostat, iotop)

