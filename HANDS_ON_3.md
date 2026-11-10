# 실습 가이드 — 쿠버네티스 스터디 3회차

2회차까지 만든 내 칸 · 내 앱(demo)에 이어서, 앱이 일하는지 묻고(프로브), 파드가 갈 노드를 고르고(스케줄링), 지난 일을 보고(로그 · 모니터링), git 으로 배포합니다(Argo CD). 실습 번호는 2회차에 이어서 11번부터입니다. 실습마다 일부러 망가뜨린 단계(고장)가 있습니다. 원인을 찾아 고쳐 보세요.

| 실습 | 폴더 | 하는 일 |
|---|---|---|
| 0. 준비 | — | 새 압축 풀기, 2회차 끝 모습으로 맞추기, 지켜보기 켜기 |
| 11. 프로브 · 끊김 없는 교체 | `labs/11-probe` | readiness, 고장 9, FAIL 없이 2.0 으로, 고장 10, startupProbe |
| 12. 스케줄링 | `labs/12-schedule` | 노드 라벨, AZ 마다 하나씩, 고장 11 · 12 (갈 곳 없는 파드 · 들어갈 수 없는 노드) |
| 13. 로그 · 모니터링 | `labs/13-observe` | 지금 로그 · 죽기 전 로그, 이벤트, Grafana, Loki 로그 대시보드, 대시보드 없이 찾기(Drilldown), 숫자 쿼리와 알림(Grafana) |
| 14. GitOps — Argo CD | `labs/14-gitops` | 헬름 값 파일을 브라우저로 커밋해 배포, git 으로 되돌리기, 고장 13 |
| 16. 종합 수리 | `labs/16-fix-it` | 밖에서 열리지 않는 shop 고치기 (힌트 없음) |

## 실습 0. 준비

받는 것은 1 · 2회차와 같습니다. **3회차 `k8s-study.zip`** 을 1:1 메시지로 받고(2회차 압축에 3회차 실습 파일과 내 계정 파일 `accounts.txt` 가 더해진 것 · 접속 파일은 같음), 이 실습 가이드를 받습니다. 앞 회차에 오지 않았어도 아래 명령 그대로 하면 됩니다.

`k8s-study.zip` 은 **다운로드 폴더**에 저장합니다. 명령은 누구나 같습니다. 이름이나 아이디를 바꿔 칠 곳은 없습니다.

### ① 새 압축 풀기

내 PC 에 맞는 칸 하나만 합니다. 앞 회차에 했어도 첫 줄부터 그대로 다시 칩니다. 첫 줄이 2회차 폴더를 덮어씁니다(앞 회차에 고친 YAML 도 원래대로 돌아갑니다).

**Windows** — PowerShell 에서 한 줄씩:

```powershell
Expand-Archive "$HOME\Downloads\k8s-study.zip" -DestinationPath $HOME -Force

$p = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$p;$HOME\k8s-study\bin", "User")
[Environment]::SetEnvironmentVariable("KUBECONFIG", "$HOME\k8s-study\kubeconfig", "User")

$env:Path += ";$HOME\k8s-study\bin"
$env:KUBECONFIG = "$HOME\k8s-study\kubeconfig"
kubectl version --client
kubectl get pods
```

**Mac** — 터미널에서 한 줄씩. 첫 줄은 이 가이드의 명령을 줄 끝 `# 설명` 까지 붙여 넣어도 에러가 나지 않게 하는 설정입니다(Mac 기본 터미널은 `#` 뒤를 설명으로 보지 않습니다). 그래서 첫 줄부터 칩니다.

```bash
setopt interactivecomments; echo 'setopt interactivecomments' >> ~/.zshrc
unzip -oq ~/Downloads/k8s-study.zip -d ~                          # -o 는 2회차 폴더를 묻지 않고 덮어쓰기
echo 'export KUBECONFIG=~/k8s-study/kubeconfig' >> ~/.zshrc; export KUBECONFIG=~/k8s-study/kubeconfig
brew install kubectl                                               # 이미 있으면 그대로
kubectl version --client
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl version --client
Client Version: v1.34.13
PS> kubectl get pods  →  curl   1/1   Running   0   13d     ← 2회차에서 남긴 것 (처음이면 No resources found)
```

**확인** — 두 줄 다 나오면 됩니다. 1 · 2회차에 했어도 첫 줄부터 그대로 쳐도 됩니다.

**막히면** — Windows: 탐색기로 풀지 않고 첫 줄 명령으로 풉니다 · 새 탭에서 kubectl 을 못 찾으면 Windows Terminal 을 닫았다 다시 엽니다. Mac: 첫 줄 끝에는 # 설명을 붙이지 않습니다 · unzip 이 파일을 못 찾으면 다운로드 폴더의 파일 이름을 봅니다.

실습 명령은 전부 **이 폴더(`k8s-study`) 맨 위**에서 칩니다. `labs` 안으로 더 들어가지 않습니다. 새 창 · 새 탭을 열면 `cd ~/k8s-study` 부터 칩니다.

### ② 2회차 끝 모습으로 맞추기

demo 1.0 × 3 · 서비스 · 입구 · curl 파드만 남게 맞추고, 내 계정 파일을 엽니다.

```powershell
cd ~/k8s-study                                      # 오늘 명령도 전부 이 폴더에서
kubectl delete -f labs/07-volume -f labs/08-hpa -f labs/10-fix-it --ignore-not-found
kubectl apply -f labs/09-rollout/deployment.yaml -f solutions/03-service -f solutions/06-ingress
kubectl get deploy,svc,ingress,pods
cat accounts.txt                                    # Gitea · Argo CD 아이디 · 비밀번호 (나만 봄)
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy,svc,ingress,pods
deployment.apps/demo   3/3     3            3           20s
service/demo           ClusterIP   10.100.183.126   <none>   80/TCP   13d
ingress.networking.k8s.io/demo   traefik   host.43-201-184-138.sslip.io   a93492…   80   6d
pod/curl                    1/1     Running   0          13d
PS> cat accounts.txt
  아이디     host                ← 사람마다 다름
```

**확인** — demo 3/3 · service demo · ingress demo · curl 이 보이고, 계정 파일에 내 아이디 · 비밀번호가 있으면 됩니다.

**막히면** — not found 는 괜찮음 · delete 가 30초쯤 멈추면 디스크를 지우는 중 · 'path does not exist' 면 cd ~/k8s-study.

### ③ 탭 ① 에 지켜보기 켜기

새 탭(탭 ①)을 열어 2회차 실습 9 의 그 반복을 켭니다. 오늘 앞 실습 내내 이 창을 봅니다.

```powershell
cd ~/k8s-study                                      # 새 탭이라 폴더부터
kubectl exec -it curl -- sh -c 'while true; do curl -s -m1 demo/ | grep ver || echo FAIL; sleep 0.5; done'
```

나와야 할 화면 (예시)

```text
PS> kubectl exec -it curl -- sh -c '…'          ← 탭 ①
version  : 1.0
version  : 1.0
version  : 1.0
```

**확인** — 탭 ① 에 version : 1.0 이 계속 찍히면 됩니다. 명령은 다른 탭(탭 ②)에서 칩니다.

**막히면** — 따옴표는 작은따옴표(') 그대로 · 끌 때는 Ctrl+C · -it 를 빼면 curl 파드 안에서 계속 돕니다.

- 터미널은 Windows 는 **Windows Terminal + PowerShell**, Mac 은 기본 **터미널**입니다. 새 탭은 Windows `Ctrl+Shift+T`, Mac `Cmd+T`. 새 탭은 `cd ~/k8s-study` 부터 칩니다.
- YAML 의 `# TODO` 줄은 VS Code 같은 편집기로 고칩니다(메모장은 들여쓰기가 깨지기 쉽습니다).
- 브라우저 주소(Grafana · Argo CD · Gitea)는 누구나 같습니다. 아이디 · 비밀번호만 `accounts.txt` 에서 봅니다.
- 내 PC 에서 curl 을 칠 때 Windows 는 **`curl.exe`**, Mac 은 `curl` 입니다. 명령 칸에 `# Windows · Mac 은 curl` 이라고 적힌 줄은 Mac 에서 `curl.exe` 를 `curl` 로 바꿔 칩니다.
- 명령 칸은 **한 줄씩** 칩니다. 작은따옴표(`'`)는 그대로 둡니다.

**읽는 법** — 단계마다 **할 일 → 명령 → 나와야 할 화면 → 확인** 순서입니다. 나와야 할 화면은 예시라서 파드 이름 · IP · 노드 이름 · 숫자 · 시각은 사람마다 다릅니다. **(먼저 끝났으면)** 이 붙은 단계는 건너뛰어도 다음 단계에 지장이 없습니다. 자주 쓰는 명령은 [치트시트](docs/치트시트.md) 에 있습니다.

---

## 실습 11. 프로브 · 끊김 없는 교체 — `labs/11-probe`

쿠버네티스에 '앱이 준비됐나 · 살아 있나 · 다 떴나' 를 묻는 장치(프로브)를 붙입니다. 2회차 고장 8 과 실습 9 의 FAIL 이 어떻게 막히는지 탭 ① 의 지켜보기로 봅니다. 탭을 두 개 씁니다(① 지켜보기 · ② 명령).

> demo · 서비스 · curl 파드가 없으면 먼저: `kubectl apply -f labs/09-rollout/deployment.yaml -f solutions/03-service -f solutions/06-ingress`

### ① 준비됐을 때만 명단에

deployment.yaml 의 TODO 를 /actuator/health/readiness 로 고쳐 apply 하고 READY 를 봅니다.

```powershell
kubectl apply -f labs/11-probe/deployment.yaml
kubectl get pods -l app=demo -w                     # 새 파드 0/1 → 1/1 · 하나씩 · Ctrl+C
kubectl describe pod -l app=demo                    # 중간의 Readiness: 줄
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -l app=demo -w
demo-549776cf6d-jwdm2   0/1     Running             0          1s     ← 떴지만 아직 명단 밖
demo-549776cf6d-jwdm2   1/1     Running             0          12s    ← 준비됨 → 명단에
demo-5794764db-vlg99    1/1     Terminating         0          91s    ← 그다음에 옛 파드 하나
PS> kubectl describe pod -l app=demo
    Readiness:  http-get http://:8080/actuator/health/readiness delay=0s timeout=1s period=2s #success=1 #failure=1
```

**확인** — 새 파드가 0/1 로 잠깐 있다가 1/1 이 되고, 그다음에 옛 파드가 내려가면 됩니다. 탭 ① 은 1.0 그대로입니다.

**막히면** — 계속 0/1 이면 TODO 를 안 고친 것(describe 맨 아래) · 꺼지는 옛 파드의 Error 와 FAIL 한두 줄은 괜찮습니다.

### ② 고장 9 — 이번엔 멈춘다

2회차 고장 8 과 같은 새 판에 readiness 를 붙여 배포합니다. 탭 ① 과 배포 상태를 보고, 되돌립니다.

```powershell
kubectl apply -f labs/11-probe/deployment-badport.yaml
kubectl rollout status deploy/demo                  # 여기서 멈춤 · 1분 보고 Ctrl+C
kubectl get pods -l app=demo                        # 1/1 셋 + 0/1 하나
kubectl describe pod -l app=demo                    # 0/1 파드의 맨 아래 Events
kubectl rollout undo deploy/demo
```

나와야 할 화면 (예시)

```text
PS> kubectl rollout status deploy/demo
Waiting for deployment "demo" rollout to finish: 1 out of 3 new replicas have been updated...
demo-549776cf6d-2nw84   1/1     Running   0          3m2s     ← get pods: 옛 판 셋 그대로
demo-858f964c58-24tlc   0/1     Running   0          63s      ← 새 판 하나만, 명단 밖
  Warning  Unhealthy  … Readiness probe failed: Get "http://10.42.2.4:8080/actuator/…": … connection refused
탭 ①  version  : 1.0     ← 계속 1.0, FAIL 없음
```

**확인** — 탭 ① 이 계속 1.0 이면 성공입니다. 2회차에는 '성공' 이라더니 전부 FAIL, 오늘은 배포가 멈추고 옛 판이 계속 일합니다.

**막히면** — describe 가 길면 맨 아래 Events 만 봅니다 · undo 뒤 get pods 에서 0/1 파드가 사라지면 됩니다.

### ③ 끊김 없이 2.0 으로

deployment-zero.yaml 을 apply 하고, 1.0 두 군데를 2.0 으로 고쳐 다시 apply 합니다. FAIL 을 셉니다.

```powershell
kubectl apply -f labs/11-probe/deployment-zero.yaml       # readiness + preStop · 1.0
kubectl rollout status deploy/demo

kubectl apply -f labs/11-probe/deployment-zero.yaml       # 1.0 두 군데를 2.0 으로 고친 뒤
kubectl rollout status deploy/demo
```

나와야 할 화면 (예시)

```text
탭 ①
version  : 1.0
version  : 2.0
version  : 1.0
version  : 2.0     ← 잠깐 섞였다가
version  : 2.0     ← 2.0 만 남음 · FAIL 없음
```

**확인** — 1.0 → (섞임) → 2.0 사이에 FAIL 이 한 줄도 없으면 성공입니다. 2회차 실습 9 ② 와 같은 교체입니다.

**막히면** — 첫 apply 에서 FAIL 한두 줄은 괜찮습니다(옛 파드에는 preStop 이 없음) · unchanged 면 파일을 저장 안 한 것.

### ④ 고장 10 — 다시 시작만 반복

CPU 를 묶어 느리게 뜨는 앱을 띄웁니다. RESTARTS 가 왜 늘어나는지 찾습니다.

```powershell
kubectl apply -f labs/11-probe/slow-broken.yaml
kubectl get pods -l app=slow-broken -w              # RESTARTS 가 늘어남 · 1~2분 보고 Ctrl+C
kubectl describe pod -l app=slow-broken             # 맨 아래 Events
kubectl logs -l app=slow-broken --previous          # 죽기 직전 로그
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -l app=slow-broken -w
slow-broken-57f7f5f659-j6bs2   0/1     Running            2 (1s ago)    54s
slow-broken-57f7f5f659-j6bs2   0/1     Running            4 (1s ago)    104s
  Warning  Unhealthy  … Liveness probe failed: Get "http://…:8080/actuator/health/liveness": … connection refused
  Normal   Killing    … Container slow-broken failed liveness probe, will be restarted
PS> kubectl logs -l app=slow-broken --previous
… DemoApplication : Starting DemoApplication using Java 21 …   ← 'Started' 줄 없이 끊김
```

**확인** — 앱이 망가진 게 아니라, 다 뜨기 전에 '살아 있나' 를 물어 죽인 것이라고 말할 수 있으면 성공입니다. 지우지 않고 둡니다.

**막히면** — RESTARTS 가 0 이면 조금 더 기다립니다 · --previous 가 비어 있으면 아직 한 번도 다시 시작하지 않은 것.

### ⑤ startupProbe 로 지켜 주기

slow.yaml 의 TODO 를 30 으로 고쳐 apply 합니다. 같은 앱 · 같은 CPU 인데 다시 시작하지 않습니다.

```powershell
kubectl apply -f labs/11-probe/slow.yaml
kubectl get pods -l app=slow -w                     # RESTARTS 0 으로 0/1 → 1/1 · Ctrl+C
kubectl top pod -l app=slow                         # CPU 가 150m 근처
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -l app=slow -w
slow-7c9d8f6b4-m3p8z   0/1     Running   0          40s     ← 기다려 주는 중
slow-7c9d8f6b4-m3p8z   1/1     Running   0          1m      ← RESTARTS 0
PS> kubectl top pod -l app=slow
NAME                    CPU(cores)   MEMORY(bytes)
slow-7c9d8f6b4-m3p8z    150m         140Mi       ← 막 뜬 직후 · 한도에 눌림
```

**확인** — RESTARTS 0 으로 1/1 이 되면 성공입니다. slow 와 slow-broken 은 실습 13 에서 다시 보니 지우지 않습니다.

**막히면** — apply 가 'cannot unmarshal' 로 거절되면 TODO 를 숫자로 안 바꾼 것 · top 이 error 면 30초 뒤 다시.

### 정리 — 실습 12 로 넘어가기 전에

탭 ① 의 반복은 Ctrl+C 로 끕니다(실습 14 에서 다시 켭니다). 남은 것을 봅니다.

```powershell
kubectl get deploy
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
demo          3/3     3            3           25m     ← 2.0 · readiness · preStop
slow          1/1     1            1           3m
slow-broken   0/1     1            0           6m      ← 실습 13 에서 다시 봄
```

**확인** — demo 3/3 · slow 1/1 · slow-broken 0/1 이면 됩니다.

---

## 실습 12. 스케줄링 — `labs/12-schedule`

파드가 갈 노드를 우리가 거드는 법입니다. AZ 마다 고루 퍼뜨리고, 갈 곳이 없는 파드와 들어갈 수 없는 노드(도구 노드)를 봅니다. 노드는 보기만 할 수 있습니다.

### ① 노드와 라벨

노드 4대의 AZ · 종류 · 역할 라벨을 보고, 지금 내 demo 파드가 어느 노드에 있는지 봅니다.

```powershell
kubectl get nodes -L topology.kubernetes.io/zone,node.kubernetes.io/instance-type,role
kubectl get pods -l app=demo -o wide                # NODE 열 = 노드 이름
```

나와야 할 화면 (예시)

```text
PS> kubectl get nodes -L topology.kubernetes.io/zone,node.kubernetes.io/instance-type,role
NAME                       STATUS   …   ZONE              INSTANCE-TYPE   ROLE
ip-192-168-64-145.…        Ready    …   ap-northeast-2a   t3.medium
ip-192-168-58-30.…         Ready    …   ap-northeast-2c   t3.medium
ip-192-168-11-197.…        Ready    …   ap-northeast-2d   t3.medium
ip-192-168-70-12.…         Ready    …   ap-northeast-2a   t3.large        tools
PS> kubectl get pods -l app=demo -o wide
demo-86bffd5566-h6hwr   1/1   Running   …   ip-192-168-58-30.…    ← 위 표에서 2c
demo-86bffd5566-…       1/1   Running   …   ip-192-168-58-30.…    ← 같은 노드에 몰려 있을 수도
```

**확인** — ROLE 이 tools 인 노드가 한 대 보이고, 내 demo 의 NODE 를 위 NAME 에 맞춰 AZ 를 말할 수 있으면 됩니다.

**막히면** — Forbidden 이 나오면 바로 알려 주세요 — 노드 보기(get · describe) 권한은 1회차부터 내 계정에 있습니다.

### ② AZ 마다 하나씩

spread.yaml 의 TODO 를 topology.kubernetes.io/zone 으로 고쳐 apply 하고 노드를 봅니다.

```powershell
kubectl apply -f labs/12-schedule/spread.yaml
kubectl get pods -l app=spread -o wide              # NODE 열 · AZ 셋에 하나씩
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -l app=spread -o wide
spread-58c99cfc6-vq7gh   1/1   Running   …   ip-192-168-64-145.…   ← 2a
spread-58c99cfc6-lfwcl   1/1   Running   …   ip-192-168-58-30.…    ← 2c
spread-58c99cfc6-zjj8p   1/1   Running   …   ip-192-168-11-197.…   ← 2d
(도구 노드에는 가지 않음 — 테인트)
```

**확인** — 세 파드가 세 노드(세 AZ)에 하나씩이면 됩니다. 2회차 demo 처럼 한 노드에 몰리지 않습니다.

**막히면** — 셋 다 Pending 이고 Events 에 missing required label 이면 TODO 를 안 고친 것 — 파일을 고쳐 다시 apply.

### ③ 고장 11 — 갈 노드가 없다

파드 하나를 띄우고, Pending 인 이유를 describe 에서 읽은 뒤 고쳐서 다시 띄웁니다.

```powershell
kubectl apply -f labs/12-schedule/pending.yaml
kubectl get pod picky                               # Pending
kubectl describe pod picky                          # Node-Selectors 줄 · 맨 아래 Events
kubectl delete pod picky                            # pending.yaml 의 disktype: ssd 를 고친 뒤 →
kubectl apply -f labs/12-schedule/pending.yaml
kubectl get pod picky -o wide                       # Running 을 봤으면 delete pod picky (자리 아끼기)
```

나와야 할 화면 (예시)

```text
PS> kubectl get pod picky
picky   0/1     Pending   0          5s
PS> kubectl describe pod picky
Node-Selectors:  disktype=ssd
  Warning  FailedScheduling  5s  default-scheduler  0/4 nodes are available:
           1 node(s) had untolerated taint(s), 3 node(s) didn't match Pod's node affinity/selector.
```

**확인** — Node-Selectors 의 disktype=ssd 라벨을 가진 노드가 하나도 없어서 Pending 이라고 말할 수 있으면 성공.

**막히면** — 고치기: disktype: ssd → topology.kubernetes.io/zone: ap-northeast-2a (들여쓰기는 그대로).

### ④ 고장 12 — 못 들어가는 노드

도구 노드의 테인트를 보고 그 노드로 가라는 파드를 띄운 뒤, tolerations 주석을 풀어 들여보냅니다.

```powershell
kubectl describe node -l role=tools                 # Taints 줄
kubectl apply -f labs/12-schedule/tools-pod.yaml
kubectl describe pod visitor                        # Node-Selectors · Tolerations · 맨 아래 Events
kubectl delete -f labs/12-schedule/tools-pod.yaml   # tolerations 주석을 푼 뒤 →
kubectl apply -f labs/12-schedule/tools-pod.yaml
kubectl get pod visitor -o wide                     # NODE = 도구 노드
```

나와야 할 화면 (예시)

```text
PS> kubectl describe node -l role=tools     →   Taints:   dedicated=tools:NoSchedule
PS> kubectl describe pod visitor
  Warning  FailedScheduling  …  0/4 nodes are available: 1 node(s) had untolerated taint(s), 3 …
PS> kubectl get pod visitor -o wide
visitor   1/1     Running   0          8s    192.168.…    ip-192-168-70-12.…   ← 도구 노드
```

**확인** — nodeSelector 는 '여기로', 테인트는 '허락받은 파드만' — 둘 다 맞아야 들어간다고 말할 수 있으면 성공.

**막히면** — 주석은 네 줄을 골라 VS Code 에서 Ctrl+/ (Mac Cmd+/) · tolerations: 의 시작 위치가 nodeSelector: 와 같으면 맞음.

### 정리 — 쉬는 시간 전에

실습 12 에서 만든 것을 지우고, 실습 13 에서 볼 demo 의 로그 설정을 켜 둡니다(쉬는 동안 파드가 바뀜).

```powershell
kubectl delete -f labs/12-schedule --ignore-not-found
kubectl patch deploy demo --patch-file labs/13-observe/demo-logging.yaml   # 실습 13 준비
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl patch deploy demo --patch-file labs/13-observe/demo-logging.yaml
deployment.apps/demo patched
PS> kubectl get pods
curl                          1/1     Running            0          13d
demo-78c85d487b-9mqqs         1/1     Running            0          20s       (demo 셋 · 이름이 바뀜)
slow-7c9d8f6b4-m3p8z          1/1     Running            0          11m
slow-broken-6d9f7c8b5-x2kq7   0/1     CrashLoopBackOff   9          14m
```

**확인** — spread · picky · visitor 가 없고, demo 셋이 새 이름으로 1/1 이면 됩니다(바뀌는 중이면 쉬는 동안 끝남).

**막히면** — patched 대신 not found 면 demo 가 없는 것 — 실습 11 ③ 의 deployment-zero.yaml 을 다시 apply 한 뒤 이 줄.

---

## 실습 13. 로그 · 모니터링 — `labs/13-observe`

실습 11 에서 다시 시작을 반복한 slow-broken 과 CPU 에 눌린 slow 를 다시 봅니다. 지금 로그 · 죽기 전 로그 · 이벤트 · 지난 그래프(Grafana) 순서입니다. ③ ④ 는 브라우저에서 합니다.

### ① 지금 로그 · 죽기 전 로그

demo 의 로그를 파드 이름을 붙여 보고, 다시 시작한 slow-broken 의 '죽기 전' 로그를 봅니다.

```powershell
kubectl logs deploy/demo --tail=3                   # 셋 중 하나만
kubectl logs -l app=demo --prefix --tail=1          # 셋 다 · 앞에 파드 이름
kubectl logs -l app=slow-broken --previous          # 직전에 죽은 컨테이너
kubectl get pods -l app=slow-broken                 # RESTARTS
```

나와야 할 화면 (예시)

```text
PS> kubectl logs -l app=demo --prefix --tail=1
[pod/demo-…-9mqqs/demo] level=info type=access requestId=- method=GET path=/actuator/health/readiness status=200
[pod/demo-…-qrh74/demo] level=info type=access requestId=- method=GET path=/ status=200 ms=6
  (요청마다 한 줄 · 앱 로그는 {"log.level":"INFO", … } JSON 한 줄 — 실습 12 끝에 켠 설정)
PS> kubectl get pods -l app=slow-broken
slow-broken-9c7979958-bjnmz   0/1     CrashLoopBackOff   9 (73s ago)   14m
```

**확인** — --prefix 로 줄마다 어느 파드인지 보이고, --previous 끝에 'Started DemoApplication' 줄이 없으면 됩니다 = 다 뜨기 전에 liveness 가 죽인 것.

**막히면** — Found 3 pods, using … 는 셋 중 하나만 골랐다는 뜻 · readiness 가 2초마다 부르는 것도 요청이라 한 줄씩 남음.

### ② 이벤트 — 칸의 알림판

내 칸의 일을 시간 순서로 보고 경고만 골라 봅니다. 마지막으로 일어난 뒤 1시간이 지나면 지워집니다.

```powershell
kubectl events                                      # 아래쪽이 최근
kubectl events --types=Warning                      # 경고만
```

나와야 할 화면 (예시)

```text
PS> kubectl events --types=Warning
LAST SEEN            TYPE      REASON             OBJECT                  MESSAGE
13m (x6 over 13m)    Warning   Unhealthy          Pod/slow-broken-…       Liveness probe failed: …
3m50s (x40 over 12m) Warning   BackOff            Pod/slow-broken-…       Back-off restarting failed container …
83s                  Warning   FailedScheduling   Pod/spread-…            0/4 nodes are available: …
18s                  Warning   FailedScheduling   Pod/visitor             0/4 nodes are available: …
```

**확인** — slow-broken · 실습 12 의 고장이 시간 순서로 보이면 됩니다. 고장 9 줄이 없으면 정상 — 다시 안 일어난 채 1시간이 지나 지워진 것으로, ④ Loki 에서 다시 봅니다.

**막히면** — (x6 over 13m) 은 같은 일이 13분 동안 6번 · 되풀이되는 일은 한 줄로 남고, 일어날 때마다 1시간을 다시 셈.

### ③ Grafana — 지난 3시간

브라우저로 Grafana 를 열어 내 칸의 CPU · 메모리 그래프를 봅니다. 로그인은 없습니다.

```powershell
http://grafana.43-201-184-138.sslip.io/             # 브라우저 주소창
# 왼쪽 메뉴 Dashboards → Kubernetes / Compute Resources / Namespace (Pods)
# 위의 namespace 목록 → 내 칸 (k8s-study- 뒤에 accounts.txt 의 아이디)
# 오른쪽 위 시간 → Last 3 hours (1시간 넘게 지난 실습 11 까지)
# Dashboards 목록 → … / Compute Resources / Pod → pod 목록 slow-… → CPU Throttling 패널
```

나와야 할 화면 (예시)

```text
브라우저
CPU Usage      — 파드마다 선 하나 · demo 를 바꿀 때마다 옛 선이 끊기고 새 선이 생김
Memory Usage   — demo 셋이 120~200Mi 근처
CPU Throttling — slow 가 뜨는 동안 한도(150m)에 눌린 봉우리 · 다 뜬 뒤엔 낮게(예: 8%)
  값 = 0.1초 구간 중 한도에 걸린 비율 · 점선 25% = 기본 알림 기준(15분 넘게 계속이면)
```

**확인** — kubectl top 은 지금 값 하나뿐인데, 여기는 지난 3시간이 선으로 남아 있다는 것을 보면 됩니다.

**막히면** — 그래프가 비면 namespace 목록 · 시간을 다시 · 안 열리면 회사 망 — 휴대폰 데이터로 같은 주소.

### ④ Loki — 죽은 컨테이너의 로그까지

로그 대시보드의 '찾을 글자' 칸으로 slow-broken 의 시작 줄을 세고, 지워진 이벤트를 찾습니다.

```powershell
# Grafana → Dashboards → 스터디 · 내 칸 로그        (시간은 처음부터 Last 3 hours)
# namespace 목록 → 내 칸 (k8s-study- 뒤에 accounts.txt 의 아이디) · pod 목록 → slow-broken-…
# 찾을 글자 칸 → Starting DemoApplication → Enter → 왼쪽 위 '찾은 로그 줄'
# 찾을 글자 칸 → probe failed → Enter → 맨 아래 '이벤트' 패널 (pod 와 상관없이 내 칸 전부)
```

나와야 할 화면 (예시)

```text
브라우저 · 스터디 · 내 칸 로그
찾은 로그 줄  27       ← 컨테이너마다 한 줄 (RESTARTS 33 · 20초 만에 죽은 몇 개는 빠짐)
로그 양 — 다시 시작할 때마다 막대 · 아래 로그 패널엔 그 줄만
이벤트 · reason=Unhealthy type=Warning … msg="Readiness probe failed: …"   ← 고장 9
--previous 는 바로 앞 하나뿐 · 여기는 죽은 컨테이너 거의 전부 · 1시간 지난 이벤트도
```

**확인** — '찾은 로그 줄' 이 여럿(RESTARTS 와 비슷하게)이고, probe failed 로 찾은 이벤트에 고장 9 (Readiness) 줄이 있으면 됩니다. 파드 밖(Loki)에 쌓아 두어서입니다.

**막히면** — pod 목록이 비면 namespace 목록부터 · 0 이면 찾을 글자의 띄어쓰기 (대소문자는 상관없음) · 막 찍힌 줄은 1분쯤 늦음.

### ⑤ 요청 번호로 찾기 — Drilldown

요청 번호를 붙여 demo 를 두 번 부르고, Grafana 기본 로그 화면에서 그 번호와 레벨로 찾습니다.

```powershell
kubectl exec curl -- curl -s -H "X-Request-Id: req-1109-0001" demo/            # 보통 요청
kubectl exec curl -- curl -s -X POST -H "X-Request-Id: req-1109-0002" demo/    # 잘못된 요청
# Grafana 왼쪽 메뉴 Drilldown → Logs · Last 15 minutes → demo 칸 Show logs
# 위 Labels 칸 → namespace → = → 내 칸 · Line filter 칸 → req-1109-0002
# Line filter 를 지우고 Log levels → warn
```

나와야 할 화면 (예시)

```text
브라우저 · Drilldown → Logs
Labels   service_name = demo   namespace = k8s-study-host        (줄을 펼치면 env = dev)
level=info type=access requestId=req-1109-0002 method=POST path=/ status=405 ms=40
warn → {"log.level":"WARN", "service.environment":"dev", … "message":"Resolved […'POST' is not supported]"}
요청 번호 하나로 — 어느 파드가 · 언제 · 무슨 결과(405) · 같은 때의 경고 줄
```

**확인** — req-1109-0002 로 찾은 줄이 status=405 이고, warn 으로 거른 줄에 같은 시각의 'POST' is not supported 가 있으면 됩니다.

**막히면** — 줄이 없으면 1분 뒤 새로 고침(수집이 늦음) · 다른 사람도 같은 번호라 namespace 를 꼭 거름 · demo 칸이 없으면 시간을 넓힘.

### ⑥ Grafana — 숫자를 묻고, 알림 보기

Explore 에서 숫자를 한 줄로 묻고, 그 숫자로 도는 기본 알림이 내 칸에서 울리는지 봅니다.

```powershell
# Grafana 왼쪽 메뉴 Explore → 오른쪽 Code → 칸에 한 줄 넣고 오른쪽 위 Run query
kube_pod_container_status_restarts_total{container="slow-broken"}
container_cpu_cfs_throttled_periods_total{container="slow"}
# 왼쪽 메뉴 Alerting → Alert rules → 검색 KubePodCrashLooping → 이름 → Instances
```

나와야 할 화면 (예시)

```text
브라우저 · Explore (위 데이터 소스 칸 = Prometheus)
…restarts_total{…namespace="k8s-study-host", pod="slow-broken-…"}  9   ← Graph 는 계단
…throttled_periods_total{…pod="slow-…"}   618   ← 뜬 뒤 지금까지 눌린 횟수(누계)
브라우저 · Alerting → Alert rules → KubePodCrashLooping
Firing · Pending period 15m · max_over_time(…reason="CrashLoopBackOff"…[5m]) >= 1
Instances — namespace=k8s-study-host  pod=slow-broken-…   Firing
보낼 곳(Alertmanager)을 꺼 둬서 아무에게도 가지 않음 — 현업이면 여기서 메신저 · 전화로
```

**확인** — Explore 에 RESTARTS 와 같은 숫자가 나오고, Alert rules 에서 KubePodCrashLooping 의 내 칸 줄이 Firing 이면 됩니다(기본 규칙 · 15분 계속이면).

**막히면** — 모든 사람 칸이 같이 나옴 — Search by label 칸에 namespace=내 칸 이름 · Pending 이면 아직 15분 전 · Builder 면 Code.

### ⑦ 부하를 걸면 (먼저 끝났으면)

2회차 실습 8 의 부하 파드를 다시 띄우고, 1분 뒤 Grafana 를 새로 고칩니다. 보고 나서 지웁니다.

```powershell
kubectl apply -f labs/08-hpa/load.yaml
# 1분 뒤 Grafana 새로 고침 → demo 의 CPU 선이 오름
kubectl delete -f labs/08-hpa/load.yaml
```

나와야 할 화면 (예시)

```text
브라우저
CPU Usage — demo 셋의 선이 같이 올라감 (2회차에는 HPA 가 여기서 파드를 늘렸음)
```

**확인** — 부하를 건 시각부터 선이 오르면 됩니다. 꼭 지웁니다.

### 정리 — 실습 14 로 넘어가기 전에

slow · slow-broken 을 지웁니다. demo · curl 만 남깁니다.

```powershell
kubectl delete deploy slow slow-broken
kubectl get pods
# Grafana 로그 대시보드 새로 고침 → 지운 slow-broken 의 로그가 그대로
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
curl                    1/1     Running   0          13d
demo-86bffd5566-h6hwr   1/1     Running   0          30m       (demo 셋)
```

**확인** — demo 셋과 curl 만 남고, Grafana 에는 지운 파드의 로그가 남아 있으면 됩니다.

---

## 실습 14. GitOps — Argo CD — `labs/14-gitops`

오늘부터 demo 는 손으로 만들지 않습니다. 내 git 저장소(Gitea)의 파일을 브라우저에서 고쳐 커밋하면 Argo CD 가 내 칸을 그 파일대로 맞춥니다. 아이디 · 비밀번호는 `accounts.txt` 에 있습니다(두 곳이 같음). `labs/14-gitops/app/` 은 저장소 파일의 참고용 사본이라 고치지 않습니다.

### ① 로그인 · 손으로 만든 것 치우기

accounts.txt 의 계정으로 Gitea 와 Argo CD 에 로그인하고, kubectl 로 만든 demo 를 지웁니다.

```powershell
cat accounts.txt                                    # 주소 · 아이디 · 비밀번호 (두 곳이 같음)
# 브라우저 ①  http://git.43-201-184-138.sslip.io/     → 오른쪽 위 로그인
# 브라우저 ②  http://argocd.43-201-184-138.sslip.io/  → SIGN IN
kubectl delete deploy,svc,ingress demo              # 오늘부터 demo 는 손으로 만들지 않음
```

나와야 할 화면 (예시)

```text
Argo CD · Applications (지우기 전)
host-demo   ♥ Healthy   ⊘ OutOfSync     ← 손으로 만든 demo 가 git 과 다름
PS> kubectl delete deploy,svc,ingress demo
deployment.apps "demo" deleted   service "demo" deleted   ingress … "demo" deleted
Argo CD (지운 뒤)
host-demo   ◯ Missing   ⊘ OutOfSync     ← git 에는 있는데 클러스터에는 없음
```

**확인** — 지운 뒤 내 Application 이 Missing · OutOfSync 가 되면 됩니다. 남의 Application 은 보이지 않습니다.

**막히면** — 로그인이 안 되면 비밀번호를 복사할 때 앞뒤 공백 · 남의 저장소는 보이기만 하고 고쳐지지 않습니다.

### ② 첫 SYNC — git 대로 만들기

Argo CD 에서 내 Application 을 열어 SYNC → SYNCHRONIZE 를 누르고 kubectl 로 봅니다.

```powershell
# Argo CD → 내 Application 카드 → 위의 SYNC → 오른쪽 창의 SYNCHRONIZE
kubectl get deploy,svc,ingress demo                 # git 의 차트가 만든 그대로
```

나와야 할 화면 (예시)

```text
Argo CD
APP HEALTH ♥ Healthy     SYNC STATUS ✓ Synced to main (2bbafa1)
나무: host-demo → deploy demo → rs → pod 2 · svc demo · ing demo
PS> kubectl get deploy,svc,ingress demo
deployment.apps/demo   2/2     2            2           1m       ← values-dev.yaml 의 replicaCount 2
```

**확인** — Synced · Healthy 가 되고 demo 가 2/2 면 됩니다. Healthy 는 Argo CD 가 우리 readiness 를 읽은 결과입니다.

**막히면** — Progressing 이 오래면 파드가 뜨는 중(1분 안팎) · 나무에서 파드를 누르면 EVENTS · LOGS 탭.

### ③ git 으로 2.0 — 브라우저에서 커밋

탭 ① 에 지켜보기(첫 줄)를 켜고, Gitea 에서 값 파일의 판 1.0 → 2.0 커밋 뒤 Argo CD 에서 맞춥니다.

```powershell
kubectl exec -it curl -- sh -c 'while true; do curl -s -m1 demo/ | grep ver || echo FAIL; sleep 0.5; done'
# Gitea → k8s-study-gitops → app → values-dev.yaml → 연필(파일 수정)
# tag: "1.0" → "2.0" (따옴표는 그대로) → 맨 아래 '변경 내용을 커밋'
# Argo CD → REFRESH → DIFF(바뀐 줄 보기) → SYNC → SYNCHRONIZE
```

나와야 할 화면 (예시)

```text
Argo CD · DIFF
-  image: …/demo-app:1.0
+  image: …/demo-app:2.0          ← 바뀐 것은 이 한 줄
탭 ①
version  : 1.0  …  version  : 2.0     ← 섞였다가 2.0 만 · FAIL 없음
```

**확인** — 값 한 줄을 커밋했는데 이미지 줄이 바뀌어 2.0 이 되고 FAIL 이 없으면 됩니다. 틀에 실습 11 의 readiness · preStop 이 들어 있어서입니다.

**막히면** — 이미지가 …:2 면 따옴표가 빠진 것 · REFRESH 뒤에도 Synced 면 커밋이 안 된 것 · 안 눌러도 30초 안에 저절로 OutOfSync.

### ④ 되돌리기 = git

방금 커밋을 되돌리는 새 커밋을 만들고 SYNC 해서 1.0 으로 돌아갑니다.

```powershell
# Gitea → 커밋 목록 → 방금 커밋 → 작업 ▾ → Revert → (창) Revert → 변경 내용을 커밋
# (메뉴를 못 찾으면 ③ 처럼 2.0 을 1.0 으로 다시 고쳐 커밋해도 같음)
# Argo CD → REFRESH → SYNC → SYNCHRONIZE
```

나와야 할 화면 (예시)

```text
Gitea · 커밋 목록
revert 3f9c2a1b7e                   ← 되돌리는 새 커밋 · 뒤는 ③ 커밋의 번호 (Revert 화면의 기본 제목)
app/values-dev.yaml 업데이트        ← ③ 의 커밋 (Gitea 가 붙인 기본 메시지)
탭 ①
version  : 2.0  …  version  : 1.0
```

**확인** — 탭 ① 이 1.0 이 되고 git 에 되돌린 커밋이 하나 더 남으면 됩니다 — 2회차 undo 와 달리 git 도 1.0.

**막히면** — 커밋 목록 주소는 …/k8s-study-gitops/commits/branch/main · SYNC 뒤에도 2.0 이면 REFRESH 부터.

### ⑤ 고장 13 — 고쳐도 되돌아간다

Argo CD 에서 자동 동기화와 Self Heal 을 켠 뒤, kubectl 로 demo 를 5개로 늘려 봅니다.

```powershell
# Argo CD → DETAILS → SYNC POLICY → ENABLE AUTO-SYNC 체크 → OK → SELF HEAL 체크 → OK
kubectl scale deploy demo --replicas=5
kubectl get deploy demo -w                          # 5 → 다시 2 · Ctrl+C
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy demo -w
demo   2/5     2            2           20m
demo   2/2     2            2           20m      ← 2초 만에 되돌아감 (EKS 실측)
```

**확인** — 누가 되돌렸는지(Argo CD 의 Self Heal)와, 5개가 필요하면 어디를 고치는지(git 의 values-dev.yaml · replicaCount) 말할 수 있으면 성공.

**막히면** — 안 돌아오면 SELF HEAL 이 꺼진 것 · SYNC STATUS 의 Synced 아래 작은 글씨가 Auto sync is enabled. 여야 함.

### 정리 — 쉬는 시간 전에

탭 ① 의 반복은 Ctrl+C 로 끕니다. demo 는 Argo CD 가 계속 맡은 채로 둡니다.

```powershell
kubectl get deploy
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy
NAME   READY   UP-TO-DATE   AVAILABLE   AGE
demo   2/2     2            2           15m      ← 2/2 = git 의 replicaCount 2 (판은 ④ Revert 대로 1.0)
```

**확인** — demo 2/2 만 있으면 됩니다. 이제 demo 를 kubectl 로 고쳐도 되돌아갑니다.

---

## 실습 16. 종합 수리 — `labs/16-fix-it`

누군가 배포한 앱 `shop` 이 밖에서 열리지 않습니다. **두 군데**가 망가져 있습니다. 힌트는 없습니다.

**시작**

```powershell
kubectl apply -f labs/16-fix-it/shop.yaml
```

`shop.yaml` 한 파일에 컨피그맵 `shop-config` · 디플로이먼트 `shop` · 서비스 `shop` · 인그레스 `shop` 네 개가 `---` 로 나뉘어 들어 있습니다. apply 한 번에 다 만들어집니다.

**목표** — 아래 명령이 보여 주는 HOSTS 칸의 주소(`shop.` 으로 시작)를 브라우저 주소창에 그대로 붙여 넣었을 때 `greeting : welcome to shop` 이 나오면 성공입니다.

```powershell
kubectl get ingress shop                  # HOSTS 칸의 주소를 브라우저 주소창에
```

**진단 순서** — 오늘 쓴 순서 그대로입니다. 하나를 고치면 다음 것이 드러납니다.

1. `kubectl get pods,ingress` — 상태 보기 (`STATUS`, `READY`, `RESTARTS`)
2. `kubectl describe pod -l app=shop` — 맨 아래 `Events` (Pending 이면 이유, 0/1 이면 프로브)
3. `kubectl logs -l app=shop` — 앱이 남긴 말 (죽었다면 `--previous`)
4. 인그레스가 의심되면 `kubectl describe ingress shop` 의 Backends

고친 것은 `shop.yaml` 에 반영하고 다시 apply 합니다.

**정리**

```powershell
kubectl delete -f labs/16-fix-it/shop.yaml
```

---

## 뒤처졌을 때

TODO 를 채운 완성본이 `solutions/` 에 있습니다(실습 11 · 12). 앞 실습을 못 끝냈으면 그 실습의 완성본을 apply 하거나, 2회차 끝 모습부터 다시 맞추고 넘어갑니다.

```powershell
# demo · 서비스 · 입구 · curl 파드가 없을 때 (실습 14 전까지)
kubectl apply -f labs/09-rollout/deployment.yaml -f solutions/03-service -f solutions/06-ingress

# 실습 11 의 완성본 (readiness · startupProbe)
kubectl apply -f solutions/11-probe
```

실습 14 부터 demo 는 Argo CD 가 맡습니다. 그 뒤에 demo 가 없거나 이상하면 Argo CD 화면에서 SYNC 를 누릅니다.

## 고장 났을 때 보는 순서

`get` → `describe` 의 맨 아래 Events(다시 시작하면 Last State) → `logs` (`--previous`) → 그래프(Grafana)

| 상태 | 뜻 | 먼저 볼 곳 |
|---|---|---|
| READY `0/1` 인데 `Running` | 떴지만 준비 확인(readiness)이 실패 — 명단 밖 | `kubectl describe pod` 의 `Readiness probe failed` 줄 (주소 · 포트) |
| RESTARTS 가 계속 늘어남 | 살아 있나(liveness)가 실패해 다시 시작 | `Liveness probe failed` · `Killing` 줄, `logs --previous` — 기동이 느리면 startupProbe |
| `rollout status` 가 Waiting… 에서 멈춤 | 새 파드가 준비되지 않아 다음 칸으로 못 감 | 새 파드의 Events, 먼저 `kubectl rollout undo` |
| 파드 `Pending` | 갈 노드가 없음 | `kubectl describe pod` 의 Events — `didn't match … selector`(라벨) · `untolerated taint`(테인트) · `Insufficient`(자리) |
| Argo CD `OutOfSync` | git 과 클러스터가 다름 | REFRESH → APP DIFF → SYNC |
| Argo CD `Degraded` · `Progressing` 이 오래 감 | 파드가 준비되지 않음 | 리소스 트리에서 파드를 눌러 EVENTS · LOGS |
| 손으로 고친 것이 되돌아감 | 자동 동기화 · Self Heal 이 git 으로 맞춤 | git(Gitea)에서 고치기 |

## 끝나고

스터디의 마지막 회차입니다. 실습 클러스터는 스터디가 끝나면 통째로 지웁니다. 접속 파일(`k8s-study/kubeconfig`)과 `accounts.txt` 는 남에게 보내지 않습니다.

접속 설정을 지워 원래 쓰던 쿠버네티스 설정으로 돌아갑니다.

- Windows — `[Environment]::SetEnvironmentVariable("KUBECONFIG", $null, "User")` 를 치고 터미널을 새로 엽니다.
- Mac — `~/.zshrc` 의 `export KUBECONFIG=~/k8s-study/kubeconfig` 줄을 지웁니다.
