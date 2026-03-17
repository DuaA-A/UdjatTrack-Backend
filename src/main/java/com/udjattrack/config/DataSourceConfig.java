package com.udjattrack.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.relational")
    public DataSourceProperties relationalDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.relational.hikari")
    public DataSource relationalDataSource() {
        return relationalDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.timeseries")
    public DataSourceProperties timeseriesDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("timeseriesDataSource")
    @ConfigurationProperties("spring.datasource.timeseries.hikari")
    public DataSource timeseriesDataSource() {
        return timeseriesDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
