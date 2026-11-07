# 실습 14. GitOps — Argo CD

내 git 저장소(Gitea)의 파일을 브라우저에서 고쳐 커밋하면 Argo CD 가 내 칸을 그 파일대로 맞춥니다. 단계는 [HANDS_ON_3.md](../../HANDS_ON_3.md) 의 실습 14 입니다. 아이디 · 비밀번호는 압축 안 `accounts.txt` 에 있습니다(Gitea · Argo CD 가 같음).

내 저장소의 `app/` 은 **헬름 차트**입니다. 틀(`templates/`) 하나에 값 파일을 끼워 Argo CD 가 YAML 을 만들어 냅니다. 고치는 파일은 `app/values-dev.yaml` 하나입니다.

| 파일 | 하는 일 |
|---|---|
| `app/values-dev.yaml` | 이 칸(dev)의 값 — 이미지 판(`tag`) · 파드 수(`replicaCount`) · 내 주소. **실습 14 에서 고치는 파일** |
| `app/values.yaml` | 모든 환경에 같은 값 — 이미지 저장소 주소 |
| `app/templates/deployment.yaml` · `service.yaml` · `ingress.yaml` | 틀. 값이 들어갈 자리만 `{{ .Values.… }}` 로 비워 둔 YAML |
| `app/Chart.yaml` | 차트 이름 · 판 |

이 폴더의 파일은 내 Gitea 저장소 `k8s-study-gitops` 의 `app/` 과 같은 내용 — **참고용 사본**입니다. 고치는 것은 브라우저의 Gitea 에서 합니다.
