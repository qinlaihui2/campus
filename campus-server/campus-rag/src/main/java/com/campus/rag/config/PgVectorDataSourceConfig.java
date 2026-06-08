package com.campus.rag.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class PgVectorDataSourceConfig {

    @Value("${spring.datasource-pg.driver-class-name}")
    private String pgDriverClassName;

    @Value("${spring.datasource-pg.url}")
    private String pgUrl;

    @Value("${spring.datasource-pg.username}")
    private String pgUsername;

    @Value("${spring.datasource-pg.password}")
    private String pgPassword;

    @Primary
    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    public DataSource pgDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(pgDriverClassName)
                .url(pgUrl)
                .username(pgUsername)
                .password(pgPassword)
                .build();
    }
}
