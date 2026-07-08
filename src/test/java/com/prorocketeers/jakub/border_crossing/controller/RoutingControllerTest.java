package com.prorocketeers.jakub.border_crossing.controller;

import com.prorocketeers.jakub.border_crossing.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutingController.class)
class RoutingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RoutingService routingService;

	@Test
	void returnsRouteAsJson() throws Exception {
		when(routingService.findRoute("CZE", "ITA"))
				.thenReturn(Optional.of(List.of("CZE", "AUT", "ITA")));

		mockMvc.perform(get("/routing/CZE/ITA"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.route").isArray())
				.andExpect(jsonPath("$.route[0]").value("CZE"))
				.andExpect(jsonPath("$.route[1]").value("AUT"))
				.andExpect(jsonPath("$.route[2]").value("ITA"));
	}

	@Test
	void returns400WhenNoRouteExists() throws Exception {
		when(routingService.findRoute("CZE", "AUS")).thenReturn(Optional.empty());

		mockMvc.perform(get("/routing/CZE/AUS"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.detail").value("No land route from CZE to AUS"));
	}

	@Test
	void returns500ProblemDetailOnUnexpectedError() throws Exception {
		when(routingService.findRoute("CZE", "ITA")).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/routing/CZE/ITA"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.detail").value("Unexpected server error"));
	}

	@Test
	void returns400ForUnknownCountryCode() throws Exception {
		when(routingService.findRoute("CZE", "XXX")).thenReturn(Optional.empty());

		mockMvc.perform(get("/routing/CZE/XXX"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uppercasesCountryCodesBeforeLookup() throws Exception {
		when(routingService.findRoute("CZE", "ITA"))
				.thenReturn(Optional.of(List.of("CZE", "AUT", "ITA")));

		mockMvc.perform(get("/routing/cze/ita"))
				.andExpect(status().isOk());

		verify(routingService).findRoute("CZE", "ITA");
	}
}
