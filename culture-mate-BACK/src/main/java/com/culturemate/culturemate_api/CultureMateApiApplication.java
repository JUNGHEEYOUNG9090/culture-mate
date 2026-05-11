package com.culturemate.culturemate_api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class CultureMateApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CultureMateApiApplication.class, args);
		log.info("CultureMate API 서버가 성공적으로 시작되었습니다.");
	}

}
