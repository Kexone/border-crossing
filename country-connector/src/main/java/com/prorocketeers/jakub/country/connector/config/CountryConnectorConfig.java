package com.prorocketeers.jakub.country.connector.config;

import com.prorocketeers.jakub.country.connector.CountryDataClient;
import com.prorocketeers.jakub.country.connector.CountryDataClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
public class CountryConnectorConfig {

    @Bean
    @ConditionalOnMissingBean
    public CountryDataClient countryDataClient(ObjectMapper objectMapper,
            @Value("${countries.data.url}") String countriesUrl,
            @Value("${countries.data.connect-timeout:5s}") Duration connectTimeout,
            @Value("${countries.data.read-timeout:30s}") Duration readTimeout) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        var restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        return new CountryDataClientImpl(objectMapper, restClient, countriesUrl);
    }
}
