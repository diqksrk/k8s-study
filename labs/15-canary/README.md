# 실습 15. 조금씩 바꾸기 — Argo Rollouts

디플로이먼트 대신 Argo Rollouts 의 Rollout 으로 새 판을 4개 중 1개에만 먼저 내보내고, 보고 나서 나머지를 바꿉니다. 단계는 [HANDS_ON_3.md](../../HANDS_ON_3.md) 의 실습 15 입니다. 명령은 `kubectl argo rollouts …`(압축의 `bin/kubectl-argo-rollouts.exe` · Mac 은 brew)입니다.

| 파일 | 하는 일 |
|---|---|
| `rollout.yaml` | Rollout canary 1.0 × 4 (25% → 멈춤 → 50% → 30초 → 100%) + 서비스 canary. 1.0 을 2.0 으로 고쳐 카나리를 시작합니다 |
| `rollout-broken.yaml` | 고장 14 — 2회차 고장 8 과 같은 새 판을 카나리로 |
