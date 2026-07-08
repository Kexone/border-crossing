package com.prorocketeers.jakub.border_crossing.controller;

import com.prorocketeers.jakub.border_crossing.dto.RouteResponse;
import com.prorocketeers.jakub.border_crossing.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@Tag(name = "Routing", description = "Land routes between countries")
public class RoutingController {

	private final RoutingService routingService;

	public RoutingController(RoutingService routingService) {
		this.routingService = routingService;
	}

	@Operation(summary = "Find the shortest land route between two countries",
			description = "Returns the shortest sequence of border crossings from origin to destination, "
					+ "both identified by their cca3 country codes (case-insensitive).")
	@ApiResponse(responseCode = "200", description = "Route found")
	@ApiResponse(responseCode = "400", description = "Unknown country code or no land route exists",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/routing/{origin}/{destination}")
	public RouteResponse route(
			@Parameter(description = "cca3 code of the origin country", example = "CZE")
			@PathVariable String origin,
			@Parameter(description = "cca3 code of the destination country", example = "ITA")
			@PathVariable String destination) {
		String from = origin.toUpperCase(Locale.ROOT);
		String to = destination.toUpperCase(Locale.ROOT);
		return routingService
				.findRoute(from, to)
				.map(RouteResponse::new)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"No land route from %s to %s".formatted(from, to)));
	}
}
