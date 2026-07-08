package com.prorocketeers.jakub.border_crossing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI borderCrossingOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Border Crossing API")
				.description("Calculates a land route of border crossings between two countries")
				.version("v1"));
	}
}
