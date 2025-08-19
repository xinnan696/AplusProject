package com.ucd.urbanflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.ucd.urbanflow.client")
public class SignalControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignalControlApplication.class, args);
	}

}
