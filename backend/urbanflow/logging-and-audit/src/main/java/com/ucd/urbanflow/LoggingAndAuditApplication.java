package com.ucd.urbanflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.ucd.urbanflow")
public class LoggingAndAuditApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoggingAndAuditApplication.class, args);
	}

}
