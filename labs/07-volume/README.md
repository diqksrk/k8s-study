# 실습 7. 볼륨 · PV · PVC

메모를 남기는 작은 파드 `note` 로, 파일이 폴더 종류에 따라 사라지기도 하고 남기도 하는 것을 봅니다. 단계는 [HANDS_ON_2.md](../../HANDS_ON_2.md) 의 실습 7 입니다.

| 파일 | 하는 일 |
|---|---|
| `note-emptydir.yaml` | `/data` 가 임시 폴더(emptyDir)인 note — 파드를 지우면 메모도 사라짐 |
| `pvc.yaml` | 디스크 청구서 `note-data` (gp3 · 1Gi · 노드 하나) |
| `note-pvc.yaml` | 같은 note 를 디스크(PVC)에 쓰게 바꾼 것. `# TODO` 한 줄(청구서 이름)을 채웁니다 |
| `config-file.yaml` · `config-v2.yaml` | 컨피그맵을 파일로 넣는 파드 `cfgfile` 과, 값만 바꾼 컨피그맵 |
| `pvc-broken.yaml` | 고장 6 — 디스크를 못 받는 파드 |

PVC 를 만들면 AWS 에 실제 디스크(EBS)가 생깁니다. 끝나면 꼭 지웁니다 — PVC 를 지우면 디스크도 같이 지워집니다.
