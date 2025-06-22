package com.ucd.urbanflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrafficSensingApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficSensingApplication.class, args);
	}

}
