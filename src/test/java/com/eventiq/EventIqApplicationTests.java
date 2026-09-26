package com.eventiq;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Needs a running Oracle database and a Gemini API key, which CI does not provide")
@SpringBootTest
class EventIqApplicationTests {

	@Test
	void contextLoads() {
	}

}
