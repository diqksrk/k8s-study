# 실습 4. 컨피그맵과 시크릿

이미지는 그대로 두고 설정만 바꿔서 앱 동작을 바꿉니다.

지금 우리는 `-Pprofile=dev` 처럼 **빌드할 때** 설정을 골라 이미지에 넣습니다. 여기서는 이미지는 하나만 두고, 프로파일(`SPRING_PROFILES_ACTIVE`)과 설정값을 **실행할 때** 바깥에서 넣습니다.

> 실습 3 을 못 끝냈으면 먼저: `kubectl apply -f solutions/02-deployment -f solutions/03-service`

## 1. 컨피그맵 · 시크릿 만들기

```powershell
kubectl apply -f labs/04-config/configmap.yaml -f labs/04-config/secret.yaml
kubectl get configmap demo-config -o yaml
kubectl get secret demo-secret -o yaml
```

시크릿의 `data` 는 알아보기 힘든 문자열로 바뀌어 있습니다. 그런데 이건 암호화가 아니라 base64 인코딩일 뿐입니다. 풀어 보면:

```powershell
$b64 = kubectl get secret demo-secret -o jsonpath='{.data.APP_PASSWORD}'
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
```

## 2. 디플로이먼트에 넣기

`labs/04-config/deployment.yaml` 의 `# TODO` 두 줄을 채우고:

```powershell
kubectl apply -f labs/04-config/deployment.yaml
kubectl get pods -w
```

파드가 새 것으로 교체되는 걸 보고 `Ctrl+C`.

```powershell
kubectl exec client -- curl -s http://demo/
```

`profile` 이 `dev`, `stage` 가 `dev-cluster`, `greeting` 이 컨피그맵 값, `password` 에 앞 두 글자가 보이면 성공입니다.

앱 코드는 컨피그맵을 전혀 모릅니다. Spring 이 환경변수 `APP_GREETING` 을 `app.greeting` 설정으로 알아서 읽을 뿐입니다.

## 3. 설정을 바꾸면?

`configmap.yaml` 의 `APP_GREETING` 값을 아무거나로 바꾸고:

```powershell
kubectl apply -f labs/04-config/configmap.yaml
kubectl exec client -- curl -s http://demo/
```

그대로입니다. 환경변수는 컨테이너가 **시작할 때 한 번** 들어가기 때문입니다. 파드를 새로 띄우면:

```powershell
kubectl rollout restart deployment demo
kubectl rollout status deployment demo
kubectl exec client -- curl -s http://demo/
```

이번엔 바뀝니다.

프로파일도 바꿔 보세요. `SPRING_PROFILES_ACTIVE` 를 `real` 로 바꾸고 → apply → `rollout restart` → `stage` 가 `real-cluster` 가 됩니다. 그동안 이미지는 한 번도 다시 만들지 않았습니다.

## 4. 시크릿은 숨겨지지 않는다

```powershell
kubectl exec deploy/demo -- env
```

`APP_PASSWORD` 가 평문으로 그대로 보입니다. 브라우저로도 봅니다:

```powershell
kubectl port-forward svc/demo 18080:80
```

<http://localhost:18080/actuator/env> 를 열고 `APP_PASSWORD` 를 찾아보세요.

시크릿은 값을 숨기는 장치가 아니라, 설정과 비밀을 **나눠서 권한을 따로 줄 수 있게** 하는 장치입니다. 운영에서는 KMS 같은 외부 저장소와 연동해서 씁니다.

> 실습 앱은 보여 주려고 일부러 `/actuator/env` 를 열어 두었습니다. 실제 서비스에서는 절대 이렇게 열지 마세요.

## 5. 고장 3 — 컨테이너가 안 만들어진다

```powershell
kubectl apply -f labs/04-config/broken-missing-config.yaml
kubectl get pods
```

`demo-missing` 파드의 `STATUS` 를 보고 원인을 찾아 고쳐 보세요.

```powershell
kubectl describe pod -l app=demo-missing
```

## 6. 고장 4 — 떴다가 계속 죽는다

```powershell
kubectl apply -f labs/04-config/broken-crash.yaml
kubectl get pods -w
```

`RESTARTS` 가 올라가는 걸 보고 `Ctrl+C`. 실무에서 가장 자주 보게 될 상태입니다. 이번엔 Events 보다 앱 로그가 실마리입니다:

```powershell
kubectl logs deploy/demo-crash
kubectl logs deploy/demo-crash --previous
```

`--previous` 는 **직전에 죽은** 컨테이너의 로그입니다. 원인을 찾아 `broken-crash.yaml` 을 고치고 apply 한 다음, 파드를 새로 띄웁니다:

```powershell
kubectl rollout restart deployment demo-crash
kubectl get pods -w
```

> 쿠버네티스는 계속 죽는 컨테이너를 점점 긴 간격(최대 5분)으로 다시 띄웁니다. 기다리지 말고 `rollout restart` 로 바로 새 파드를 띄웁니다.

## 정리

```powershell
kubectl delete -f labs/04-config/broken-missing-config.yaml -f labs/04-config/broken-crash.yaml
```
