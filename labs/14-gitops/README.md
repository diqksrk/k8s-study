# 실습 14. GitOps — Argo CD

내 git 저장소(Gitea)의 파일을 브라우저에서 고쳐 커밋하면 Argo CD 가 내 칸을 그 파일대로 맞춥니다. 단계는 [HANDS_ON_3.md](../../HANDS_ON_3.md) 의 실습 14 입니다. 아이디 · 비밀번호는 압축 안 `accounts.txt` 에 있습니다(Gitea · Argo CD 가 같음).

| 파일 | 하는 일 |
|---|---|
| `app/deployment.yaml` · `app/service.yaml` · `app/ingress.yaml` | 내 Gitea 저장소 `k8s-study-gitops` 의 `app/` 과 같은 내용 — **참고용 사본**입니다. 고치는 것은 브라우저의 Gitea 에서 합니다 |
