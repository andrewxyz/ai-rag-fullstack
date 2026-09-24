package com.hr.assistance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running infrastructure: PostgreSQL + Groq API key")
class AssistanceApplicationTests {

	@Test
	void contextLoads() {
	}

}
