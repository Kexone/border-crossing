package com.prorocketeers.jakub.app.controller;

import com.prorocketeers.jakub.backend.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
				.thenReturn(List.of("CZE", "AUT", "ITA"));

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
		when(routingService.findRoute("CZE", "AUS")).thenReturn(List.of());

		mockMvc.perform(get("/routing/CZE/AUS"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("NO_LAND_ROUTE"))
				.andExpect(jsonPath("$.message").value("No land route from CZE to AUS"));
	}

	@Test
	void returns500ErrorResponseOnUnexpectedError() throws Exception {
		when(routingService.findRoute("CZE", "ITA")).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/routing/CZE/ITA"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("Unexpected server error"));
	}

	@Test
	void returns400WhenCountryCodeTooShort() throws Exception {
		mockMvc.perform(get("/routing/CZ/ITA"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("INVALID_COUNTRY_CODE"))
				.andExpect(jsonPath("$.message")
						.value("origin must match \"[A-Za-z]{3}\""));

		verifyNoInteractions(routingService);
	}

	@Test
	void returns400WhenCountryCodeContainsNonLetters() throws Exception {
		mockMvc.perform(get("/routing/CZE/1TA"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("INVALID_COUNTRY_CODE"))
				.andExpect(jsonPath("$.message")
						.value("destination must match \"[A-Za-z]{3}\""));

		verifyNoInteractions(routingService);
	}

	@Test
	void returns400ForUnknownCountryCode() throws Exception {
		when(routingService.findRoute("CZE", "XXX")).thenReturn(List.of());

		mockMvc.perform(get("/routing/CZE/XXX"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uppercasesCountryCodesBeforeLookup() throws Exception {
		when(routingService.findRoute("CZE", "ITA"))
				.thenReturn(List.of("CZE", "AUT", "ITA"));

		mockMvc.perform(get("/routing/cze/ita"))
				.andExpect(status().isOk());

		verify(routingService).findRoute("CZE", "ITA");
	}
}
