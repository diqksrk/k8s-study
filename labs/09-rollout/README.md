# 실습 9. 롤링 업데이트

탭 ① 에 0.5초마다 demo 의 버전을 찍는 반복을 켜 두고, 탭 ② 에서 1.0 을 2.0 으로 바꿉니다. 단계는 [HANDS_ON_2.md](../../HANDS_ON_2.md) 의 실습 9 입니다.

| 파일 | 하는 일 |
|---|---|
| `deployment.yaml` | demo 1.0 × 3, 하나씩 바꾸는 규칙(maxSurge 1 · maxUnavailable 0). 파일 안의 `1.0` 두 군데를 `2.0` 으로 고쳐 apply 하면 배포가 시작됩니다 |
| `deployment-broken.yaml` | 고장 8 — 배포는 끝났다는데 접속이 안 되는 새 판 |

막히면 먼저 `kubectl rollout undo deploy/demo` 로 되돌립니다.
