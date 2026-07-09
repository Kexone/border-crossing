package com.prorocketeers.jakub.app.config;

import com.prorocketeers.jakub.country.connector.config.CountryConnectorConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({CountryConnectorConfig.class})
@ComponentScan
public class BorderCrossingAppConfig {
}
