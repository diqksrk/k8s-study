# 실습 6. 인그레스

1회차에는 클러스터 안의 curl 파드에서만 닿던 앱을 내 PC 의 브라우저에서 엽니다. 단계는 [HANDS_ON_2.md](../../HANDS_ON_2.md) 의 실습 6 입니다. 명령은 모두 **저장소 맨 위 폴더(`~/k8s-study`)**에서 칩니다.

| 파일 | 하는 일 |
|---|---|
| `ingress.yaml` | 내 주소(host)로 오면 서비스 `demo` 로 보내는 입구 규칙. `# TODO` 한 줄을 채웁니다 |
| `demo-v2.yaml` | 같은 입구 뒤에 세울 두 번째 앱(2.0)과 그 서비스 |
| `ingress-two.yaml` | 주소 앞에 `v2.` 가 붙으면 `demo-v2` 로 보내는 규칙을 더한 것 |
| `ingress-safe.yaml` | 밖으로 열 길만 남긴 규칙 (`/` 와 `/pod` — `/actuator` 는 닫힘) |
| `ingress-broken.yaml` | 고장 5 — `broken.` 주소가 열리지 않습니다 |

`host` 줄의 `__HOST__` 는 실습 0 ① (내 주소 채우기)에서 내 주소로 바뀝니다(`__HOST__` 그대로면 실습 0 ① 을 다시 칩니다). 내 주소는 `kubectl get ingress demo` 의 HOSTS 입니다.
