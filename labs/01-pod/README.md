# 실습 1. 파드

파드 하나를 띄우고 안팎을 들여다본 다음, 일부러 고장 내 봅니다. 명령은 모두 **저장소 맨 위 폴더(`~/k8s-study`)**에서 칩니다. 이 폴더(`labs/01-pod`) 안으로 들어가서 치면 경로가 맞지 않습니다.

## 1. 띄우기

```powershell
kubectl apply -f labs/01-pod/pod.yaml
kubectl get pods -o wide -w
```

`STATUS` 가 `ContainerCreating` 에서 `Running` 이 되면 `Ctrl+C`. `IP` 와 `NODE` 열도 봐 두세요.

## 2. 누가 띄웠나 — Events

```powershell
kubectl describe pod demo-pod
```

맨 아래 `Events` 를 위에서부터 읽습니다. `From` 열을 보면

- `default-scheduler` 가 노드를 고르고 (`Scheduled`)
- 그 노드의 `kubelet` 이 이미지를 받고 (`Pulling` → `Pulled`) 컨테이너를 만들어 (`Created`) 시작합니다 (`Started`)

방금 본 아키텍처 그림의 컴포넌트들이 실제로 일한 흔적입니다.

## 3. 로그와 접속

```powershell
kubectl logs demo-pod
kubectl port-forward pod/demo-pod 18080:8080
```

port-forward 는 켜 둔 채로, **새 터미널 탭**에서:

```powershell
curl.exe -s http://localhost:18080/     # Windows
curl -s http://localhost:18080/         # Mac
```

브라우저로 <http://localhost:18080/> 을 열어도 됩니다. 다 봤으면 port-forward 탭에서 `Ctrl+C`.

> 내 PC 의 8080 은 다른 앱이 쓰고 있을 수 있어서 18080 을 씁니다.
> Windows 는 `curl.exe`, Mac 은 `curl` 입니다. `curl.exe` 는 Windows 10 부터 기본으로 들어 있는 프로그램이고, PowerShell 의 `curl` 은 다른 명령(Invoke-WebRequest)의 별칭입니다.

## 4. 안으로 들어가기

```powershell
kubectl exec -it demo-pod -- sh
```

안에서:

```sh
hostname
env | grep APP
exit
```

`hostname` 이 파드 이름과 같고, `APP_` 로 시작하는 환경변수는 아직 하나도 없습니다. (실습 4 에서 생깁니다)

## 5. 내가 쓴 것 vs 쿠버네티스가 붙인 것

```powershell
kubectl get pod demo-pod -o yaml
```

- `spec` — 내가 쓴 내용에 기본값이 채워진 것. **원하는 상태**
- `status` — 쿠버네티스가 관찰한 지금 모습. **실제 상태**

이 둘이 따로 있고, 쿠버네티스는 실제 상태를 원하는 상태로 계속 맞춥니다. 오늘 전체의 핵심입니다.

필드 뜻이 궁금할 때는 책 대신 이걸 칩니다:

```powershell
kubectl explain pod.spec.containers.ports
```

## 6. 지우면?

```powershell
kubectl delete pod demo-pod
kubectl get pods
```

다시 생기지 않습니다. 파드 혼자서는 다시 살아나지 않습니다. → 실습 2

## 7. 고장 1 — 뜨지 않는 파드

```powershell
kubectl apply -f labs/01-pod/pod-broken.yaml
kubectl get pods -w
```

`STATUS` 를 보고 `Ctrl+C`. 이제 원인을 찾아보세요. 실마리는 `kubectl describe pod demo-broken` 의 맨 아래에 있습니다.

원인을 찾았으면 `pod-broken.yaml` 을 고치고 다시 `kubectl apply -f labs/01-pod/pod-broken.yaml` → `Running` 이 되면 성공.

> 이미지를 받는 건 내 PC 가 아니라 **노드의 kubelet** 입니다.

## 정리

```powershell
kubectl delete -f labs/01-pod/pod-broken.yaml
```
