package com.prorocketeers.jakub.app;

import com.prorocketeers.jakub.country.connector.CountryDataClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BorderCrossingApplicationTests {

	@MockitoBean
	private CountryDataClient countryDataClient;

	@Test
	void contextLoads() {
	}

}
