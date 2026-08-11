package com.example.demo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.time.Duration;

@Configuration
public class LambdaConfig {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.of(region))
                .httpClient(ApacheHttpClient.builder()
                        .socketTimeout(Duration.ofMinutes(6))
                        .connectionTimeout(Duration.ofSeconds(10))
                        .build())
                .build();
    }
}
