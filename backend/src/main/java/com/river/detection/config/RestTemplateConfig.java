package com.river.detection.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Autowired
    private InferenceProperties inferenceProperties;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(inferenceProperties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(inferenceProperties.getReadTimeoutMs()))
                .build();
    }
}

