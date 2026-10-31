# 실습 가이드 — 쿠버네티스 스터디 1회차

작은 Spring Boot 앱 하나를 파드로 띄우고, 디플로이먼트로 개수를 지키고, 서비스로 이름을 붙이고, 컨피그맵 · 시크릿으로 설정을 넣습니다. 실습마다 끝에 일부러 망가뜨린 파일이 하나씩 있습니다. 원인을 찾아 고쳐 보세요.

| 실습 | 폴더 | 하는 일 |
|---|---|---|
| 0. 준비 · 접속 | — | kubectl 확인, 저장소 받기, 접속 파일, 내 칸에 접속 |
| 1. 파드 | `labs/01-pod` | 띄우고 들여다보기, Events 읽기, 고장 1 |
| 2. 디플로이먼트 | `labs/02-deployment` | 지워도 다시 생기는 파드, 레이블, 없는 버전으로 바꾸기 |
| 3. 서비스 | `labs/03-service` | 서비스 이름으로 부르기, 파드를 갈아엎어도 그대로, 고장 2 |
| 4. 컨피그맵 · 시크릿 | `labs/04-config` | 설정만 바꿔서 앱 바꾸기, 시크릿은 숨겨지지 않는다, 고장 3 · 4 |
| 5. 종합 수리 | `labs/05-fix-it` | 두 군데 망가진 앱 고치기 (힌트 없음) |

## 실습 0. 준비 · 접속

내 PC 에서 클러스터의 내 칸(네임스페이스 `k8s-study-<아이디>`)에 붙는 데까지 합니다. 받을 것은 두 가지, **kubectl** 과 1:1 로 받은 **접속 파일 `<아이디>.kubeconfig`** 입니다. AWS 계정은 필요 없습니다. 접속 파일에는 내 칸에 들어가는 열쇠(토큰)가 들어 있으니 다른 사람에게 넘기지 않습니다.

### ① kubectl 준비

스터디 전에 [접속 가이드](docs/접속가이드.md) 대로 설치해 두었으면 확인만 합니다.

- Windows — 받은 `kubectl.exe` 를 접속 가이드 1번대로 `$HOME\bin` 에 넣고, **터미널을 닫았다가 새로 엽니다**.
- Mac — `brew install kubectl`. 그리고 한 번만 `echo 'setopt interactivecomments' >> ~/.zshrc` 를 치고 터미널을 새로 엽니다. 이 가이드의 명령을 줄 끝 `# 설명` 까지 붙여 넣어도 에러가 나지 않게 하는 설정입니다(Mac 기본 터미널은 `#` 뒤를 설명으로 보지 않습니다).

```powershell
kubectl version --client
```

나와야 할 화면 (예시)

```text
Client Version: v1.34.13
Kustomize Version: v5.7.1
```

**확인** — `Client Version` 줄이 나오면 됩니다. 버전 숫자는 조금 달라도 괜찮습니다.

**막히면** — `'kubectl' 용어가 ... 인식되지 않습니다` 는 설치 뒤에 터미널을 새로 열지 않은 경우입니다.

### ② 저장소 받고 접속 파일 쓰기

1:1 로 받은 접속 파일을 다운로드 폴더에 두고, 실습 저장소를 받아 그 폴더로 들어갑니다.

```powershell
cd ~                                               # 홈 폴더에서
git clone https://github.com/diqksrk/k8s-study.git
cd k8s-study                                       # 오늘 명령은 전부 이 폴더에서 칩니다
# 받은 접속 파일 쓰기 · 내 PC 에 맞는 한 줄만 · <아이디> 는 내 아이디로 · 터미널 창 · 탭마다 한 번
export KUBECONFIG=~/Downloads/<아이디>.kubeconfig           # Mac
$env:KUBECONFIG="$HOME\Downloads\<아이디>.kubeconfig"       # Windows (PowerShell)
```

나와야 할 화면 (예시)

```text
PS> cd ~
PS> git clone https://github.com/diqksrk/k8s-study.git
Cloning into 'k8s-study'...
PS> cd k8s-study
PS> ls
HANDS_ON.md  README.md  demo-app  docs  labs  solutions   ← 이렇게 보이면 맞는 폴더 (Windows 는 표 모양)
```

**확인** — ls 에 labs · docs · HANDS_ON.md 가 보이면 맞는 폴더입니다. ③ 도 이 창에서 칩니다.

**막히면** — Mac 은 먼저 setopt interactivecomments 한 줄 · git 이 없으면 Code → Download ZIP · 새 창은 KUBECONFIG 줄부터.

실습 명령은 전부 **이 폴더(`k8s-study`) 맨 위**에서 칩니다. `labs` 안으로 더 들어가지 않습니다. `KUBECONFIG` 줄은 지금 연 터미널 창에만 적용되니, **창 · 탭을 새로 열면 다시** 칩니다. ([접속 가이드](docs/접속가이드.md) 대로 `~/.kube/config` 에 미리 넣어 두었다면 이 줄은 건너뜁니다.) Mac 에서 `parse error near '>'` 가 나오면 `<아이디>` 를 내 아이디로 바꾸지 않은 것입니다.

### ③ 내 칸에 접속해 보기

내 접속 파일로 클러스터에 붙어서, 내 칸과 권한을 확인합니다.

```powershell
kubectl config view --minify          # namespace 줄이 내 칸
kubectl get pods                      # 내 칸은 아직 비어 있음
kubectl get nodes                     # 노드(서버)는 보기만 됨
```

나와야 할 화면 (예시)

```text
PS> kubectl config view --minify
    namespace: k8s-study-user1
PS> kubectl get pods
No resources found in k8s-study-user1 namespace.
PS> kubectl get nodes
NAME                                                STATUS   ROLES    AGE   VERSION
ip-192-168-11-197.ap-northeast-2.compute.internal   Ready    <none>   6m    v1.34.12-eks-577a51e
ip-192-168-58-30.ap-northeast-2.compute.internal    Ready    <none>   6m    v1.34.12-eks-577a51e
ip-192-168-64-145.ap-northeast-2.compute.internal   Ready    <none>   6m    v1.34.12-eks-577a51e
```

**확인** — namespace 가 내 칸이고, get pods 는 비어 있고, 노드 3대가 Ready 면 정상입니다.

**막히면** — Unable to connect · Unauthorized 가 나오면 접속 파일 문제입니다. docs/접속가이드.md 의 '자주 막히는 곳'.

- 터미널은 Windows 는 **Windows Terminal + PowerShell**, Mac 은 기본 **터미널**입니다. 새 탭은 Windows `Ctrl+Shift+T`, Mac `Cmd+T`.
- YAML 의 `# TODO` 줄은 VS Code 같은 편집기로 고칩니다(메모장은 들여쓰기가 깨지기 쉽습니다).
- 내 PC 에서 curl 을 칠 때 Windows 는 **`curl.exe`**, Mac 은 `curl` 입니다. `curl.exe` 는 Windows 10 부터 기본으로 들어 있는 프로그램이고, PowerShell 에서 그냥 `curl` 은 다른 명령(Invoke-WebRequest)의 별칭입니다.
- 명령 칸은 **한 줄씩** 칩니다. 여러 줄을 한 줄에 이어 붙이면 `unknown shorthand flag` 같은 에러가 납니다.

**읽는 법** — 단계마다 **할 일 → 명령 → 나와야 할 화면 → 확인** 순서입니다. 나와야 할 화면은 예시라서 파드 이름 · IP · 시각은 사람마다 다릅니다. **(먼저 끝났으면)** 이 붙은 단계는 건너뛰어도 다음 단계에 지장이 없습니다. 자주 쓰는 명령은 [치트시트](docs/치트시트.md) 에 있습니다.

---

## 실습 1. 파드 — `labs/01-pod`

파드 하나를 띄우고 안팎을 들여다본 다음, 일부러 고장 내 봅니다.

### ① 띄우기

파드 하나를 띄우고, Running 이 될 때까지 지켜봅니다.

```powershell
kubectl apply -f labs/01-pod/pod.yaml
kubectl get pods -o wide -w          # -o wide 는 IP · NODE 열까지, -w 는 계속 지켜보기 · Ctrl+C 로 끝
```

나와야 할 화면 (예시)

```text
PS> kubectl apply -f labs/01-pod/pod.yaml
pod/demo-pod created
PS> kubectl get pods -o wide -w
NAME       READY   STATUS              RESTARTS   AGE   IP              NODE                              …
demo-pod   0/1     ContainerCreating   0          2s    <none>          ip-192-168-41-12.ap-northeast-2…
demo-pod   1/1     Running             0          9s    192.168.41.23   ip-192-168-41-12.ap-northeast-2…
```

**확인** — STATUS 가 Running 이 되면 Ctrl+C. IP 열이 이 파드의 주소(podIP), NODE 열이 올라간 노드입니다.

**막히면** — 'path … does not exist' 면 k8s-study 맨 위 폴더가 아닙니다 · ContainerCreating 몇 초는 이미지 받는 중.

### ② 누가 띄웠나 — Events

describe 맨 아래 Events 에서 일한 컴포넌트의 이름을 찾습니다.

```powershell
kubectl describe pod demo-pod          # 출력 맨 아래 Events 를 위에서부터
```

나와야 할 화면 (예시)

```text
PS> kubectl describe pod demo-pod
...
Events:
  Type    Reason     Age   From               Message
  Normal  Scheduled  40s   default-scheduler  Successfully assigned k8s-study-user1/demo-pod to ip-…
  Normal  Pulling    39s   kubelet            spec.containers{demo}: Pulling image "…/k8s-study/demo-app:1.0"
  Normal  Pulled     33s   kubelet            spec.containers{demo}: Successfully pulled image … in 3.9s …
  Normal  Created    33s   kubelet            spec.containers{demo}: Created container: demo
  Normal  Started    32s   kubelet            spec.containers{demo}: Started container demo
```

**확인** — From 열에 default-scheduler 와 kubelet 이 보이면 됩니다. 아키텍처 그림의 ④ 스케줄러 · ⑤ kubelet 입니다.

**막히면** — 메시지 앞 spec.containers{demo}: 는 '파드 안의 demo 컨테이너' 라는 표시입니다 · 맨 아래 Events 만 봅니다.

### ③ 로그 보고 접속하기

앱 로그를 보고, 내 PC 에서 파드로 통로를 열어 앱 화면을 봅니다.

```powershell
kubectl logs demo-pod
kubectl port-forward pod/demo-pod 18080:8080      # 켜 둔 채로 둡니다
curl.exe -s http://localhost:18080/                # 새 터미널 탭에서 · Windows
curl -s http://localhost:18080/                    # 새 터미널 탭에서 · Mac
```

나와야 할 화면 (예시)

```text
PS> kubectl logs demo-pod
…  Tomcat started on port 8080 (http) with context path '/'
PS> kubectl port-forward pod/demo-pod 18080:8080
Forwarding from 127.0.0.1:18080 -> 8080
PS> curl.exe -s http://localhost:18080/            (새 탭)
pod      : demo-pod
version  : 1.0
profile  : (not set)              …  stage · greeting · password · started 줄이 이어서
```

**확인** — profile : (not set) 이면 정상입니다(앱 기본값). 다 봤으면 port-forward 탭에서 Ctrl+C.

**막히면** — curl.exe 는 Windows 10 부터 들어 있는 기본 프로그램(따로 안 받음) · 새 탭에서 · 18080 이 이미 쓰이면 28080:8080.

### ③ 안으로 들어가기

컨테이너 안에 들어가 명령을 쳐 봅니다.

```powershell
kubectl exec -it demo-pod -- sh
hostname
env | grep APP
exit
```

나와야 할 화면 (예시)

```text
PS> kubectl exec -it demo-pod -- sh
# hostname
demo-pod
# env | grep APP
#                       ← 아무것도 안 나옵니다 (실습 4 에서 생깁니다)
# exit
PS>
```

**확인** — hostname 이 파드 이름과 같고, APP_ 로 시작하는 환경변수는 아직 없습니다.

**막히면** — 프롬프트가 안 보이면 Enter 한 번 · 'Unable to use a TTY' 면 Git Bash 말고 PowerShell 에서.

### ④ spec 과 status (먼저 끝났으면)

내가 쓴 것(spec)과 쿠버네티스가 본 것(status)을 나눠 봅니다.

```powershell
kubectl get pod demo-pod -o yaml          # spec: 과 status: 두 곳만 찾습니다
kubectl explain pod.spec.containers.ports
```

나와야 할 화면 (예시)

```text
spec:
  containers:
  - image: …/k8s-study/demo-app:1.0
  ...
  restartPolicy: Always                ← 내가 안 쓴 값이 기본값으로
status:
  phase: Running
  podIP: 192.168.41.23
```

**확인** — restartPolicy 처럼 내가 안 쓴 값은 기본값으로 채워집니다. status 는 내가 쓰는 곳이 아닙니다.

### ⑤ 지우면?

파드를 지우고, 다시 생기는지 봅니다.

```powershell
kubectl delete pod demo-pod
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl delete pod demo-pod
pod "demo-pod" deleted from k8s-study-user1 namespace
PS> kubectl get pods
No resources found in k8s-study-user1 namespace.
```

**확인** — 다시 생기지 않습니다. 파드 혼자서는 다시 살아나지 않습니다 → 실습 2 에서 풉니다.

### ⑥ 고장 1 — 뜨지 않는 파드

고장 난 파드를 띄우고, 원인을 찾아 고칩니다.

```powershell
kubectl apply -f labs/01-pod/pod-broken.yaml
kubectl get pods -w                              # STATUS 를 보고 Ctrl+C
kubectl describe pod demo-broken                 # 실마리는 맨 아래에
kubectl delete -f labs/01-pod/pod-broken.yaml    # 고쳐서 Running 을 본 뒤 정리
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -w
NAME          READY   STATUS              RESTARTS   AGE
demo-broken   0/1     ContainerCreating   0          1s
demo-broken   0/1     ErrImagePull        0          4s
demo-broken   0/1     ImagePullBackOff    0          17s
```

**확인** — pod-broken.yaml 을 고쳐 다시 apply → Running 이면 성공. 그다음 맨 아래 명령으로 꼭 정리합니다.

**막히면** — 이미지를 받는 건 내 PC 가 아니라 노드의 kubelet 입니다.

---

## 실습 2. 디플로이먼트 — `labs/02-deployment`

'파드 3개' 를 선언해 두면 쿠버네티스가 그 수를 계속 맞춰 주는 것을 봅니다. 터미널 탭을 두 개 씁니다(지켜보기 · 명령).

### ① TODO 채우고 띄우기

deployment.yaml 의 TODO 한 줄을 채워 '파드 3개' 를 선언합니다.

```powershell
# labs/02-deployment/deployment.yaml 의  app: TODO  →  app: demo
kubectl apply -f labs/02-deployment/deployment.yaml
kubectl get deploy,rs,pods
```

나와야 할 화면 (예시)

```text
NAME                             READY   UP-TO-DATE   AVAILABLE
deployment.apps/demo             3/3     3            3
NAME                             DESIRED   CURRENT   READY
replicaset.apps/demo-6d8f9b7c5   3         3         3
NAME                       READY   STATUS    RESTARTS
pod/demo-6d8f9b7c5-x2k4p   1/1     Running   0
pod/demo-6d8f9b7c5-q7w9z   1/1     Running   0
pod/demo-6d8f9b7c5-m3n8v   1/1     Running   0
```

**확인** — 디플로이먼트 1개 → 레플리카셋 1개 → 파드 3개 Running. 파드 이름 = 레플리카셋 이름 + 임의 문자.

**막히면** — `selector` does not match template `labels` 에러는 TODO 를 안 채운 것입니다. 꼭 demo.

### ② 지워도 다시 생긴다

한 탭에서 지켜보고, 다른 탭에서 파드 하나를 지웁니다.

```powershell
kubectl get pods -w                          # 탭 ① 에 켜 둡니다
kubectl delete pod <파드 이름 하나>             # 탭 ② · 이름은 탭 ① 에서 복사
kubectl describe rs                          # 탭 ② · 누가 다시 만들었나
```

나와야 할 화면 (예시)

```text
(탭 ①)
demo-6d8f9b7c5-x2k4p   1/1   Terminating         0     3m
demo-6d8f9b7c5-h5t2r   0/1   Pending             0     0s
demo-6d8f9b7c5-h5t2r   0/1   ContainerCreating   0     0s
demo-6d8f9b7c5-h5t2r   1/1   Running             0     4s
(탭 ② · describe rs 맨 아래)
  Normal  SuccessfulCreate  5s  replicaset-controller  Created pod: demo-6d8f9b7c5-h5t2r
```

**확인** — 새 이름의 파드가 생기고, From 에 replicaset-controller 가 보입니다. 지운 파드를 다시 만든 건 레플리카셋 컨트롤러입니다.

**막히면** — 새 탭에서 'host is down' 이나 다른 칸이 보이면 그 탭에서 KUBECONFIG 줄부터 다시 (실습 준비 장).

### ③ 레이블을 바꾸면

파드 하나의 레이블을 바꿔, 레플리카셋이 무엇으로 세는지 봅니다.

```powershell
kubectl label pod <파드 이름 하나> app=other --overwrite
kubectl get pods --show-labels
kubectl delete pod -l app=other                # 보고 나서 정리
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods --show-labels
NAME                   READY   STATUS    LABELS
demo-6d8f9b7c5-h5t2r   1/1     Running   app=demo,pod-template-hash=6d8f9b7c5
demo-6d8f9b7c5-m3n8v   1/1     Running   app=demo,pod-template-hash=6d8f9b7c5
demo-6d8f9b7c5-q7w9z   1/1     Running   app=other,pod-template-hash=6d8f9b7c5    ← 빠진 파드
demo-6d8f9b7c5-w8k2d   1/1     Running   app=demo,pod-template-hash=6d8f9b7c5     ← 새로 생긴 파드
```

**확인** — 파드가 4개입니다. app=other 는 셀렉터에서 빠졌고, 레플리카셋이 app=demo 하나를 새로 만들었습니다.

**막히면** — 'already has a value' 가 나오면 --overwrite 를 빠뜨린 것입니다.

### ④ 명령으로 늘리고 파일로 되돌리기 (먼저 끝났으면)

명령으로 바꾼 개수가 파일을 apply 하면 어떻게 되는지 봅니다.

```powershell
kubectl scale deployment demo --replicas=5
kubectl get pods
kubectl apply -f labs/02-deployment/deployment.yaml
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl scale deployment demo --replicas=5
deployment.apps/demo scaled
  → get pods 에 파드 5개
PS> kubectl apply -f labs/02-deployment/deployment.yaml
deployment.apps/demo configured
  → 파드 2개가 Terminating, 다시 3개
```

**확인** — 파일을 다시 apply 하면 파일 값(3)으로 돌아갑니다. 파일을 고치고 apply 하는 게 정석입니다.

### ⑤ 없는 버전으로 바꾸면

이미지를 없는 태그(9.9)로 바꿔 apply 하고, 기존 파드가 어떻게 되는지 봅니다.

```powershell
kubectl apply -f labs/02-deployment/deployment-badtag.yaml
kubectl get pods
kubectl rollout status deployment demo                       # 끝나지 않으니 잠깐 보고 Ctrl+C
kubectl apply -f labs/02-deployment/deployment.yaml          # 되돌리기
kubectl describe deployment demo                             # 맨 아래 Events: 지금까지 개수가 바뀐 기록
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
NAME                   READY   STATUS             RESTARTS   AGE
demo-5b7d9c8f4-t6p1x   0/1     ImagePullBackOff   0          20s     ← 9.9 새 파드
demo-6d8f9b7c5-h5t2r   1/1     Running            0          6m
demo-6d8f9b7c5-m3n8v   1/1     Running            0          9m
demo-6d8f9b7c5-w8k2d   1/1     Running            0          3m
(되돌린 뒤)  1.0 파드 3개만 Running
```

**확인** — 기존 1.0 파드 3개가 그대로 Running 입니다. 되돌린 뒤 describe 맨 아래에 새 레플리카셋 0 → 1 → 0 이 보이면 끝.

**막히면** — 디플로이먼트 demo 는 실습 3 에서 그대로 씁니다. 지우지 마세요.

디플로이먼트 `demo` 는 실습 3 에서 그대로 씁니다. **지우지 마세요.**

---

## 실습 3. 서비스 — `labs/03-service`

파드 IP 는 계속 바뀌어도 서비스 이름으로는 계속 접속되는 것을 봅니다. 이번 실습의 curl 은 전부 내 PC 가 아니라 curl 파드 안에서 칩니다(명령 앞의 `kubectl exec curl --`). 서비스 이름과 ClusterIP 는 클러스터 안에서만 통하기 때문입니다. 앞의 `curl` 은 파드 이름, 뒤의 `curl` 은 그 안에서 실행할 프로그램입니다.

> 앞 실습을 못 끝냈으면 먼저: `kubectl apply -f solutions/02-deployment`

### ① TODO 채우고 띄우기

service.yaml 의 셀렉터를 채우고, 서비스 뒤에 어떤 파드가 붙었는지 봅니다.

```powershell
# labs/03-service/service.yaml 의  app: TODO  →  app: demo
kubectl apply -f labs/03-service/service.yaml -f labs/03-service/curl.yaml   # curl 파드 = 부르는 쪽
kubectl get svc demo
kubectl get endpointslices -l kubernetes.io/service-name=demo
```

나와야 할 화면 (예시)

```text
PS> kubectl get svc demo
NAME   TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
demo   ClusterIP   10.100.183.126   <none>        80/TCP    6s
PS> kubectl get endpointslices -l kubernetes.io/service-name=demo
NAME         ADDRESSTYPE   PORTS   ENDPOINTS                                     AGE
demo-qr5pp   IPv4          8080    192.168.63.161,192.168.40.27,192.168.40.144   6s
```

**확인** — ENDPOINTS 의 IP 3개가 kubectl get pods -o wide 의 파드 IP 와 같습니다.

**막히면** — ENDPOINTS 가 `<unset>` 이면 셀렉터를 잘못 쓴 것입니다 (demo-pod 가 아니라 demo).

### ② 이름으로 부르기

curl 파드 안에서 서비스 이름 demo 로 부릅니다.

```powershell
kubectl exec curl -- curl -s http://demo/
1..6 | ForEach-Object { kubectl exec curl -- curl -s http://demo/pod }             # Windows
for i in {1..6}; do kubectl exec curl -- curl -s http://demo/pod; done              # Mac
```

나와야 할 화면 (예시)

```text
PS> kubectl exec curl -- curl -s http://demo/
pod      : demo-6d8f9b7c5-m3n8v
version  : 1.0
...
PS> 1..6 | ForEach-Object { kubectl exec curl -- curl -s http://demo/pod }
demo-6d8f9b7c5-m3n8v
demo-6d8f9b7c5-h5t2r
demo-6d8f9b7c5-m3n8v
demo-6d8f9b7c5-w8k2d   …
```

**확인** — 파드 이름이 두 개 이상 섞여 나오면 성공입니다. 같은 이름이 연달아 나와도 정상입니다.

**막히면** — Uri: 를 묻거나 's' 가 모호하다는 에러 · Could not resolve host 면 내 PC 에서 친 것. 꼭  kubectl exec curl --  를 붙입니다.

### ③ 파드를 전부 갈아엎어도

파드를 모두 지운 뒤에도 같은 이름으로 접속되는지 봅니다.

```powershell
kubectl delete pods -l app=demo
kubectl get endpointslices -l kubernetes.io/service-name=demo
1..6 | ForEach-Object { kubectl exec curl -- curl -s http://demo/pod }    # Windows · 실패면 20초 뒤
for i in {1..6}; do kubectl exec curl -- curl -s http://demo/pod; done     # Mac
```

나와야 할 화면 (예시)

```text
PS> kubectl get endpointslices -l kubernetes.io/service-name=demo
NAME         ADDRESSTYPE   PORTS   ENDPOINTS                                   AGE
demo-qr5pp   IPv4          8080    192.168.47.9,192.168.53.120,192.168.62.31   4m     ← IP 가 다 바뀜
PS> 1..6 | ForEach-Object { kubectl exec curl -- curl -s http://demo/pod }
command terminated with exit code 7        ← 지운 직후: 새 파드가 아직 뜨는 중 (여러 줄)
(10~20초 뒤 다시 치면)
demo-6d8f9b7c5-p2v8n
demo-6d8f9b7c5-z4c6q   …
```

**확인** — 파드 IP 는 전부 바뀌었는데 http://demo 는 그대로 됩니다.

**막히면** — 지운 직후 몇 초 실패는 정상입니다. 새 파드의 Spring Boot 가 아직 뜨는 중입니다. 10~20초 뒤 다시.

### ④ DNS 이름과 옆 사람 서비스 (먼저 끝났으면)

짧은 이름이 어떻게 찾아지는지 보고, 옆 사람 칸의 서비스를 부릅니다.

```powershell
kubectl exec curl -- cat /etc/resolv.conf
kubectl exec curl -- curl -s http://demo.<옆 사람 네임스페이스>/pod
```

나와야 할 화면 (예시)

```text
PS> kubectl exec curl -- cat /etc/resolv.conf
search k8s-study-user1.svc.cluster.local svc.cluster.local cluster.local …
nameserver 10.100.0.10
options ndots:5
PS> kubectl exec curl -- curl -s http://demo.k8s-study-user2/pod
demo-7f4c8d9b6-k3j7m                ← 옆 사람 파드
```

**확인** — 옆 사람 파드 이름이 나옵니다. 옆 사람이 아직이면  demo.k8s-study-helper  를 부릅니다.

### ⑤ 고장 2 — 접속이 안 되는 서비스

고장 난 서비스를 띄우고, 접속이 안 되는 원인을 찾아 고칩니다.

```powershell
kubectl apply -f labs/03-service/service-broken.yaml
kubectl exec curl -- curl -sS -m 3 http://demo-broken/
# 실마리: get endpointslices · describe svc demo-broken · get pods --show-labels
```

나와야 할 화면 (예시)

```text
PS> kubectl apply -f labs/03-service/service-broken.yaml
service/demo-broken created
PS> kubectl exec curl -- curl -sS -m 3 http://demo-broken/
curl: (7) Failed to connect to demo-broken port 80 after 1 ms: Could not connect to server
command terminated with exit code 7
```

**확인** — service-broken.yaml 을 고쳐 다시 apply → curl 이 앱 화면을 돌려주면 성공.

**막히면** — 끝나면  kubectl delete -f labs/03-service/service-broken.yaml  · 서비스 demo 와 curl 파드는 지우지 마세요.

서비스 `demo` 와 파드 `curl` 은 실습 4 · 5 에서 씁니다. **지우지 마세요.**

---

## 실습 4. 컨피그맵 · 시크릿 — `labs/04-config`

이미지는 그대로 두고 설정만 바꿔서 앱 동작을 바꿉니다. 지금 우리는 `-Pprofile=dev` 처럼 빌드할 때 설정을 골라 넣지만, 여기서는 이미지를 하나만 두고 프로파일과 설정값을 실행할 때 바깥에서 넣습니다.

> 앞 실습을 못 끝냈으면 먼저: `kubectl apply -f solutions/02-deployment -f solutions/03-service`

### ① 컨피그맵 · 시크릿 만들기

설정(컨피그맵)과 비밀(시크릿)을 만들고, 시크릿이 어떻게 저장됐는지 봅니다.

```powershell
kubectl apply -f labs/04-config/configmap.yaml -f labs/04-config/secret.yaml
kubectl get secret demo-secret -o yaml                 # data 칸을 봅니다
$b64 = kubectl get secret demo-secret -o jsonpath='{.data.APP_PASSWORD}'      # Windows 두 줄
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
kubectl get secret demo-secret -o jsonpath='{.data.APP_PASSWORD}' | base64 -d; echo     # Mac 한 줄
```

나와야 할 화면 (예시)

```text
configmap/demo-config created
secret/demo-secret created
PS> kubectl get secret demo-secret -o yaml
data:
  APP_PASSWORD: czNjcjN0LXBhc3M=
    kubectl.kubernetes.io/last-applied-configuration: |      ← 조금 아래 metadata 에
      {…"stringData":{"APP_PASSWORD":"s3cr3t-pass"}…}
PS> [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
s3cr3t-pass
```

**확인** — data 는 알아보기 힘든 문자열이지만 한 줄로 원래 값이 나옵니다. 바로 아래 apply 기록에는 평문이 그대로 있습니다.

### ② 디플로이먼트에 넣기

TODO 두 줄에 이름을 채워, 컨피그맵 · 시크릿을 환경변수로 넣습니다.

```powershell
# labs/04-config/deployment.yaml 의  name: TODO  두 줄 →  demo-config ,  demo-secret
kubectl apply -f labs/04-config/deployment.yaml
kubectl get pods -w                               # 새 파드로 바뀌면 Ctrl+C
kubectl exec curl -- curl -s http://demo/
```

나와야 할 화면 (예시)

```text
PS> kubectl exec curl -- curl -s http://demo/
pod      : demo-7c9f6b8d4-r2x5k
version  : 1.0
profile  : dev
stage    : dev-cluster
greeting : hello from configmap
password : s3****
```

**확인** — profile · stage · greeting · password 네 줄이 위처럼 바뀌면 성공입니다. 이미지는 그대로입니다.

**막히면** — TODO 를 남기면 apply 가 거절됩니다(Invalid value: "TODO") · 파드가 잠깐 4~5개인 건 교체 중이라 정상.

### ③ 설정을 바꾸면?

컨피그맵을 바꿔도 돌던 파드는 그대로인 것을 보고, 새로 띄워 반영합니다.

```powershell
# configmap.yaml 의 APP_GREETING 값을 아무거나로 바꾸고
kubectl apply -f labs/04-config/configmap.yaml
kubectl exec curl -- curl -s http://demo/          # 아직 그대로
kubectl rollout restart deployment demo
kubectl rollout status deployment demo
kubectl exec curl -- curl -s http://demo/          # 이번엔 바뀜
```

나와야 할 화면 (예시)

```text
configmap/demo-config configured
greeting : hello from configmap           ← 아직 그대로
deployment "demo" successfully rolled out      ← restart · status 뒤
greeting : hi from user1                  ← 바뀜
```

**확인** — restart 전엔 그대로, 뒤엔 바뀝니다. SPRING_PROFILES_ACTIVE 를 real 로 바꿔 같은 순서로 하면 stage 도 바뀝니다.

**막히면** — rollout status 가 끝나도 앱이 뜨기까지 10초쯤 걸립니다. exit code 7 만 나오면 10초 뒤 다시 (3회차 Readiness Probe).

### ④ 시크릿은 숨겨지지 않는다 (먼저 끝났으면)

시크릿으로 넣은 값이 파드 안에서 어떻게 보이는지 봅니다.

```powershell
kubectl exec deploy/demo -- env
kubectl port-forward svc/demo 18080:80     # 브라우저로 http://localhost:18080/actuator/env
```

나와야 할 화면 (예시)

```text
PS> kubectl exec deploy/demo -- env
...
APP_GREETING=hi from user1
APP_PASSWORD=s3cr3t-pass                 ← 평문 그대로
SPRING_PROFILES_ACTIVE=dev
...
```

**확인** — APP_PASSWORD 가 평문으로 보입니다. 시크릿은 값을 숨기는 게 아니라 권한을 나누는 장치입니다.

**막히면** — 브라우저가 빈 화면 · 연결 끊김이면 방금 restart 한 앱이 아직 뜨는 중입니다. 10초 뒤 port-forward 부터 다시.

### ⑤ 고장 3 — 컨테이너가 안 만들어진다

고장 난 디플로이먼트를 띄우고, 원인을 찾아 고칩니다.

```powershell
kubectl apply -f labs/04-config/broken-missing-config.yaml
kubectl get pods
kubectl describe pod -l app=demo-missing          # 실마리는 맨 아래에
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
NAME                           READY   STATUS                       RESTARTS   AGE
curl                           1/1     Running                      0          40m
demo-7c9f6b8d4-r2x5k           1/1     Running                      0          5m
...
demo-missing-6b9d7c5f8-q4w2z   0/1     CreateContainerConfigError   0          12s
```

**확인** — broken-missing-config.yaml 을 고쳐 다시 apply → demo-missing 파드가 Running 이면 성공.

**막히면** — 고장 4 를 안 하면 여기서 정리:  kubectl delete -f labs/04-config/broken-missing-config.yaml.

### ⑤ 고장 4 — 떴다가 계속 죽는다 (먼저 끝났으면)

계속 다시 시작하는 파드의 원인을 로그에서 찾아 고칩니다.

```powershell
kubectl apply -f labs/04-config/broken-crash.yaml
kubectl get pods -w                                  # RESTARTS 가 오르면 Ctrl+C
kubectl logs deploy/demo-crash --previous            # 이번 실마리는 로그
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -w
demo-crash-5f7b9d6c4-x8m2p   0/1   Error              1 (3s ago)    9s
demo-crash-5f7b9d6c4-x8m2p   0/1   CrashLoopBackOff   1 (2s ago)    11s
demo-crash-5f7b9d6c4-x8m2p   0/1   Error              2 (15s ago)   24s
demo-crash-5f7b9d6c4-x8m2p   0/1   CrashLoopBackOff   2 (1s ago)    25s
```

**확인** — 고쳐서 apply → kubectl rollout restart deployment demo-crash → Running 이면 성공.

**막히면** — 값은 "8080" 처럼 따옴표째 · 끝나면 고장 3 · 4 파일을 둘 다 delete -f 로 정리.

정리 (고장 4 를 안 했으면 앞 파일만):

```powershell
kubectl delete -f labs/04-config/broken-missing-config.yaml -f labs/04-config/broken-crash.yaml
```

---

## 실습 5. 종합 수리 — `labs/05-fix-it`

누군가 배포한 앱 `shop` 이 동작하지 않습니다. **두 군데**가 망가져 있습니다. 힌트는 없습니다.

> `curl` 파드가 없으면 먼저: `kubectl apply -f solutions/03-service`

**시작**

```powershell
kubectl apply -f labs/05-fix-it/shop.yaml
```

**목표** — 이 명령이 `greeting : welcome to shop` 이 들어 있는 화면을 돌려주면 성공입니다.

```powershell
kubectl exec curl -- curl -sS -m 3 http://shop/
```

**진단 순서** — 오늘 쓴 순서 그대로입니다. 하나를 고치면 다음 것이 드러납니다.

1. `kubectl get` — 상태 보기 (`STATUS`, `READY`, `RESTARTS`)
2. `kubectl describe` — 맨 아래 `Events`
3. `kubectl logs` — 앱이 남긴 말 (죽었다면 `--previous`)
4. `kubectl exec` — 안에 들어가서 직접 확인

서비스가 의심되면 `kubectl get endpointslices -l kubernetes.io/service-name=shop` 과 `kubectl describe svc shop`. 고친 것은 `shop.yaml` 에 반영하고 다시 apply 합니다.

**정리**

```powershell
kubectl delete -f labs/05-fix-it/shop.yaml
```

---

## 뒤처졌을 때

TODO 를 채운 완성본이 `solutions/` 에 있습니다. 앞 실습의 완성본을 apply 하고 다음 실습으로 넘어갑니다.

```powershell
# 실습 3 부터 시작할 때
kubectl apply -f solutions/02-deployment

# 실습 4 · 5 부터 시작할 때
kubectl apply -f solutions/02-deployment -f solutions/03-service
```

## 고장 났을 때 보는 순서

`get` → `describe` 의 맨 아래 Events → `logs` (`--previous`) → `exec`

| 상태 | 뜻 | 먼저 볼 곳 |
|---|---|---|
| `Pending` | 아직 노드에 배정이 안 됨 | describe 의 Events |
| `ErrImagePull` / `ImagePullBackOff` | 노드가 이미지를 못 받음 | describe 의 Events (이미지 이름 · 태그) |
| `CreateContainerConfigError` | 컨테이너에 넣을 설정이 없음 | describe 의 Events (컨피그맵 · 시크릿 이름 · 키) |
| `CrashLoopBackOff` | 앱이 떴다가 계속 죽음 | `logs --previous` |
| `Running` 인데 접속 안 됨 | 서비스 연결 문제 | 엔드포인트 슬라이스, 셀렉터, targetPort |
| `get pods` 에 파드가 아예 없음 | 내 칸의 쿼터(파드 15개)가 참 | `kubectl describe rs` 의 Events (exceeded quota) |

## 끝나고

디플로이먼트 `demo` · 서비스 `demo` · 파드 `curl` 은 2회차에도 이어 씁니다. 그대로 둡니다. `broken` 이 붙은 파일과 `shop` 은 지워도 됩니다. 접속 파일(`<아이디>.kubeconfig`)은 남에게 보내지 않습니다.
