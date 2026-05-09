package com.tyh.chat.rag.embedding.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceBuilder;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.druid.vector", name = "enabled", havingValue = "true")
public class VectorDataSourceConfig {

    @Bean(name = "vectorDataSource")
    @ConfigurationProperties("spring.datasource.druid.vector")
    public DataSource vectorDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
