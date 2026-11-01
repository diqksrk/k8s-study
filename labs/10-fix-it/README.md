# 실습 10. 종합 수리

누군가 배포한 앱 `shop` 이 밖에서 열리지 않습니다. 두 군데가 망가져 있습니다. 힌트는 없습니다. 자세한 것은 [HANDS_ON_2.md](../../HANDS_ON_2.md) 의 실습 10 입니다.

```powershell
kubectl apply -f labs/10-fix-it/shop.yaml
```

목표는 브라우저에서 `http://shop.<HOSTS 값>/` 을 열었을 때 `greeting : welcome to shop` 이 나오는 것입니다. 끝나면 `kubectl delete -f labs/10-fix-it/shop.yaml` 로 지웁니다(PVC 가 들어 있어서 디스크도 같이 지워집니다).
