package com.river.detection;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.river.detection.mapper")
public class RiverDetectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiverDetectionApplication.class, args);
    }
}

