package com.river.detection.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "inference")
public class InferenceProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:8000";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
    // 回调基础地址，用于推理服务回调进度
    private String callbackBase = "http://localhost:8080";
}
