# 실습 가이드 — 쿠버네티스 스터디 2회차

1회차에 만든 내 칸 · 내 앱(demo)에 이어서, 밖에서 들어오는 입구를 만들고(인그레스), 파드가 죽어도 남는 디스크를 붙이고(볼륨 · PVC), 바쁘면 저절로 늘게 하고(HPA), 끊지 않고 새 버전으로 바꿉니다(롤링 업데이트). 실습 번호는 1회차에 이어서 6번부터입니다. 실습마다 끝에 일부러 망가뜨린 파일이 하나씩 있습니다. 원인을 찾아 고쳐 보세요.

| 실습 | 폴더 | 하는 일 |
|---|---|---|
| 0. 준비 · 이어 쓰기 | — | 새 압축 풀기, 1회차 끝 모습으로 맞추기 |
| 6. 인그레스 | `labs/06-ingress` | 내 주소로 밖에서 열기, 이름으로 나누기, 열면 안 되는 길 닫기, 고장 5 |
| 7. 볼륨 · PV · PVC | `labs/07-volume` | 사라지는 폴더 · 남는 디스크, EBS 볼륨 보기, 설정을 파일로, 고장 6 |
| 8. 리소스 · HPA | `labs/08-hpa` | 적어 둔 값과 쓰는 값, 2 코어 예약, 고장 7, 부하에 따라 늘어나는 파드 |
| 9. 롤링 업데이트 | `labs/09-rollout` | 1.0 → 2.0 을 지켜보며 바꾸기, 되돌리기, 고장 8 |
| 10. 종합 수리 | `labs/10-fix-it` | 밖에서 열리지 않는 shop 고치기 (힌트 없음) |

## 실습 0. 준비 · 이어 쓰기

받는 것은 1회차와 같습니다. **2회차 `k8s-study.zip`** 을 1:1 메시지로 받고(1회차 압축에 2회차 실습 파일과 내 주소가 더해진 것 · 접속 파일은 같음), 이 실습 가이드를 받습니다. 1회차에 오지 않았어도 아래 명령 그대로 하면 됩니다.

`k8s-study.zip` 은 **다운로드 폴더**에 저장합니다. 명령은 누구나 같습니다. 이름이나 아이디를 바꿔 칠 곳은 없습니다.

### ① 새 압축 풀기

내 PC 에 맞는 칸 하나만 합니다. 1회차에 했어도 첫 줄부터 그대로 다시 칩니다. 첫 줄이 1회차 폴더를 덮어씁니다(1회차 때 고친 YAML 도 원래대로 돌아갑니다 — 오늘은 쓰지 않습니다).

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
unzip -oq ~/Downloads/k8s-study.zip -d ~                          # -o 는 1회차 폴더를 묻지 않고 덮어쓰기
echo 'export KUBECONFIG=~/k8s-study/kubeconfig' >> ~/.zshrc; export KUBECONFIG=~/k8s-study/kubeconfig
brew install kubectl                                              # 이미 있으면 그대로
kubectl get pods
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods
curl                    1/1     Running   0          6d     ← 1회차에서 남긴 것 (처음이면 No resources found)
```

**확인** — 파드 목록이나 No resources found 가 나오면 됩니다. 1회차에 했어도 첫 줄부터 그대로 쳐도 됩니다.

**막히면** — Windows: 탐색기로 풀지 않고 첫 줄 명령으로 풉니다 · 새 탭에서 kubectl 을 못 찾으면 Windows Terminal 을 닫았다 다시 엽니다. Mac: 첫 줄 끝에는 # 설명을 붙이지 않습니다 · unzip 이 파일을 못 찾으면 다운로드 폴더의 파일 이름을 봅니다.

실습 명령은 전부 **이 폴더(`k8s-study`) 맨 위**에서 칩니다. `labs` 안으로 더 들어가지 않습니다. 새 창 · 새 탭을 열면 `cd ~/k8s-study` 부터 칩니다.

### ② 1회차 끝 모습으로 맞추기

1회차 실습 4 · 5 에서 남은 것을 지우고, demo · 서비스 · curl 파드만 있는 모습으로 맞춥니다.

```powershell
cd ~/k8s-study                                                # 오늘 명령도 전부 이 폴더에서
kubectl delete -f labs/04-config -f labs/05-fix-it --ignore-not-found
kubectl apply -f solutions/02-deployment -f solutions/03-service
kubectl get deploy,svc,pods
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy,svc,pods
NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/demo   3/3     3            3           40s
service/demo           ClusterIP   10.100.183.126   <none>   80/TCP   6d
pod/curl                    1/1     Running   0          6d
pod/demo-765c687f8d-9tclb   1/1     Running   0          40s       (demo 파드 셋)
```

**확인** — deployment demo 3/3 · service demo · pod curl 이 보이면 됩니다. 오늘 실습은 이 위에 쌓습니다.

**막히면** — delete 에 not found 가 나와도 괜찮습니다 · 폴더가 다르면 'path does not exist' — cd ~/k8s-study 부터.

- 터미널은 Windows 는 **Windows Terminal + PowerShell**, Mac 은 기본 **터미널**입니다. 새 탭은 Windows `Ctrl+Shift+T`, Mac `Cmd+T`. 새 탭은 `cd ~/k8s-study` 부터 칩니다.
- YAML 의 `# TODO` 줄은 VS Code 같은 편집기로 고칩니다(메모장은 들여쓰기가 깨지기 쉽습니다).
- 내 PC 에서 curl 을 칠 때 Windows 는 **`curl.exe`**, Mac 은 `curl` 입니다. 명령 칸에 `# Windows · Mac 은 curl` 이라고 적힌 줄은 Mac 에서 `curl.exe` 를 `curl` 로 바꿔 칩니다.
- `<HOSTS 값>` 은 실습 6 ① 에서 본 내 주소입니다. 브라우저 주소창에도 그대로 씁니다.
- 명령 칸은 **한 줄씩** 칩니다. 작은따옴표(`'`)는 그대로 둡니다.

**읽는 법** — 단계마다 **할 일 → 명령 → 나와야 할 화면 → 확인** 순서입니다. 나와야 할 화면은 예시라서 파드 이름 · IP · 주소 · 숫자 · 시각은 사람마다 다릅니다. **(먼저 끝났으면)** 이 붙은 단계는 건너뛰어도 다음 단계에 지장이 없습니다. 자주 쓰는 명령은 [치트시트](docs/치트시트.md) 에 있습니다.

---

## 실습 6. 인그레스 — `labs/06-ingress`

1회차에는 클러스터 안의 curl 파드에서만 닿던 앱을, 내 PC 의 브라우저와 터미널에서 바로 엽니다. 압축에는 내 주소(host)가 이미 채워져 있습니다. ① 의 `kubectl get ingress` 화면에서 HOSTS 값을 보고, 그다음부터 명령의 `<HOSTS 값>` 자리에 그 값을 붙여 넣습니다(꺾쇠까지 바꿉니다).

> demo · 서비스 · curl 파드가 없으면 먼저: `kubectl apply -f solutions/02-deployment -f solutions/03-service`

### ① TODO 채우고 입구 만들기

ingress.yaml 의 TODO 에 1회차 서비스 이름을 채우고, 입구 규칙(인그레스)을 만듭니다.

```powershell
kubectl get ingressclass                          # 우리 클러스터의 입구 — traefik 하나
kubectl apply -f labs/06-ingress/ingress.yaml
kubectl get ingress demo                          # ADDRESS 가 비어 있으면 10초 뒤 다시
```

나와야 할 화면 (예시)

```text
PS> kubectl get ingressclass
NAME      CONTROLLER                      PARAMETERS   AGE
traefik   traefik.io/ingress-controller   <none>       6d
PS> kubectl apply -f labs/06-ingress/ingress.yaml
ingress.networking.k8s.io/demo created
PS> kubectl get ingress demo
NAME   CLASS     HOSTS                        ADDRESS                                     PORTS   AGE
demo   traefik   user1.3-38-247-68.sslip.io   a1b2c3….elb.ap-northeast-2.amazonaws.com    80      9s
```

**확인** — HOSTS 가 내 주소, ADDRESS 가 입구(NLB)입니다. HOSTS 값은 사람마다 다르고, 다음 단계부터 이 값을 복사해 씁니다.

**막히면** — HOSTS 가 __HOST__ 면 2회차 압축이 아닙니다 · TODO 그대로면 apply 가 Invalid value: "TODO" 로 거절.

### ② 밖에서 열기

브라우저 주소창에 http://`<HOSTS 값>`/ 을 열고, 터미널에서도 /pod 를 여러 번 부릅니다.

```powershell
curl.exe -s http://<HOSTS 값>/                                # Windows · <HOSTS 값> 은 ① 화면에서 복사
curl -s http://<HOSTS 값>/                                    # Mac
1..4 | ForEach-Object { curl.exe -s http://<HOSTS 값>/pod }     # Windows · 네 번
for i in {1..4}; do curl -s http://<HOSTS 값>/pod; done         # Mac
```

나와야 할 화면 (예시)

```text
PS> curl.exe -s http://user1.3-38-247-68.sslip.io/
pod      : demo-765c687f8d-9tclb        version  : 1.0   …  (화면에서는 일곱 줄)
PS> 1..4 | ForEach-Object { curl.exe -s http://user1.3-38-247-68.sslip.io/pod }
demo-765c687f8d-9tclb
demo-765c687f8d-gxf5v
demo-765c687f8d-j92cp
demo-765c687f8d-gxf5v
```

**확인** — 내 PC 에서 바로 1회차 그 화면이 뜨면 됩니다(kubectl exec 없이). /pod 는 파드 셋이 돌아가며 답합니다.

**막히면** — 시간 초과면 회사 망이 80 을 막는 것일 수 있습니다 — 휴대폰(데이터)으로 같은 주소를 열어 봅니다.

### ③ 입구 하나, 이름 둘

2.0 앱을 하나 더 띄우고, 주소 앞에 v2. 를 붙이면 그쪽으로 가는 규칙을 더합니다.

```powershell
kubectl apply -f labs/06-ingress/demo-v2.yaml -f labs/06-ingress/ingress-two.yaml
kubectl get ingress demo                          # HOSTS 가 두 개 · ADDRESS 는 그대로 하나
curl.exe -s http://v2.<HOSTS 값>/                  # Windows · Mac 은 curl · 2.0 이 뜨는 데 20초쯤
curl.exe -s http://<HOSTS 값>/                     # 원래 주소는 그대로 1.0
```

나와야 할 화면 (예시)

```text
PS> kubectl get ingress demo
NAME   CLASS     HOSTS                                                   ADDRESS     PORTS   AGE
demo   traefik   user1.3-38-247-68.sslip.io,v2.user1.3-38-247-68.sslip.io   a1b2c3…     80      3m
PS> curl.exe -s http://v2.user1.3-38-247-68.sslip.io/
pod      : demo-v2-6c9f7d8b4-k2m4p        version  : 2.0  (화면에서는 두 줄)
PS> curl.exe -s http://user1.3-38-247-68.sslip.io/
pod      : demo-765c687f8d-j92cp          version  : 1.0
```

**확인** — v2. 주소는 version 2.0, 원래 주소는 1.0 이면 됩니다. 들어오는 입구(ADDRESS)는 하나인데 이름으로 나뉩니다.

**막히면** — v2. 주소가 503 · 502 면 2.0 파드가 아직 뜨는 중입니다 — 20초 뒤 다시.

### ④ 밖에서 보이면 안 되는 길

브라우저로 http://`<HOSTS 값>`/actuator/env 를 열어 보고, 밖으로 열 길만 남긴 규칙으로 바꿉니다.

```powershell
curl.exe -sI http://<HOSTS 값>/actuator/env       # Windows · Mac 은 curl · -I 는 응답 머리만
kubectl apply -f labs/06-ingress/ingress-safe.yaml  # 열 길만: / 와 /pod
curl.exe -sI http://<HOSTS 값>/actuator/env
curl.exe -s http://<HOSTS 값>/pod                  # 열어 둔 길은 그대로
```

나와야 할 화면 (예시)

```text
PS> curl.exe -sI http://user1.3-38-247-68.sslip.io/actuator/env
HTTP/1.1 200 OK
PS> kubectl apply -f labs/06-ingress/ingress-safe.yaml
PS> curl.exe -sI http://user1.3-38-247-68.sslip.io/actuator/env
HTTP/1.1 404 Not Found
PS> curl.exe -s http://user1.3-38-247-68.sslip.io/pod
demo-765c687f8d-gxf5v
```

**확인** — 닫기 전 200, 닫은 뒤 404 면 됩니다. 브라우저의 env 화면에는 파드의 환경변수 · 내부 주소가 다 보였습니다.

**막히면** — v2. 주소도 이 파일에는 없어서 같이 닫혔습니다(정상) · 200 이 그대로면 apply 한 파일 이름을 봅니다.

### ⑤ 고장 5 — 열리지 않는 주소

고장 난 입구 규칙을 만들고, broken. 주소가 안 열리는 원인을 찾아 고칩니다.

```powershell
kubectl apply -f labs/06-ingress/ingress-broken.yaml
curl.exe -sI http://broken.<HOSTS 값>/            # Windows · Mac 은 curl
kubectl get ingress                               # 두 줄을 칸마다 비교합니다
```

나와야 할 화면 (예시)

```text
PS> curl.exe -sI http://broken.user1.3-38-247-68.sslip.io/
HTTP/1.1 404 Not Found
PS> kubectl get ingress
NAME          CLASS     HOSTS                               ADDRESS     PORTS   AGE
demo          traefik   user1.3-38-247-68.sslip.io          a1b2c3…     80      20m
demo-broken   <none>    broken.user1.3-38-247-68.sslip.io               80      9s
```

**확인** — 원인을 찾아 ingress-broken.yaml 을 고치고 다시 apply 합니다. 같은 curl 이 200 OK 면 성공.

**막히면** — ADDRESS 가 비어 있다 = 이 규칙을 맡은 입구가 없다 · 두 파일을 나란히 열어 비교해 봅니다.

### 정리 — 실습 7 로 넘어가기 전에

2.0 앱과 고장 규칙을 지우고, 입구 규칙 demo 와 디플로이먼트 demo 만 남았는지 봅니다.

```powershell
kubectl delete -f labs/06-ingress/demo-v2.yaml -f labs/06-ingress/ingress-broken.yaml
kubectl get ingress,deploy
```

나와야 할 화면 (예시)

```text
PS> kubectl get ingress,deploy
NAME                             CLASS     HOSTS                        ADDRESS     PORTS   AGE
ingress.networking.k8s.io/demo   traefik   user1.3-38-247-68.sslip.io   a1b2c3…     80      12m
NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/demo   3/3     3            3           20m
```

**확인** — ingress demo 와 deployment demo 만 남으면 됩니다. 입구 규칙 demo 는 남겨 둡니다(3회차에도 이어 씀).

---

## 실습 7. 볼륨 · PV · PVC — `labs/07-volume`

메모를 남기는 작은 파드 `note` 로, 같은 파일이 폴더 종류에 따라 사라지기도 하고 남기도 하는 것을 봅니다. ② 부터는 AWS 에 실제 디스크(EBS)가 생깁니다. 끝의 정리 단계에서 꼭 지웁니다.

### ① 임시 폴더 — emptyDir

메모하는 파드 note 를 띄우고 /data 에 쓴 뒤, 파드를 지우면 메모가 어떻게 되는지 봅니다.

```powershell
kubectl apply -f labs/07-volume/note-emptydir.yaml
kubectl rollout status deploy/note
kubectl exec deploy/note -- sh -c 'date >> /data/note.txt; cat /data/note.txt'
kubectl delete pod -l app=note                    # 디플로이먼트가 새 파드를 띄웁니다
kubectl rollout status deploy/note
kubectl exec deploy/note -- cat /data/note.txt
```

나와야 할 화면 (예시)

```text
PS> kubectl exec deploy/note -- sh -c 'date >> /data/note.txt; cat /data/note.txt'
Sun Nov  8 01:23:45 UTC 2026
PS> kubectl exec deploy/note -- cat /data/note.txt          ← 파드를 지운 뒤
cat: can't open '/data/note.txt': No such file or directory
```

**확인** — 새 파드에는 note.txt 가 없으면 됩니다. emptyDir 는 파드가 지워질 때 같이 지워집니다.

**막히면** — exec 가 'container not found' 면 파드가 아직 뜨는 중 — rollout status 부터 다시.

### ② 디스크 청구서 — PVC

디스크 청구서(PVC)를 만들고, note-pvc.yaml 의 TODO 에 그 이름을 채워 note 가 디스크에 쓰게 합니다.

```powershell
kubectl apply -f labs/07-volume/pvc.yaml
kubectl get pvc                                   # Pending — 쓸 파드가 생길 때까지 기다리는 중
kubectl apply -f labs/07-volume/note-pvc.yaml
kubectl get pvc -w                                # Bound 가 되면 Ctrl+C (디스크를 만드는 데 10~30초)
kubectl rollout status deploy/note
kubectl exec deploy/note -- sh -c 'date >> /data/note.txt; cat /data/note.txt'
```

나와야 할 화면 (예시)

```text
PS> kubectl get pvc
NAME        STATUS    VOLUME           CAPACITY   ACCESS MODES   STORAGECLASS   …   AGE
note-data   Pending                                               gp3            …   5s
PS> kubectl get pvc -w
note-data   Bound     pvc-3f9c1e2a-…   1Gi        RWO            gp3            …   40s
Sun Nov  8 01:31:02 UTC 2026                 ← 마지막 줄 exec 의 출력
```

**확인** — Pending 이 Bound(실제 디스크 PV 와 짝이 됨)로 바뀌고, 메모가 써지면 됩니다.

**막히면** — Permission denied 면 note-pvc.yaml 의 fsGroup 줄이 빠진 것 · claimName 오타면 파드가 Pending.

### ③ 지워도 남는다 · 디스크는 어디에

파드를 지워도 메모가 남는지 보고, 실제 디스크(PV)가 AWS 의 어느 디스크 · 어느 AZ 인지 봅니다.

```powershell
kubectl delete pod -l app=note
kubectl rollout status deploy/note
kubectl exec deploy/note -- cat /data/note.txt       # 아까 쓴 줄이 그대로
kubectl get pv                                       # 클러스터 전체의 디스크 · CLAIM 에 내 칸
kubectl describe pv                                  # VolumeHandle = EBS 볼륨 ID · Node Affinity = AZ
```

나와야 할 화면 (예시)

```text
Sun Nov  8 01:31:02 UTC 2026                 ← 새 파드에서 cat · 아까 쓴 줄 그대로
PS> kubectl describe pv
Claim:             k8s-study-user1/note-data
Node Affinity:     Required Terms:
    Term 0:        topology.kubernetes.io/zone in [ap-northeast-2a]
Source:            Type: CSI · Driver: ebs.csi.aws.com
    VolumeHandle:  vol-0a1b2c3d4e5f67890
```

**확인** — 새 파드에도 아까 쓴 줄이 있으면 됩니다. 디스크는 vol- 로 시작하는 EBS 볼륨이고, AZ 하나에 묶여 있습니다.

**막히면** — describe pv 가 여러 개면 Claim 줄이 내 칸(k8s-study-…/note-data)인 것만 봅니다.

### ④ 설정을 파일로

컨피그맵을 파일로 넣고, 값을 바꾸면 파드를 그대로 둔 채 파일이 따라 바뀌는지 봅니다.

```powershell
kubectl apply -f labs/07-volume/config-file.yaml
kubectl exec cfgfile -- cat /config/greeting         # 파드가 뜨면 (몇 초 뒤)
kubectl apply -f labs/07-volume/config-v2.yaml
kubectl exec cfgfile -- cat /config/greeting         # 바로는 v1 · 30~90초 뒤 다시 치면 v2
```

나와야 할 화면 (예시)

```text
PS> kubectl exec cfgfile -- cat /config/greeting
hello from file v1
PS> kubectl apply -f labs/07-volume/config-v2.yaml
configmap/note-config configured
PS> kubectl exec cfgfile -- cat /config/greeting
hello from file v1                 ← 바로 치면 아직
PS> kubectl exec cfgfile -- cat /config/greeting
hello from file v2                 ← 1분쯤 뒤
```

**확인** — 파드를 다시 띄우지 않았는데 v2 가 나오면 됩니다. 1회차 실습 4 의 환경변수는 다시 띄워야 바뀌었습니다.

**막히면** — 1분 넘게 v1 이면 조금 더 기다립니다(노드가 주기적으로 확인) · exec 에러면 kubectl get pod cfgfile.

### ⑤ 고장 6 — 디스크를 못 받는 파드

고장 난 PVC · 디플로이먼트를 띄우고, 파드가 뜨지 않는 원인을 찾아 고칩니다.

```powershell
kubectl apply -f labs/07-volume/pvc-broken.yaml
kubectl get pods,pvc                              # 둘 다 STATUS 를 봅니다
kubectl describe pvc broken-data                  # 실마리는 맨 아래 Events
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods,pvc
NAME                              READY   STATUS    RESTARTS   AGE
pod/broken-note-7d9f6c5b8-q2w3e   0/1     Pending   0          15s
NAME                                STATUS    VOLUME   CAPACITY   ACCESS MODES   STORAGECLASS   …
persistentvolumeclaim/broken-data   Pending                                      gp3-fast       …
PS> kubectl describe pvc broken-data
  Warning  ProvisioningFailed  …  storageclass.storage.k8s.io "gp3-fast" not found
```

**확인** — 원인을 고친 뒤 PVC 는 지우고 다시 만들어야 합니다(만든 PVC 는 거의 못 고침). 파드가 Running 이면 성공.

**막히면** — 있는 디스크 종류는 kubectl get storageclass · 다시 만들기: delete -f 후 apply -f.

### 정리 — 실습 8 로 넘어가기 전에

실습 7 의 것을 지웁니다. PVC 를 지우면 AWS 의 디스크(EBS)도 같이 지워집니다.

```powershell
kubectl delete -f labs/07-volume/pvc-broken.yaml -f labs/07-volume/config-file.yaml
kubectl delete -f labs/07-volume/note-pvc.yaml -f labs/07-volume/pvc.yaml
kubectl get pvc,pods
```

나와야 할 화면 (예시)

```text
PS> kubectl get pvc,pods
NAME                        READY   STATUS    RESTARTS   AGE
pod/curl                    1/1     Running   0          6d
pod/demo-765c687f8d-9tclb   1/1     Running   0          45m
…
```

**확인** — PVC 가 없고 curl · demo 파드만 남으면 됩니다.

**막히면** — delete 가 오래 걸리면 디스크를 떼는 중입니다 — 그대로 기다립니다.

---

## 실습 8. 리소스 · HPA — `labs/08-hpa`

파드에 적어 두는 두 값(requests · limits)이 어디에 쓰이는지 보고, 부하를 걸어 파드 수가 저절로 늘어나는 것을 봅니다. 터미널 탭을 두 개 씁니다(① 지켜보기 · ② 명령).

### ① 적어 둔 값과 쓰는 값

demo 에 requests · limits 를 적고, 적은 값(describe)과 실제로 쓰는 값(top)을 나란히 봅니다.

```powershell
kubectl apply -f labs/08-hpa/deployment.yaml      # replicas 를 안 적어서 1개로 줄어듭니다
kubectl rollout status deploy/demo
kubectl describe pod -l app=demo                  # 가운데 Limits · Requests, 아래쪽 QoS Class
kubectl top pod -l app=demo                       # 실제로 쓰는 CPU · 메모리
```

나와야 할 화면 (예시)

```text
PS> kubectl describe pod -l app=demo
    Limits:
      cpu:     1        ← 안 적었는데 칸 기본값
      memory:  384Mi
    Requests:
      cpu:        100m
      memory:     256Mi
QoS Class:                   Burstable
demo-5c8d7f9b6-q8w2e    3m           171Mi       ← kubectl top (CPU · 메모리)
```

**확인** — 적어 둔 값(100m · 256Mi)과 실제 쓰는 값(몇 m · 120~200Mi)이 다릅니다. top 이 안 나오면 30초 뒤.

### ② 2 코어를 예약하면

CPU 2 코어를 예약하는 파드를 띄우고, 노드 3대 중 어디에 들어가는지 봅니다.

```powershell
kubectl apply -f labs/08-hpa/big.yaml
kubectl get pods -l app=big                       # Pending 에서 멈춥니다
kubectl describe pod -l app=big                   # 맨 아래 Events
kubectl get nodes -o custom-columns=NAME:.metadata.name,CPU:.status.allocatable.cpu
kubectl delete -f labs/08-hpa/big.yaml
```

나와야 할 화면 (예시)

```text
PS> kubectl describe pod -l app=big
  Warning  FailedScheduling  5s  default-scheduler  0/3 nodes are available: 3 Insufficient cpu. …
PS> kubectl get nodes -o custom-columns=NAME:.metadata.name,CPU:.status.allocatable.cpu
NAME                                                CPU
ip-192-168-11-197.ap-northeast-2.compute.internal   1930m
ip-192-168-58-30.ap-northeast-2.compute.internal    1930m
ip-192-168-64-145.ap-northeast-2.compute.internal   1930m
```

**확인** — 노드는 2 코어지만 파드에 내줄 수 있는 몫은 1930m 라서, 2 코어 예약은 어느 노드에도 못 들어갑니다.

**막히면** — exceeded quota 가 나오면 칸 쿼터에 막힌 것 — 앞 실습의 파드가 남았는지 kubectl get pods.

### ③ 고장 7 — 계속 다시 시작하는 파드

고장 난 디플로이먼트를 띄우고, 파드가 계속 다시 시작하는 원인을 찾아 고칩니다.

```powershell
kubectl apply -f labs/08-hpa/oom.yaml
kubectl get pods -l app=oom -w                    # RESTARTS 가 오르면 Ctrl+C
kubectl describe pod -l app=oom                   # Last State 의 Reason · Exit Code
```

나와야 할 화면 (예시)

```text
PS> kubectl get pods -l app=oom -w
NAME                  READY   STATUS             RESTARTS      AGE
oom-6f7d9c8b5-p2k4x   0/1     OOMKilled          1 (4s ago)    9s
oom-6f7d9c8b5-p2k4x   0/1     CrashLoopBackOff   1 (10s ago)   15s
PS> kubectl describe pod -l app=oom
    Last State:     Terminated
      Reason:       OOMKilled
      Exit Code:    137
```

**확인** — 원인을 찾아 oom.yaml 을 고치고 apply 합니다. Running 으로 1분 넘게 버티면 성공.

**막히면** — 1회차 고장 4 와 달리 logs 에는 남는 게 거의 없습니다 — describe 의 Last State 를 봅니다.

### ④ HPA 붙이기

hpa.yaml 의 TODO 에 목표 50 을 채우고, demo 에 HPA(Horizontal Pod Autoscaler)를 붙입니다.

```powershell
kubectl apply -f labs/08-hpa/hpa.yaml
kubectl get hpa demo                              # TARGETS 가 <unknown> 이면 30초 뒤 다시
```

나와야 할 화면 (예시)

```text
PS> kubectl apply -f labs/08-hpa/hpa.yaml
horizontalpodautoscaler.autoscaling/demo created
PS> kubectl get hpa demo
NAME   REFERENCE         TARGETS       MINPODS   MAXPODS   REPLICAS   AGE
demo   Deployment/demo   cpu: 2%/50%   1         5         1          40s
```

**확인** — TARGETS 가 cpu: 몇%/50% 로 나오면 됩니다. 지금 사용률은 requests(100m) 대비 % 이고, 한가해서 1개입니다.

**막히면** — apply 가 'TODO' 로 에러면 숫자 50 을 안 채운 것 · `<unknown>` 은 붙인 직후 잠깐은 정상.

### ⑤ 부하를 걸면

탭 ① 에서 HPA 를 지켜보며, 탭 ② 에서 demo 를 쉬지 않고 부르는 부하 파드 하나를 띄웁니다.

```powershell
kubectl get hpa demo -w                           # 탭 ① 에 켜 둡니다
kubectl apply -f labs/08-hpa/load.yaml            # 탭 ②
kubectl get pods -l app=demo                      # 탭 ② · 1~2분 뒤 파드가 늘었나
```

나와야 할 화면 (예시)

```text
PS> kubectl get hpa demo -w
NAME   REFERENCE         TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
demo   Deployment/demo   cpu: 5%/50%     1         5         1          10m
demo   Deployment/demo   cpu: 407%/50%   1         5         1          11m
demo   Deployment/demo   cpu: 587%/50%   1         5         5          11m
demo   Deployment/demo   cpu: 237%/50%   1         5         5          11m
demo   Deployment/demo   cpu: 187%/50%   1         5         5          12m
```

**확인** — TARGETS 가 50% 를 넘고 1분 안팎에 REPLICAS 가 늘면 됩니다. 늘 수 = 올림(지금 수 × 사용률 ÷ 50), 최대 5.

**막히면** — 1분 넘게 그대로면 kubectl get pods -l app=load 로 부하 파드가 Running 인지 봅니다.

### ⑥ 부하를 빼면 (먼저 끝났으면)

부하 파드를 지우고, HPA 가 얼마 뒤에 파드를 줄이는지 탭 ① 에서 봅니다.

```powershell
kubectl delete -f labs/08-hpa/load.yaml           # 탭 ② · 탭 ① 은 계속 지켜보기
```

나와야 할 화면 (예시)

```text
PS> kubectl get hpa demo -w
demo   Deployment/demo   cpu: 12%/50%    1         5         5          13m
demo   Deployment/demo   cpu: 5%/50%     1         5         4          13m
demo   Deployment/demo   cpu: 5%/50%     1         5         2          13m
demo   Deployment/demo   cpu: 5%/50%     1         5         1          14m
```

**확인** — 1분 남짓 지나면 5 → 1 로 몇 번에 나눠 줄어듭니다. 지켜보는 시간을 실습이라 30초로 줄였고, 기본은 5분입니다.

### 정리 — 실습 9 로 넘어가기 전에

탭 ① 은 Ctrl+C 로 끄고, HPA · 부하 · 고장 파드를 지웁니다.

```powershell
kubectl delete -f labs/08-hpa/hpa.yaml -f labs/08-hpa/load.yaml -f labs/08-hpa/oom.yaml --ignore-not-found
kubectl get hpa,deploy
```

나와야 할 화면 (예시)

```text
PS> kubectl get hpa,deploy
NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/demo   1/1     1            1           1h
```

**확인** — HPA 가 없고 디플로이먼트 demo 만 남으면 됩니다(지금은 1개 · 실습 9 에서 3개로 다시 정함).

---

## 실습 9. 롤링 업데이트 — `labs/09-rollout`

탭 ① 에 0.5초마다 demo 를 불러 버전을 찍는 반복을 켜 두고, 탭 ② 에서 버전을 바꿉니다. 배포하는 동안 탭 ① 에 무엇이 찍히는지가 이 실습의 요점입니다. FAIL 이 몇 줄 나와도 놀라지 않습니다.

> demo · 서비스 · curl 파드가 없으면 먼저: `kubectl apply -f solutions/02-deployment -f solutions/03-service`

### ① 지켜보기 켜기

탭 ② 에서 demo 를 1.0 × 3 으로 맞추고, 탭 ① 에서 0.5초마다 버전을 찍는 반복을 켭니다.

```powershell
kubectl apply -f labs/09-rollout/deployment.yaml        # 탭 ② · 1.0 × 3 · 하나씩 바꾸는 규칙
kubectl rollout status deploy/demo                      # 탭 ②
kubectl exec -it curl -- sh -c 'while true; do curl -s -m1 demo/ | grep ver || echo FAIL; sleep 0.5; done'
```

나와야 할 화면 (예시)

```text
PS> kubectl exec -it curl -- sh -c '…'          ← 탭 ①
version  : 1.0
version  : 1.0
version  : 1.0
```

**확인** — 탭 ① 에 version : 1.0 이 계속 찍히면 됩니다. 탭 ① 은 실습 9 가 끝날 때까지 켜 둡니다.

**막히면** — 마지막 줄은 탭 ① 에서 칩니다 · 따옴표는 작은따옴표(') 그대로 · 끌 때는 Ctrl+C.

### ② 2.0 으로 바꾸기

deployment.yaml 의 1.0 두 군데를 2.0 으로 고쳐 저장하고, 탭 ② 에서 apply 한 뒤 탭 ① 을 봅니다.

```powershell
kubectl apply -f labs/09-rollout/deployment.yaml
kubectl get rs -w                                       # 새 것 0→1→2→3 · 옛 것 3→2→1→0 · Ctrl+C
kubectl rollout status deploy/demo
```

나와야 할 화면 (예시)

```text
탭 ①
version  : 1.0
version  : 1.0
FAIL
FAIL                ← 몇 초 동안 이어짐
FAIL
version  : 2.0
version  : 2.0
```

**확인** — 1.0 → FAIL 이 몇 초 → 2.0 만 남으면 됩니다. 새 앱이 다 뜨기 전에 옛 파드가 모두 내려가서 생긴 FAIL 입니다.

**막히면** — unchanged 면 저장을 안 한 것 · 2.0 이 안 보이면 kubectl get rs 로 새 레플리카셋이 생겼는지.

### ③ 지나간 판 · 되돌리기

배포 기록을 보고, 바로 앞 판(1.0)으로 되돌립니다.

```powershell
kubectl rollout history deploy/demo
kubectl rollout undo deploy/demo                        # 바로 앞 판으로
kubectl rollout status deploy/demo
```

나와야 할 화면 (예시)

```text
PS> kubectl rollout history deploy/demo
deployment.apps/demo
REVISION  CHANGE-CAUSE
3         image 1.0
4         image 2.0
PS> kubectl rollout undo deploy/demo
deployment.apps/demo rolled back
탭 ①
FAIL  …  version  : 1.0     ← 되돌릴 때도 FAIL 이 몇 초
```

**확인** — 탭 ① 이 다시 1.0 이 되면 됩니다. 클러스터는 1.0 인데 파일은 아직 2.0 이라는 것도 봐 둡니다.

**막히면** — REVISION 숫자는 사람마다 다릅니다(1회차부터 바꾼 횟수) · CHANGE-CAUSE 가 `<none>` 인 줄은 1회차 것.

### ④ 고장 8 — 배포는 성공, 접속은 실패

새 판을 배포하고, 배포는 끝났다는데 탭 ① 이 실패하는 것을 봅니다. 먼저 살리고, 원인을 찾습니다.

```powershell
kubectl apply -f labs/09-rollout/deployment-broken.yaml
kubectl rollout status deploy/demo                      # successfully rolled out
kubectl get pods -l app=demo                            # 전부 Running · 1/1
```

나와야 할 화면 (예시)

```text
PS> kubectl rollout status deploy/demo
deployment "demo" successfully rolled out
PS> kubectl get pods -l app=demo
NAME                    READY   STATUS    RESTARTS   AGE
demo-7b6c5d9f8-a1b2c    1/1     Running   0          40s
탭 ①
FAIL
FAIL
```

**확인** — 먼저 rollout undo 로 살리고(탭 ① 이 다시 version), 그다음 원인을 찾습니다. 원인을 말할 수 있으면 성공.

**막히면** — 원인은 새 판 파드의 kubectl logs 에서 'Tomcat started on port' 줄 · 되돌린 뒤엔 rollout history.

### 정리 — 실습 10 으로 넘어가기 전에

탭 ① 의 반복은 Ctrl+C 로 끄고, demo 가 1.0 × 3 으로 돌아왔는지 봅니다.

```powershell
kubectl get deploy,pods
```

나와야 할 화면 (예시)

```text
PS> kubectl get deploy,pods
NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/demo   3/3     3            3           1h
NAME                        READY   STATUS    RESTARTS   AGE
pod/curl                    1/1     Running   0          6d
pod/demo-765c687f8d-9tclb   1/1     Running   0          2m
```

**확인** — demo 3/3 과 curl 이 Running 이면 됩니다. 실습 10 은 이와 따로 shop 을 띄웁니다.

**막히면** — -it 없이 반복을 켰었다면 curl 파드 안에 남아 계속 돕니다 — kubectl delete pod curl 후 solutions/03-service 를 다시 apply.

---

## 실습 10. 종합 수리 — `labs/10-fix-it`

누군가 배포한 앱 `shop` 이 밖에서 열리지 않습니다. **두 군데**가 망가져 있습니다. 힌트는 없습니다.

**시작**

```powershell
kubectl apply -f labs/10-fix-it/shop.yaml
```

`shop.yaml` 한 파일에 컨피그맵 `shop-config` · PVC `shop-data` · 디플로이먼트 `shop` · 서비스 `shop` · 인그레스 `shop` 다섯 개가 `---` 로 나뉘어 들어 있습니다. apply 한 번에 다 만들어집니다.

**목표** — 브라우저에서 `http://shop.<HOSTS 값>/` 을 열었을 때 `greeting : welcome to shop` 이 나오면 성공입니다. 주소는 `kubectl get ingress shop` 의 HOSTS 에 있습니다.

```powershell
curl.exe -s http://shop.<HOSTS 값>/       # Windows · Mac 은 curl
```

**진단 순서** — 오늘 쓴 순서 그대로입니다. 하나를 고치면 다음 것이 드러납니다.

1. `kubectl get pods,pvc,ingress` — 상태 보기 (`STATUS`, `READY`, `RESTARTS`)
2. `kubectl describe` — 파드 · PVC 는 맨 아래 `Events`, 다시 시작하는 파드는 `Last State`
3. `kubectl logs` — 앱이 남긴 말 (죽었다면 `--previous`)
4. 인그레스가 의심되면 `kubectl describe ingress shop` 의 Backends

고친 것은 `shop.yaml` 에 반영하고 다시 apply 합니다.

**정리** — PVC 가 들어 있으니 꼭 지웁니다. 지우면 AWS 의 디스크도 같이 지워집니다.

```powershell
kubectl delete -f labs/10-fix-it/shop.yaml
kubectl get pvc
```

---

## 뒤처졌을 때

TODO 를 채운 완성본이 `solutions/` 에 있습니다. 앞 실습을 못 끝냈으면 그 실습의 완성본을 apply 하거나, 1회차 끝 모습부터 다시 맞추고 넘어갑니다.

```powershell
# 어느 실습이든 시작 전에 demo · 서비스 · curl 파드가 없을 때
kubectl apply -f solutions/02-deployment -f solutions/03-service

# 실습 6 의 입구 규칙 완성본
kubectl apply -f solutions/06-ingress
```

## 고장 났을 때 보는 순서

`get` → `describe` 의 맨 아래 Events(다시 시작하면 Last State) → `logs` (`--previous`) → `exec`

| 상태 | 뜻 | 먼저 볼 곳 |
|---|---|---|
| 밖에서 `404` | 입구는 받았는데 맞는 규칙이 없음 | `kubectl get ingress` 의 CLASS · ADDRESS(비었으면 맡은 입구 없음) → `kubectl describe ingress` 의 Backends · 주소(host) · 경로 |
| 밖에서 `503` (`no available server`) | 규칙은 있는데 뒤에 준비된 파드가 없음 | 파드 상태(Pending · 뜨는 중), 서비스 이름 · 셀렉터, 엔드포인트 슬라이스 |
| 밖에서 `502` / `504` | 파드까지 갔는데 앱이 응답을 못 함 | 앱이 아직 뜨는 중인지, 앱이 듣는 포트(`logs`) |
| 밖에서 시간 초과 | 입구까지 못 감 | 주소 오타, 회사 망 (휴대폰 데이터로 같은 주소) |
| `get ingress` 의 ADDRESS 가 비어 있음 | 이 규칙을 맡는 입구가 없음 | `ingressClassName` |
| PVC `Pending` | 쓸 파드를 기다림(정상) 또는 디스크를 못 만듦 | `kubectl describe pvc` 의 Events (디스크 종류 이름) |
| 파드 `Pending` | 노드 자리 · 디스크가 없음 | `kubectl describe pod` 의 Events (Insufficient · persistentvolumeclaim) |
| `OOMKilled` / Exit Code `137` | 메모리 한도를 넘어 죽음 | `kubectl describe pod` 의 Last State, `resources.limits.memory` |
| `CrashLoopBackOff` | 앱이 떴다가 계속 죽음 | `logs --previous`, 로그가 없으면 Last State |
| HPA 의 TARGETS `<unknown>` | 사용량을 아직 못 셈 | 붙인 직후면 30초 기다림, 계속이면 `resources.requests.cpu` |
| 배포는 성공인데 접속 실패 | 컨테이너는 떠 있는데 앱이 일을 안 함 | 먼저 `kubectl rollout undo`, 그다음 `logs` |

## 끝나고

디플로이먼트 `demo` · 서비스 `demo` · 파드 `curl` · 인그레스 `demo` 는 3회차에도 이어 씁니다. 그대로 둡니다. `shop` 과 실습 7 의 PVC 처럼 디스크를 쓰는 것은 꼭 지웁니다. `kubectl get pvc` 가 `No resources found` 면 됩니다. 접속 파일(`k8s-study/kubeconfig`)은 남에게 보내지 않습니다.

스터디가 모두 끝나면 접속 설정을 지웁니다. 원래 쓰던 쿠버네티스 설정으로 돌아갑니다.

- Windows — `[Environment]::SetEnvironmentVariable("KUBECONFIG", $null, "User")` 를 치고 터미널을 새로 엽니다.
- Mac — `~/.zshrc` 의 `export KUBECONFIG=~/k8s-study/kubeconfig` 줄을 지웁니다.
