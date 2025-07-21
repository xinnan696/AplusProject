package com.ucd.urbanflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.ucd.urbanflow.mapper")
public class StatusSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatusSyncApplication.class, args);
	}

}
