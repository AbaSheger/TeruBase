package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(TeruBaseProperties.class)
@ConditionalOnProperty(name = "terubase.enabled", havingValue = "true", matchIfMissing = true)
public class TeruBaseAutoConfiguration {

    private static final Logger LOGGER = Logger.getLogger(TeruBaseAutoConfiguration.class.getName());

    @Bean(name = "teruBaseDataSource")
    @ConditionalOnMissingBean(name = "teruBaseDataSource")
    public DataSource teruBaseDataSource(TeruBaseProperties properties) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(properties.getJdbcUrl());
        dataSource.setUser(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        LOGGER.info(() -> "TeruBase isolated datasource initialized: " + properties.getJdbcUrl());
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public TeruBaseSqlService teruBaseSqlService(DataSource teruBaseDataSource) {
        return new TeruBaseSqlService(teruBaseDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public TeruBaseSchemaService teruBaseSchemaService(ApplicationContext applicationContext, TeruBaseProperties properties) {
        return new TeruBaseSchemaService(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TeruBaseOpenAiClient teruBaseOpenAiClient(ObjectMapper objectMapper, TeruBaseProperties properties) {
        return new TeruBaseOpenAiClient(objectMapper, properties, Executors.newVirtualThreadPerTaskExecutor());
    }
}
