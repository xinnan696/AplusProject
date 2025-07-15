package com.ucd.urbanflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.ucd.urbanflow.mapper")
public class TrafficSensingApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficSensingApplication.class, args);
	}

}
