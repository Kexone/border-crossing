package com.prorocketeers.jakub.country.connector;

import com.prorocketeers.jakub.country.connector.dto.Country;

import java.util.List;

public interface CountryDataClient {

	List<Country> fetchCountries();
}
