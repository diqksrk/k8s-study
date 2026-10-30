# 쿠버네티스 스터디 1회차 실습

『쿠버네티스 인 액션』 스터디 1회차(아키텍처 · Pod · Deployment · Service · ConfigMap · Secret) 실습 저장소입니다.

## 시작 전에

1. [접속 가이드](docs/접속가이드.md) 대로 접속을 확인해 두세요. `kubectl get pods` 가 `No resources found in <내 네임스페이스> namespace.` 를 내면 준비 끝입니다.
2. 셸은 **Windows Terminal + PowerShell** 로 통일합니다. 자주 쓰는 명령은 [치트시트](docs/치트시트.md) 에 있습니다.
3. 터미널은 **이 저장소 루트**에서 여세요. 실습 명령의 경로는 모두 루트 기준입니다.

## 실습 순서

**[실습 가이드 HANDS_ON.md](HANDS_ON.md)** 를 위에서부터 따라 하면 됩니다. 단계마다 칠 명령과 나와야 할 화면이 있습니다. 각 폴더의 README 에도 같은 실습이 있습니다. YAML 은 `# TODO` 가 붙은 줄만 고칩니다.

1. [labs/01-pod](labs/01-pod) — 파드 띄우고 들여다보기, Events 읽기, 고장 1
2. [labs/02-deployment](labs/02-deployment) — 지워도 다시 생기는 파드, 레이블, scale, 잘못된 버전
3. [labs/03-service](labs/03-service) — 서비스 이름으로 접속, DNS, 옆 사람 서비스 부르기, 고장 2
4. [labs/04-config](labs/04-config) — 컨피그맵 · 시크릿 주입, 프로파일 바꾸기, 고장 3 · 4
5. [labs/05-fix-it](labs/05-fix-it) — 종합 수리

## 뒤처졌을 때

TODO 를 채운 완성본이 `solutions/` 에 있습니다. 앞 실습의 완성본을 apply 하고 다음 실습으로 넘어가세요.

```powershell
# 실습 3 부터 시작할 때
kubectl apply -f solutions/02-deployment

# 실습 4 · 5 부터 시작할 때
kubectl apply -f solutions/02-deployment -f solutions/03-service
```

## 실습 앱

[demo-app](demo-app) 은 실습에 쓰는 아주 작은 Spring Boot 앱의 소스입니다. 진행자가 미리 빌드해서 ECR(AWS 의 이미지 저장소)에 올려 두었으니 직접 빌드할 필요는 없습니다. 화면은 이렇게 생겼습니다.

```text
pod      : demo-6d8f9b7c5-x2k4p       ← 응답한 파드 이름
version  : 1.0                        ← 이미지 태그
profile  : dev                        ← SPRING_PROFILES_ACTIVE
stage    : dev-cluster                ← 프로파일별 application-<profile>.yml 값
greeting : hello from configmap       ← 컨피그맵의 APP_GREETING
password : s3****                     ← 시크릿의 APP_PASSWORD (앞 두 글자만)
started  : 2026-11-05T01:23:45Z
```
