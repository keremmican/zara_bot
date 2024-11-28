package com.mican.zara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZaraApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZaraApplication.class, args);
	}

}
