package com.cinefinder.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.cinefinder.theater.data.repository")
public class ElasticsearchConfig {

}
