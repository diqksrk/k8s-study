# 실습 가이드 — 쿠버네티스 스터디 3회차

2회차까지 만든 내 칸 · 내 앱(demo)에 이어서, 앱이 일하는지 묻고(프로브), 파드가 갈 노드를 고르고(스케줄링), 지난 일을 보고(로그 · 모니터링), git 으로 배포하고(Argo CD), 새 판을 조금씩 내보냅니다(Argo Rollouts). 실습 번호는 2회차에 이어서 11번부터입니다. 실습마다 일부러 망가뜨린 단계(고장)가 있습니다. 원인을 찾아 고쳐 보세요.

| 실습 | 폴더 | 하는 일 |
|---|---|---|
| 0. 준비 | — | 새 압축 풀기, 2회차 끝 모습으로 맞추기, 지켜보기 켜기 |
| 11. 프로브 · 끊김 없는 교체 | `labs/11-probe` | readiness, 고장 9, FAIL 없이 2.0 으로, 고장 10, startupProbe |
| 12. 스케줄링 | `labs/12-schedule` | 노드 라벨, AZ 마다 하나씩, 고장 11 · 12 (갈 곳 없는 파드 · 들어갈 수 없는 노드) |
| 13. 로그 · 모니터링 | `labs/13-observe` | 지금 로그 · 죽기 전 로그, 이벤트, Grafana, Prometheus |
| 14. GitOps — Argo CD | `labs/14-gitops` | 브라우저로 커밋해 배포, git 으로 되돌리기, 고장 13 |
| 15. 조금씩 바꾸기 — Argo Rollouts | `labs/15-canary` | 25% 만 먼저, promote, 고장 14 |
| 16. 종합 수리 | `labs/16-fix-it` | 밖에서 열리지 않는 shop 고치기 (힌트 없음) |

## 실습 0. 준비

받는 것은 1 · 2회차와 같습니다. **3회차 `k8s-study.zip`** 을 1:1 메시지로 받고(2회차 압축에 3회차 실습 파일 · 내 계정 파일 `accounts.txt` · 카나리 도구 `kubectl-argo-rollouts` 가 더해진 것 · 접속 파일은 같음), 이 실습 가이드를 받습니다. 앞 회차에 오지 않았어도 아래 명령 그대로 하면 됩니다.

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
kubectl argo rollouts version
kubectl get pods
```

**Mac** — 터미널에서 한 줄씩. 첫 줄은 이 가이드의 명령을 줄 끝 `# 설명` 까지 붙여 넣어도 에러가 나지 않게 하는 설정입니다(Mac 기본 터미널은 `#` 뒤를 설명으로 보지 않습니다). 그래서 첫 줄부터 칩니다.

```bash
setopt interactivecomments; echo 'setopt interactivecomments' >> ~/.zshrc
unzip -oq ~/Downloads/k8s-study.zip -d ~                          # -o 는 2회차 폴더를 묻지 않고 덮어쓰기
echo 'export KUBECONFIG=~/k8s-study/kubeconfig' >> ~/.zshrc; export KUBECONFIG=~/k8s-study/kubeconfig
brew install kubectl argoproj/tap/kubectl-argo-rollouts           # 이미 있으면 그대로
kubectl argo rollouts version
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts version
kubectl-argo-rollouts: v1.10.1+…
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
kubectl get pods -l app=demo -o wide                # NODE 칸
```

나와야 할 화면 (예시)

```text
PS> kubectl get nodes -L topology.kubernetes.io/zone,node.kubernetes.io/instance-type,role
NAME                       STATUS   …   ZONE              INSTANCE-TYPE   ROLE
ip-192-168-64-145.…        Ready    …   ap-northeast-2a   t3.medium
ip-192-168-58-30.…         Ready    …   ap-northeast-2c   t3.medium
ip-192-168-11-197.…        Ready    …   ap-northeast-2d   t3.medium
ip-192-168-70-12.…         Ready    …   ap-northeast-2a   t3.large        tools
```

**확인** — 노드 4대 중 ROLE 이 tools 인 한 대가 보이면 됩니다. 내 demo 는 tools 가 아닌 세 대 어딘가에 있습니다.

**막히면** — Forbidden 이 나오면 접속 파일(KUBECONFIG)이 3회차 압축의 것인지 봅니다.

### ② AZ 마다 하나씩

spread.yaml 의 TODO 를 topology.kubernetes.io/zone 으로 고쳐 apply 하고 노드를 봅니다.

```powershell
kubectl apply -f labs/12-schedule/spread.yaml
kubectl get pods -l app=spread -o wide              # NODE 칸 · AZ 셋에 하나씩
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

**막히면** — 셋 다 Pending 이면 TODO 그대로 — Events 에 missing required label · 지우고 고쳐서 다시 apply.

### ③ 고장 11 — 갈 노드가 없다

파드 하나를 띄우고, Pending 인 이유를 describe 의 Events 에서 읽습니다.

```powershell
kubectl apply -f labs/12-schedule/pending.yaml
kubectl get pod picky                               # Pending
kubectl describe pod picky                          # 맨 아래 Events
kubectl delete pod picky                            # 고쳐서 뜨는 것까지 봤으면 (자리 아끼기)
```

나와야 할 화면 (예시)

```text
PS> kubectl get pod picky
picky   0/1     Pending   0          5s
PS> kubectl describe pod picky
  Warning  FailedScheduling  5s  default-scheduler  0/4 nodes are available:
           1 node(s) had untolerated taint(s), 3 node(s) didn't match Pod's node affinity/selector.
```

**확인** — 노드 4대가 각각 왜 안 되는지 나뉘어 나옵니다. 3대는 라벨이 안 맞고, 1대는 테인트 — 그 라벨을 가진 노드가 없다고 말할 수 있으면 성공.

**막히면** — 고치기: disktype: ssd → topology.kubernetes.io/zone: ap-northeast-2a 로 고치고 delete 후 apply.

### ④ 고장 12 — 못 들어가는 노드

도구 노드의 테인트를 보고 그 노드로 가라는 파드를 띄운 뒤, tolerations 주석을 풀어 들여보냅니다.

```powershell
kubectl describe node -l role=tools                 # Taints 줄
kubectl apply -f labs/12-schedule/tools-pod.yaml
kubectl describe pod visitor                        # 맨 아래 Events
kubectl delete -f labs/12-schedule/tools-pod.yaml   # tolerations 주석을 푼 뒤 →
kubectl apply -f labs/12-schedule/tools-pod.yaml
kubectl get pod visitor -o wide                     # NODE = 도구 노드
```

나와야 할 화면 (예시)

```text
PS> kubectl describe node -l role=tools
Taints:             dedicated=tools:NoSchedule
  Warning  FailedScheduling  …  0/4 nodes are available: 1 node(s) had untolerated taint(s), 3 …
PS> kubectl get pod visitor -o wide
visitor   1/1     Running   0          8s    10.42.3.16   ip-192-168-70-12.…   ← 도구 노드
```

**확인** — 라벨(nodeSelector)은 '여기로', 테인트는 '허락받은 것만' — 둘 다 맞아야 들어간다고 말할 수 있으면 성공.

**막히면** — 주석은 네 줄을 골라 VS Code 에서 Ctrl+/ (Mac Cmd+/) · tolerations: 가 nodeSelector: 와 같은 칸이면 맞음.

### 정리 — 쉬는 시간 전에

실습 12 에서 만든 것을 지웁니다. demo · slow · slow-broken · curl 만 남깁니다.

```powershell
kubectl delete -f labs/12-schedule --ignore-not-found
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
curl                          1/1     Running            0          13d
demo-86bffd5566-h6hwr         1/1     Running            0          17m       (demo 셋)
slow-7c9d8f6b4-m3p8z          1/1     Running            0          11m
slow-broken-6d9f7c8b5-x2kq7   0/1     CrashLoopBackOff   9          14m
```

**확인** — spread · picky · visitor 가 없으면 됩니다(Terminating 은 곧 사라짐).

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
[pod/demo-86bffd5566-h6hwr/demo] … DispatcherServlet : Completed initialization in 197 ms
[pod/demo-86bffd5566-hwh85/demo] … DispatcherServlet : Completed initialization in 93 ms
PS> kubectl get pods -l app=slow-broken
slow-broken-9c7979958-bjnmz   0/1     CrashLoopBackOff   9 (73s ago)   14m
```

**확인** — --prefix 로 어느 파드의 줄인지 보이고, --previous 는 지금이 아니라 직전에 죽은 컨테이너의 로그라는 것을 알면 됩니다.

**막히면** — --previous 에 Started 줄이 없으면 그게 답(다 뜨기 전에 죽음) · Found 3 pods, using … 는 하나만 골랐다는 뜻.

### ② 이벤트 — 칸의 알림판

내 칸에서 일어난 일을 시간 순서로 보고, 경고만 골라 봅니다. 오늘 만난 고장이 다 들어 있습니다.

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

**확인** — 실습 11 · 12 의 고장이 시간 순서로 보이면 됩니다. 이벤트는 1시간이 지나면 지워집니다.

**막히면** — (x6 over 13m) 은 같은 일이 13분 동안 6번 · 오래된 줄이 안 보이면 1시간이 지난 것.

### ③ Grafana — 지난 1시간

브라우저로 Grafana 를 열어 내 칸의 CPU · 메모리 그래프를 봅니다. 로그인은 없습니다.

```powershell
http://grafana.43-201-184-138.sslip.io/             # 브라우저 주소창
# 왼쪽 메뉴 Dashboards → Kubernetes / Compute Resources / Namespace (Pods)
# 위의 namespace 칸 → k8s-study- 뒤에 accounts.txt 의 아이디가 붙은 것
# 오른쪽 위 시간 → Last 1 hour
# 같은 목록의 … / Pod → pod 칸에서 slow → CPU Throttling (뜰 때의 봉우리)
```

나와야 할 화면 (예시)

```text
브라우저
CPU Usage      — 파드마다 선 하나 · 실습 11 에서 demo 를 바꿀 때마다 튄 자리
Memory Usage   — demo 셋이 120~200Mi 근처
CPU Throttling — slow 가 뜨는 동안 한도(150m)에 눌린 봉우리 · 다 뜬 뒤엔 0 근처
```

**확인** — kubectl top 은 지금 값 하나뿐인데, 여기는 지난 1시간이 선으로 남아 있다는 것을 보면 됩니다.

**막히면** — 그래프가 비면 namespace 칸 · 시간을 다시 · 안 열리면 회사 망 — 휴대폰 데이터로 같은 주소.

### ④ Prometheus 한 줄

Prometheus 화면에 쿼리를 한 줄씩 넣어 다시 시작 횟수와 CPU 눌림을 숫자로 봅니다.

```powershell
http://prom.43-201-184-138.sslip.io/                # 브라우저 · 위 칸에 한 줄 넣고 Execute
kube_pod_container_status_restarts_total{container="slow-broken"}
container_cpu_cfs_throttled_periods_total{container="slow"}
```

나와야 할 화면 (예시)

```text
브라우저 · Table
kube_pod_container_status_restarts_total{…namespace="k8s-study-host", pod="slow-broken-…"}   9
container_cpu_cfs_throttled_periods_total{…pod="slow-…"}   412   ← 뜬 뒤 지금까지 눌린 횟수(누계)
```

**확인** — 모든 사람의 칸이 한 표에 나옵니다. 내 칸(namespace)의 줄을 찾아 숫자를 읽으면 됩니다.

**막히면** — Graph 탭으로 바꾸면 선으로 · 결과가 없으면 따옴표가 큰따옴표(")인지.

### ⑤ 부하를 걸면 (먼저 끝났으면)

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
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
curl                    1/1     Running   0          13d
demo-86bffd5566-h6hwr   1/1     Running   0          30m       (demo 셋)
```

**확인** — demo 셋과 curl 만 남으면 됩니다.

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
kubectl get deploy,svc,ingress demo                 # git 의 세 파일 그대로
```

나와야 할 화면 (예시)

```text
Argo CD
APP HEALTH ♥ Healthy     SYNC STATUS ✓ Synced to main (2bbafa1)
나무: host-demo → deploy demo → rs → pod 2 · svc demo · ing demo
PS> kubectl get deploy,svc,ingress demo
deployment.apps/demo   2/2     2            2           1m       ← git 의 replicas 2
```

**확인** — Synced · Healthy 가 되고 demo 가 2/2 면 됩니다. Healthy 는 Argo CD 가 우리 readiness 를 읽은 결과입니다.

**막히면** — Progressing 이 오래면 파드가 뜨는 중(1분 안팎) · 나무에서 파드를 누르면 EVENTS · LOGS 탭.

### ③ git 으로 2.0 — 브라우저에서 커밋

탭 ① 에 지켜보기(첫 줄)를 켜고, Gitea 에서 이미지 1.0 → 2.0 커밋 뒤 Argo CD 에서 맞춥니다.

```powershell
kubectl exec -it curl -- sh -c 'while true; do curl -s -m1 demo/ | grep ver || echo FAIL; sleep 0.5; done'
# Gitea → k8s-study-gitops → app → deployment.yaml → 연필(파일 수정)
# image 줄의 1.0 → 2.0 → 맨 아래 '변경 내용을 커밋' (main 브랜치에 직접)
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

**확인** — 커밋만 했는데 2.0 이 되고 FAIL 이 없으면 됩니다. 실습 11 의 readiness · preStop 이 git 파일에 들어 있어서입니다.

**막히면** — REFRESH 뒤에도 Synced 면 커밋이 안 된 것(Gitea 의 커밋 목록) · 기다리면 30초 안에 저절로 OutOfSync.

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
Revert "app/deployment.yaml 업데이트"   ← 되돌리는 새 커밋 (기록이 남음)
app/deployment.yaml 업데이트            ← ③ 의 커밋 (Gitea 가 붙인 기본 메시지)
탭 ①
version  : 2.0  …  version  : 1.0
```

**확인** — 탭 ① 이 1.0 으로 돌아오고, git 기록에 '되돌렸다' 는 커밋이 하나 더 남으면 됩니다.

**막히면** — 2회차 rollout undo 는 클러스터만 1.0 · 파일은 2.0 이었습니다. 오늘은 git 과 클러스터가 같습니다.

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

**확인** — 누가 되돌렸는지(Argo CD 의 Self Heal)와, 그럼 5개가 필요하면 어디를 고쳐야 하는지(git) 말할 수 있으면 성공.

**막히면** — 되돌아가지 않으면 SELF HEAL 이 꺼져 있는 것 · Argo CD 화면 맨 위에 Auto sync is enabled 가 보여야 함.

### 정리 — 쉬는 시간 전에

탭 ① 의 반복은 Ctrl+C 로 끕니다. demo 는 Argo CD 가 계속 맡은 채로 둡니다.

```powershell
kubectl get deploy
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy
NAME   READY   UP-TO-DATE   AVAILABLE   AGE
demo   2/2     2            2           15m      ← 1.0 · git 이 원하는 상태
```

**확인** — demo 2/2 만 있으면 됩니다. 이제 demo 를 kubectl 로 고쳐도 되돌아갑니다.

---

## 실습 15. 조금씩 바꾸기 — Argo Rollouts — `labs/15-canary`

디플로이먼트 대신 Argo Rollouts 의 Rollout 으로 새 판을 4개 중 1개에만 먼저 내보내고, 보고 나서 나머지를 바꿉니다. 실습 14 의 demo 와 섞이지 않게 이름이 canary 입니다. 탭을 두 개 씁니다(① 지켜보기 · ② 명령).

### ① 도구 확인

카나리 도구(kubectl 의 argo rollouts 플러그인)가 되는지 봅니다.

```powershell
kubectl argo rollouts version
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts version
kubectl-argo-rollouts: v1.10.1+…
```

**확인** — 판 번호가 나오면 됩니다.

**막히면** — unknown command 면 bin 의 kubectl-argo-rollouts.exe 가 없는 것 · Mac 은 실습 0 ① 의 brew 줄.

### ② 카나리 띄우기

Rollout canary 1.0 × 4 를 띄우고, 탭 ① 에서 지켜보기를 canary 로 켭니다(마지막 줄).

```powershell
kubectl apply -f labs/15-canary/rollout.yaml
kubectl argo rollouts get rollout canary            # Healthy 가 될 때까지 몇 번
kubectl exec -it curl -- sh -c 'while true; do curl -s -m1 canary/ | grep ver || echo FAIL; sleep 0.5; done'
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts get rollout canary
Status:          ✔ Healthy          Step: 4/4     SetWeight: 100
NAME                                KIND        STATUS     AGE    INFO
└──# revision:1
   └──⧉ canary-59699447d9           ReplicaSet  ✔ Healthy  105s   stable
      ├──□ canary-59699447d9-2sh78  Pod         ✔ Running  105s   ready:1/1    (파드 넷)
```

**확인** — Status 가 Healthy 이고 탭 ① 에 version : 1.0 이 흐르면 됩니다.

**막히면** — Progressing 이면 파드가 뜨는 중 · 탭 ① 의 FAIL 은 서비스 canary 가 아직 비어서.

### ③ 25% 만 새 판으로

rollout.yaml 의 1.0 을 2.0 으로 고쳐 apply 하고, 4개 중 1개만 2.0 인 채 멈춘 것을 봅니다.

```powershell
kubectl apply -f labs/15-canary/rollout.yaml            # 1.0 → 2.0 고친 뒤
kubectl argo rollouts get rollout canary
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts get rollout canary
Status:          ॥ Paused          Message: CanaryPauseStep
Step:            1/4     SetWeight: 25     ActualWeight: 25     Updated: 1 · Current: 4
├──# revision:2   ⧉ canary-6ff9dfbcd8   ✔ Healthy   canary    ← 2.0 × 1
└──# revision:1   ⧉ canary-59699447d9   ✔ Healthy   stable    ← 1.0 × 3
탭 ①
version : 1.0 · 1.0 · 1.0 · 2.0 …     ← 넷에 하나꼴로 2.0 · FAIL 없음
```

**확인** — Paused · 1/4 · 2.0 하나 · 1.0 셋(모두 4개)이면 됩니다. 멈춘 동안 새 판을 보고 승인할지 정합니다.

**막히면** — unchanged 면 저장 안 함 · Progressing 이 길면 새 파드가 뜨는 중.

### ④ promote — 승인

승인하면 50% → 30초 → 100% 로 저절로 갑니다. 끝까지 지켜봅니다.

```powershell
kubectl argo rollouts promote canary
kubectl argo rollouts get rollout canary --watch    # Healthy 까지 · Ctrl+C
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts promote canary
rollout 'canary' promoted
Step: 2/4 (SetWeight 50) → 3/4 (30초) → 4/4
Status:          ✔ Healthy      ← 2.0 × 4 가 이제 stable
```

**확인** — Healthy 가 되고 탭 ① 이 2.0 만이면 됩니다.

### ⑤ 고장 14 — 멈춘 카나리

2회차 고장 8 과 같은 새 판을 카나리로 내보냅니다. 60초 뒤의 상태를 보고 abort 합니다.

```powershell
kubectl apply -f labs/15-canary/rollout-broken.yaml
kubectl argo rollouts get rollout canary            # 60초 뒤 다시
kubectl argo rollouts abort canary
kubectl apply -f labs/15-canary/rollout.yaml        # 2.0 판으로 되돌려 Healthy
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts get rollout canary
Status:   ✖ Degraded     Step: 0/4     SetWeight: 25     ActualWeight: 0
Message:  ProgressDeadlineExceeded: ReplicaSet "canary-656b7c6678" has timed out progressing.
├──# revision:3   ⧉ canary-656b7c6678   ◌ Progressing   canary   ← 2.0+9090 · ready:0/1
├──# revision:2   ⧉ canary-6ff9dfbcd8   ✔ Healthy       stable   ← 2.0 × 3
탭 ①  version : 2.0 만 · FAIL 없음
```

**확인** — 새 판 파드가 준비되지 않아 25% 에서 멈추고, 60초 뒤 Degraded 가 되며, 그동안 옛 판(셋)만 일했다고 말할 수 있으면 성공.

**막히면** — 원인은 새 판 파드의 describe(Readiness probe failed) · abort 뒤 Degraded 는 마지막 줄 apply 로 풂.

### ⑥ 대시보드 (먼저 끝났으면)

같은 도구로 브라우저 화면을 엽니다. 단계 막대와 Promote · Abort 버튼이 있습니다.

```powershell
kubectl argo rollouts dashboard                     # 끌 때 Ctrl+C
# 브라우저  http://localhost:3100/rollouts → 위 NAMESPACE 칸에 내 칸 → canary
```

나와야 할 화면 (예시)

```text
PS> kubectl argo rollouts dashboard
INFO[0000] Argo Rollouts Dashboard is now available at http://127.0.0.1:3100/rollouts
```

**확인** — 내 칸의 canary 가 단계 막대와 같이 보이면 됩니다. 내 PC 에서만 열리는 화면입니다.

### 정리 — 종합 수리로 넘어가기 전에

탭 ① 의 반복은 Ctrl+C 로 끄고, 카나리를 지웁니다.

```powershell
kubectl delete -f labs/15-canary/rollout.yaml
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
curl                    1/1     Running   0          13d
demo-…                  1/1     Running   0          40m       (demo 둘 · Argo CD 가 맡음)
```

**확인** — curl 과 demo 둘만 남으면 됩니다.

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
| Rollout `Paused` | 사람이 promote 할 때까지 기다리는 단계 | `kubectl argo rollouts get rollout canary` |
| Rollout `Degraded` | 새 판이 정해진 시간 안에 준비되지 않음 | `kubectl argo rollouts abort canary` 로 옛 판만 남기고, 새 판 파드의 Events |

## 끝나고

스터디의 마지막 회차입니다. 실습 클러스터는 스터디가 끝나면 통째로 지웁니다. 접속 파일(`k8s-study/kubeconfig`)과 `accounts.txt` 는 남에게 보내지 않습니다.

접속 설정을 지워 원래 쓰던 쿠버네티스 설정으로 돌아갑니다.

- Windows — `[Environment]::SetEnvironmentVariable("KUBECONFIG", $null, "User")` 를 치고 터미널을 새로 엽니다.
- Mac — `~/.zshrc` 의 `export KUBECONFIG=~/k8s-study/kubeconfig` 줄을 지웁니다.
