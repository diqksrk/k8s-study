# 실습 13. 로그 · 모니터링

실습 11 에서 다시 시작을 반복한 `slow-broken` 과 CPU 에 눌린 `slow` 를 로그 · 이벤트 · 그래프로 다시 봅니다. 단계는 [HANDS_ON_3.md](../../HANDS_ON_3.md) 의 실습 13 입니다. ③ ~ ⑥ 은 브라우저에서 합니다(주소는 `accounts.txt` 아래쪽).

| 파일 | 하는 일 |
|---|---|
| `demo-logging.yaml` | demo 의 로그 설정만 바꾸는 patch (실습 12 정리에서 `kubectl patch deploy demo --patch-file labs/13-observe/demo-logging.yaml`). 앱 로그를 JSON 한 줄씩, 환경(dev) 표시, 요청마다 한 줄(요청 머리말 `X-Request-Id` 를 `requestId` 칸에). 이미지 · 프로브는 그대로 |

④ 는 Grafana 의 로그 대시보드에서 죽은 컨테이너의 로그까지, ⑤ 는 Grafana 의 Drilldown → Logs 에서 요청 번호 · 레벨로 찾고, ⑥ 은 Grafana 의 Explore 에서 숫자를 묻고 Alerting 에서 알림을 봅니다.
