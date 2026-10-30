# demo-app

스터디 실습용 Spring Boot 앱입니다 (Java 21, Spring Boot 3.4.2). 하는 일은 두 가지뿐입니다.

- `GET /` — 응답한 파드 이름, 버전, 프로파일, 설정값(컨피그맵 · 시크릿에서 온 값)을 텍스트로 보여 줌
- `GET /pod` — 파드 이름 한 줄 (서비스 부하 분산 확인용)

그 밖에 `/actuator/health`, `/actuator/env` 가 열려 있습니다. `/actuator/env` 는 시크릿이 평문으로 보인다는 것을 보여 주려고 **일부러** 값을 다 보이게 열었습니다. 실제 서비스에서는 이렇게 하지 마세요.

## 설정이 들어오는 길

| 환경변수 | 설정 | 기본값 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 (`dev`, `real`) | 없음 |
| `APP_GREETING` | `app.greeting` | `hello from application.yml` |
| `APP_PASSWORD` | `app.password` | 빈 값 |
| (프로파일 파일) | `app.stage` | `no-profile`, dev → `dev-cluster`, real → `real-cluster` |
| `SERVER_PORT` | `server.port` | `8080` |

## 빌드 (진행자)

Docker 없이 Jib 으로 ECR 에 바로 올립니다. PowerShell 에서는 `-P` · `-D` 인자를 **반드시 큰따옴표로** 감쌉니다 (안 감싸면 `1.0` 이 쪼개집니다).

```powershell
$registry = "<계정 ID>.dkr.ecr.ap-northeast-2.amazonaws.com"
$password = aws ecr get-login-password --region ap-northeast-2
.\gradlew.bat test
.\gradlew.bat jib "-PappVersion=1.0" "-Pregistry=$registry" "-Djib.to.auth.username=AWS" "-Djib.to.auth.password=$password"
.\gradlew.bat jib "-PappVersion=2.0" "-Pregistry=$registry" "-Djib.to.auth.username=AWS" "-Djib.to.auth.password=$password"
```

베이스 이미지는 기본으로 Docker Hub 의 `eclipse-temurin:21-jre` 를 씁니다. 다른 곳에서 받아야 하면 `"-PbaseImage=<이미지>"` 를 붙입니다.
