package com.prorocketeers.jakub.border_crossing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BorderCrossingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BorderCrossingApplication.class, args);
	}

}
