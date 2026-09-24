package com.hr.assistance;

import com.hr.assistance.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class AssistanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistanceApplication.class, args);
	}

}
