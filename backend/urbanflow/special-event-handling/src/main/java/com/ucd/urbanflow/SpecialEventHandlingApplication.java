package com.ucd.urbanflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Special Event Handling Application
 * Main application class for special event handling module
 */
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
public class SpecialEventHandlingApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpecialEventHandlingApplication.class, args);
	}

}