# 실습 11. 프로브 · 끊김 없는 교체

쿠버네티스에 '준비됐나 · 살아 있나 · 다 떴나' 를 묻는 장치(프로브)를 붙이고, 2회차 고장 8 과 실습 9 의 FAIL 이 어떻게 막히는지 봅니다. 단계는 [HANDS_ON_3.md](../../HANDS_ON_3.md) 의 실습 11 입니다. 터미널 탭을 두 개 씁니다.

| 파일 | 하는 일 |
|---|---|
| `deployment.yaml` | demo 1.0 × 3 + readinessProbe. `# TODO` 에 묻는 주소를 채웁니다 |
| `deployment-badport.yaml` | 고장 9 — 2회차 고장 8(2.0 + SERVER_PORT 9090)에 readiness 를 붙인 판 |
| `deployment-zero.yaml` | readiness + 꺼지기 전 5초 기다리기(preStop). 1.0 두 군데를 2.0 으로 고쳐 교체합니다 |
| `slow-broken.yaml` | 고장 10 — CPU 를 묶어 느리게 뜨는 앱을 살아 있나 확인(liveness)이 다시 시작시킴 |
| `slow.yaml` | 같은 앱에 startupProbe. `# TODO` 에 기다릴 횟수를 채웁니다 |
