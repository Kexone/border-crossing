package com.prorocketeers.jakub.app;

import com.prorocketeers.jakub.country.connector.config.CountryConnectorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.prorocketeers.jakub.app",
		"com.prorocketeers.jakub.backend"
})
@EnableScheduling
@Import(CountryConnectorConfig.class)
public class BorderCrossingApplication {

    static void main(String[] args) {
		SpringApplication.run(BorderCrossingApplication.class, args);
	}

}
