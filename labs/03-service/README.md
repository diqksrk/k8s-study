# 실습 3. 서비스

파드 IP 는 계속 바뀌어도 서비스 이름으로는 계속 접속되는 것을 봅니다.

> 실습 2 를 못 끝냈으면 먼저: `kubectl apply -f solutions/02-deployment`

## 1. TODO 채우고 띄우기

`labs/03-service/service.yaml` 의 `# TODO` 를 채우세요.

```powershell
kubectl apply -f labs/03-service/service.yaml -f labs/03-service/client.yaml
kubectl get svc demo
kubectl get pods -o wide
kubectl get endpointslices -l kubernetes.io/service-name=demo
```

- 서비스의 `CLUSTER-IP` — 클러스터 안에서만 쓰는, 바뀌지 않는 IP
- 엔드포인트 슬라이스의 `ENDPOINTS` — 지금 이 서비스 뒤에 있는 파드 IP 들. 위 `get pods -o wide` 의 IP 와 같은지 비교해 보세요

`client` 는 클러스터 안에서 curl 을 쳐 보기 위한 임시 파드입니다.

## 2. 이름으로 부르기

```powershell
kubectl exec client -- curl -s http://demo/
```

여러 번 부르면 응답하는 파드가 바뀝니다:

```powershell
1..6 | ForEach-Object { kubectl exec client -- curl -s http://demo/pod }
```

## 3. 파드를 전부 갈아엎어도

```powershell
kubectl delete pods -l app=demo
kubectl get pods -o wide
kubectl get endpointslices -l kubernetes.io/service-name=demo
1..6 | ForEach-Object { kubectl exec client -- curl -s http://demo/pod }
```

파드 IP 는 전부 바뀌었는데 `http://demo` 는 그대로 됩니다.

> 새 파드가 막 떴을 때 몇 초 동안은 실패할 수 있습니다. 앱이 아직 준비가 안 됐는데 쿠버네티스는 그걸 모르기 때문입니다. 3회차 Readiness Probe 의 주제입니다.

## 4. DNS 이름의 정체

```powershell
kubectl exec client -- cat /etc/resolv.conf
```

`search` 줄에 `<내 네임스페이스>.svc.<클러스터 도메인>` 이 있어서 `demo` 만 써도 찾아집니다. 전체 이름으로도 불러 보세요 (도메인은 위 `search` 줄에 나온 것):

```powershell
kubectl exec client -- curl -s http://demo.<내 네임스페이스>.svc.cluster.local/pod
```

이제 **옆 사람의 서비스**를 불러 봅니다. 옆 사람에게 네임스페이스 이름을 물어보세요:

```powershell
kubectl exec client -- curl -s http://demo.<옆 사람 네임스페이스>/pod
```

옆 사람 파드의 이름이 나옵니다. `서비스.네임스페이스` 로 다른 네임스페이스를 부를 수 있습니다.

## 5. 고장 2 — 접속이 안 되는 서비스

```powershell
kubectl apply -f labs/03-service/service-broken.yaml
kubectl exec client -- curl -sS -m 3 http://demo-broken/
```

접속이 안 됩니다. 원인을 찾아 고쳐 보세요. 실마리:

```powershell
kubectl get endpointslices -l kubernetes.io/service-name=demo-broken
kubectl describe svc demo-broken
kubectl get pods --show-labels
```

고쳤으면 다시 apply 하고 curl 이 되는지 확인.

## 6. (참고) 내 PC 에서 서비스로

```powershell
kubectl port-forward svc/demo 18080:80
```

port-forward 는 서비스 뒤의 **파드 하나에만** 붙습니다. 부하 분산은 안 되는, 개발용 통로입니다. 밖에서 정식으로 들어오는 길(인그레스)은 2회차에서 다룹니다.

## 정리

```powershell
kubectl delete -f labs/03-service/service-broken.yaml
```

서비스 `demo` 와 파드 `client` 는 실습 4 · 5 에서 씁니다. **지우지 마세요.**
