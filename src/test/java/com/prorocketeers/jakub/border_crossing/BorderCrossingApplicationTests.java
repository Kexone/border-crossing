package com.prorocketeers.jakub.border_crossing;

import com.prorocketeers.jakub.border_crossing.client.CountryDataClient;
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
