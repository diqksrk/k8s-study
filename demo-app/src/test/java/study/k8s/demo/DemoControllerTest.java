package study.k8s.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

class DemoControllerTest {

	/**
	 * 컨피그맵으로 SPRING_PROFILES_ACTIVE=dev, APP_GREETING, 시크릿으로 APP_PASSWORD 가 들어온 상황
	 */
	@Nested
	@SpringBootTest(properties = {"app.greeting=hello from configmap", "app.password=s3cr3t-pass"})
	@AutoConfigureMockMvc
	@ActiveProfiles("dev")
	class WithConfigMapAndSecret {

		@Autowired
		MockMvc mockMvc;

		@Test
		void 화면에_프로파일_컨피그맵_시크릿_값이_보인다() throws Exception {
			String body = mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			assertThat(body).contains("profile  : dev\n");
			assertThat(body).contains("stage    : dev-cluster\n");
			assertThat(body).contains("greeting : hello from configmap\n");
			assertThat(body).contains("password : s3****\n");
			assertThat(body).doesNotContain("s3cr3t-pass");
		}

		@Test
		void actuator_env_에서_시크릿이_평문으로_보인다() throws Exception {
			mockMvc.perform(get("/actuator/env/app.password"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("s3cr3t-pass")));
		}
	}

	/**
	 * 아무 설정도 넣지 않은 상황 (실습 1 의 파드)
	 */
	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	class WithoutAnyConfig {

		@Autowired
		MockMvc mockMvc;

		@Test
		void 기본값이_보인다() throws Exception {
			String body = mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			assertThat(body).contains("profile  : (not set)\n");
			assertThat(body).contains("stage    : no-profile\n");
			assertThat(body).contains("greeting : hello from application.yml\n");
			assertThat(body).contains("password : (not set)\n");
		}

		@Test
		void pod_는_파드_이름_한_줄만_돌려준다() throws Exception {
			String body = mockMvc.perform(get("/pod"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			assertThat(body).endsWith("\n");
			assertThat(body.trim()).doesNotContain("\n");
		}
	}

	@Test
	void mask_는_앞_두_글자만_남긴다() {
		assertThat(DemoController.mask("s3cr3t-pass")).isEqualTo("s3****");
		assertThat(DemoController.mask("ab")).isEqualTo("**");
		assertThat(DemoController.mask("")).isEqualTo("(not set)");
		assertThat(DemoController.mask(null)).isEqualTo("(not set)");
	}
}
