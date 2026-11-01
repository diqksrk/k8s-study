# 실습 8. 리소스 · HPA

파드에 적어 두는 두 값(requests · limits)이 어디에 쓰이는지 보고, 부하를 걸어 파드 수가 저절로 늘어나는 것을 봅니다. 단계는 [HANDS_ON_2.md](../../HANDS_ON_2.md) 의 실습 8 입니다. 터미널 탭을 두 개 씁니다.

| 파일 | 하는 일 |
|---|---|
| `deployment.yaml` | demo 에 requests(CPU 100m · 메모리 256Mi) · limits(메모리 384Mi). replicas 는 적지 않습니다(HPA 가 정함) |
| `big.yaml` | CPU 2 코어를 예약하는 파드 — 노드에 들어갈까? |
| `oom.yaml` | 고장 7 — 떴다가 계속 다시 시작하는 파드 |
| `hpa.yaml` | demo 를 1 ~ 5개 사이로 늘리고 줄이는 규칙. `# TODO` 에 목표 사용률을 채웁니다 |
| `load.yaml` | demo 를 쉬지 않고 부르는 부하 파드 하나 (CPU 상한 300m) |
