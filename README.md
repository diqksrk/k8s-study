# 쿠버네티스 스터디 실습

『쿠버네티스 인 액션』 스터디 실습 저장소입니다. 1회차(아키텍처 · Pod · Deployment · Service · ConfigMap · Secret)는 실습 1 ~ 5, 2회차(Ingress · Volume · PV/PVC · HPA · 리소스 · Rolling Update)는 실습 6 ~ 10 입니다.

## 시작 전에

1. 1:1 로 받은 `k8s-study.zip` 을 다운로드 폴더에 저장합니다. 실습 파일, 내 접속 파일(`kubeconfig`), Windows 용 kubectl 이 들어 있습니다. git 은 필요 없습니다.
2. [HANDS_ON.md](HANDS_ON.md) 의 '실습 0 ① 압축 풀고 kubectl 쓰기' 를 내 PC(Windows · Mac)에 맞는 칸대로 칩니다. 한 번만 하면 새 창 · 새 탭에서도 그대로입니다.
3. `cd ~/k8s-study` 로 들어가 `kubectl get pods` 가 `No resources found in k8s-study-… namespace.` 를 내면 준비 끝입니다. 실습 명령은 전부 **이 폴더 맨 위**에서 칩니다.
4. 터미널은 Windows 는 **Windows Terminal + PowerShell**, Mac 은 기본 **터미널**입니다. 막히면 [접속 가이드](docs/접속가이드.md), 자주 쓰는 명령은 [치트시트](docs/치트시트.md).

## 실습 순서

1회차는 **[HANDS_ON.md](HANDS_ON.md)**, 2회차는 **[HANDS_ON_2.md](HANDS_ON_2.md)** 를 위에서부터 따라 하면 됩니다. 단계마다 칠 명령과 나와야 할 화면이 있습니다. 1회차 폴더의 README 에도 같은 실습이 있습니다(2회차 폴더의 README 는 파일 안내만). YAML 은 `# TODO` 가 붙은 줄만 고칩니다.

1. [labs/01-pod](labs/01-pod) — 파드 띄우고 들여다보기, Events 읽기, 고장 1
2. [labs/02-deployment](labs/02-deployment) — 지워도 다시 생기는 파드, 레이블, scale, 잘못된 버전
3. [labs/03-service](labs/03-service) — 서비스 이름으로 접속, DNS, 옆 사람 서비스 부르기, 고장 2
4. [labs/04-config](labs/04-config) — 컨피그맵 · 시크릿 주입, 프로파일 바꾸기, 고장 3 · 4
5. [labs/05-fix-it](labs/05-fix-it) — 종합 수리
6. [labs/06-ingress](labs/06-ingress) — 내 주소로 밖에서 열기, 이름으로 나누기, 열면 안 되는 길 닫기, 고장 5
7. [labs/07-volume](labs/07-volume) — 사라지는 폴더 · 남는 디스크(PVC), 설정을 파일로, 고장 6
8. [labs/08-hpa](labs/08-hpa) — requests · limits, 2 코어 예약, 고장 7, 부하에 늘어나는 파드(HPA)
9. [labs/09-rollout](labs/09-rollout) — 1.0 → 2.0 롤링 업데이트, 되돌리기, 고장 8
10. [labs/10-fix-it](labs/10-fix-it) — 종합 수리

2회차 실습 YAML 의 `__HOST__` 는 사람마다 받는 압축에서 내 주소로 채워져 있습니다.

## 뒤처졌을 때

TODO 를 채운 완성본이 `solutions/` 에 있습니다. 앞 실습의 완성본을 apply 하고 다음 실습으로 넘어가세요.

```powershell
# 실습 3 부터 시작할 때
kubectl apply -f solutions/02-deployment

# 실습 4 · 5 부터 시작할 때
kubectl apply -f solutions/02-deployment -f solutions/03-service
```

## 실습 앱

[demo-app](demo-app) 은 실습에 쓰는 아주 작은 Spring Boot 앱의 소스입니다. 미리 빌드해서 ECR(AWS 의 이미지 저장소)에 올려 두었으니 직접 빌드할 필요는 없습니다. 화면은 이렇게 생겼습니다.

```text
pod      : demo-6d8f9b7c5-x2k4p       ← 응답한 파드 이름
version  : 1.0                        ← 이미지 태그
profile  : dev                        ← SPRING_PROFILES_ACTIVE
stage    : dev-cluster                ← 프로파일별 application-<profile>.yml 값
greeting : hello from configmap       ← 컨피그맵의 APP_GREETING
password : s3****                     ← 시크릿의 APP_PASSWORD (앞 두 글자만)
started  : 2026-11-05T01:23:45Z
```
