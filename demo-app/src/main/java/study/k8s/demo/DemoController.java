package study.k8s.demo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실습 화면. 어느 파드가 응답했는지와, 설정이 어디서 왔는지(프로파일 · 컨피그맵 · 시크릿)를 한 화면에 보여 준다.
 * 출력은 PowerShell 콘솔에서 글자가 깨지지 않도록 ASCII 로만 쓴다.
 */
@RestController
public class DemoController {

	private static final String NOT_SET = "(not set)";

	private final Environment environment;
	private final String version;
	private final String greeting;
	private final String stage;
	private final String password;
	private final Instant startedAt;

	public DemoController(
		Environment environment,
		ObjectProvider<BuildProperties> buildProperties,
		@Value("${app.greeting}") String greeting,
		@Value("${app.stage}") String stage,
		@Value("${app.password}") String password
	) {
		this.environment = environment;
		BuildProperties properties = buildProperties.getIfAvailable();
		if (properties != null) {
			this.version = properties.getVersion();
		} else {
			// IDE 에서 build-info 없이 실행한 경우
			this.version = "dev";
		}
		this.greeting = greeting;
		this.stage = stage;
		this.password = password;
		this.startedAt = Instant.now();
	}

	@GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	public String home() {
		StringBuilder body = new StringBuilder();
		body.append("pod      : ").append(podName()).append('\n');
		body.append("version  : ").append(version).append('\n');
		body.append("profile  : ").append(activeProfiles()).append('\n');
		body.append("stage    : ").append(stage).append('\n');
		body.append("greeting : ").append(greeting).append('\n');
		body.append("password : ").append(mask(password)).append('\n');
		body.append("started  : ").append(startedAt).append('\n');
		return body.toString();
	}

	/**
	 * 서비스 부하 분산을 볼 때 쓰는 짧은 응답. 파드 이름만 돌려준다.
	 */
	@GetMapping(value = "/pod", produces = MediaType.TEXT_PLAIN_VALUE)
	public String pod() {
		return podName() + "\n";
	}

	/**
	 * 쿠버네티스는 컨테이너의 HOSTNAME 환경변수를 파드 이름으로 넣어 준다.
	 */
	private String podName() {
		String hostname = System.getenv("HOSTNAME");
		if (hostname != null && !hostname.isBlank()) {
			return hostname;
		}
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "unknown";
		}
	}

	private String activeProfiles() {
		String[] profiles = environment.getActiveProfiles();
		if (profiles.length == 0) {
			return NOT_SET;
		}
		return String.join(",", profiles);
	}

	/**
	 * 시크릿 값은 앞 두 글자만 보여 준다. 값이 들어왔다는 것만 확인하는 용도.
	 */
	static String mask(String value) {
		if (value == null || value.isEmpty()) {
			return NOT_SET;
		}
		if (value.length() <= 2) {
			return "**";
		}
		return value.substring(0, 2) + "****";
	}
}
