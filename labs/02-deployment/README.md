# 실습 2. 디플로이먼트

"파드 3개" 를 선언해 두면, 쿠버네티스가 그 수를 계속 맞춰 주는 것을 봅니다. 명령은 모두 **저장소 맨 위 폴더(`~/k8s-study`)**에서 칩니다.

> 실습 1 의 `demo-broken` 이 아직 보이면 `kubectl delete pod demo-broken` 으로 지웁니다. 이름 뒤에 임의 문자가 없는 파드는 주인(레플리카셋)이 없어서 지우면 그대로 사라집니다.
> 터미널 탭을 새로 열면 `cd ~/k8s-study` 부터 칩니다. 접속 설정은 실습 0 에서 한 번 해 두었으니 그대로입니다(Windows 에서 새 탭이 안 되면 Windows Terminal 을 닫았다 다시 엽니다).

## 1. TODO 채우고 띄우기

`labs/02-deployment/deployment.yaml` 의 `# TODO` 한 줄을 채우세요.

```powershell
kubectl apply -f labs/02-deployment/deployment.yaml
kubectl get deploy,rs,pods
```

TODO 를 안 채우고 apply 하면 ``… `selector` does not match template `labels` `` 에러가 납니다. 디플로이먼트는 **이름이 아니라 레이블로** 자기 파드를 찾기 때문입니다.

잘 되면 디플로이먼트 1개 → 레플리카셋 1개 → 파드 3개가 보입니다. 파드 이름이 `레플리카셋 이름 + 임의 문자` 인 것도 봐 두세요.

## 2. 지워도 다시 생긴다

터미널 탭 하나에서 지켜보기를 켜 둡니다:

```powershell
kubectl get pods -w
```

다른 탭에서 파드 하나를 골라 지웁니다:

```powershell
kubectl delete pod <파드 이름>
```

지운 파드가 `Terminating` 이 되는 동시에 새 파드가 생깁니다. "3개여야 하는데 2개" 를 본 컨트롤러가 하나를 더 만든 것입니다. 우리가 한 일은 지운 것뿐입니다.

## 3. 레이블과 셀렉터

```powershell
kubectl get pods --show-labels
kubectl get pods -l app=demo
```

파드 하나의 레이블을 바꿔 봅니다:

```powershell
kubectl label pod <파드 이름> app=other --overwrite
kubectl get pods --show-labels
```

파드가 4개가 됐습니다. 레이블이 바뀐 파드는 셀렉터에서 빠져서 주인 없는 파드가 되고, 레플리카셋은 `app=demo` 파드를 3개로 맞추려고 하나를 새로 만들었습니다.

```powershell
kubectl delete pod -l app=other
```

## 4. scale 과 파일

```powershell
kubectl scale deployment demo --replicas=5
kubectl get pods
kubectl apply -f labs/02-deployment/deployment.yaml
kubectl get pods
```

다시 3개로 돌아갑니다. 명령으로 바꾼 값은 파일을 다시 apply 하면 파일 값으로 덮입니다. **파일을 고치고 apply 하는 것이 정석**입니다. 파일이 기준이어야 누가 언제 무엇을 바꿨는지 남습니다.

## 5. 없는 버전으로 바꾸면?

```powershell
kubectl apply -f labs/02-deployment/deployment-badtag.yaml
kubectl get pods
kubectl rollout status deployment demo
```

`rollout status` 는 끝나지 않으니 잠시 보고 `Ctrl+C`.

새 파드 하나는 실습 1 에서 본 그 상태인데, **기존 파드 3개는 그대로 Running** 입니다. 잘못된 배포를 해도 서비스가 안 끊긴 겁니다. 왜 이렇게 되는지는 2회차(Rolling Update)에서 다룹니다.

되돌리기:

```powershell
kubectl apply -f labs/02-deployment/deployment.yaml
kubectl get pods
```

지금까지 무슨 일이 있었는지는 디플로이먼트의 Events 에 시간순으로 남습니다:

```powershell
kubectl describe deployment demo
```

맨 아래에 `Scaled up replica set … from 0 to 3` 처럼 개수가 바뀐 줄이 나옵니다. 없는 버전으로 바꿨을 때 새 레플리카셋이 `0 to 1`, 되돌렸을 때 `1 to 0` 이 된 것까지 보입니다. 4번(scale)을 했다면 `3 to 5`, `5 to 3` 도 있습니다.

## 정리

디플로이먼트 `demo` 는 실습 3 에서 그대로 씁니다. **지우지 마세요.**
