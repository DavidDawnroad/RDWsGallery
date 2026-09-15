package com.test.prac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing // BaseEntity.java 관련 Auditing 기능 활성화 명시
public class PracApplication {

	public static void main(String[] args) {
		SpringApplication.run(PracApplication.class, args);
	}

}
