# 실습 5. 종합 수리

누군가 배포한 앱 `shop` 이 동작하지 않습니다. **두 군데**가 망가져 있습니다. 힌트는 없습니다.

> `curl` 파드가 없으면 먼저: `kubectl apply -f solutions/03-service`

## 시작

```powershell
kubectl apply -f labs/05-fix-it/shop.yaml
```

## 목표

```powershell
kubectl exec curl -- curl -sS -m 3 http://shop/
```

이 명령이 `greeting : welcome to shop` 이 들어 있는 화면을 돌려주면 성공입니다.

## 진단 순서

오늘 쓴 순서 그대로입니다.

1. `kubectl get` — 상태 보기 (`STATUS`, `READY`, `RESTARTS`)
2. `kubectl describe` — 맨 아래 `Events`
3. `kubectl logs` — 앱이 남긴 말 (죽었다면 `--previous`)
4. `kubectl exec` — 안에 들어가서 직접 확인

서비스가 의심되면 `kubectl get endpointslices -l kubernetes.io/service-name=shop` 과 `kubectl describe svc shop`.

고친 것은 `shop.yaml` 에 반영하고 다시 apply 하세요.

## 정리

```powershell
kubectl delete -f labs/05-fix-it/shop.yaml
```
