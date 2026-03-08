package com.instagram.post_service.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient() {
        String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://minio:9000");
        String accessKey = System.getenv().getOrDefault("MINIO_ACCESS_KEY", "admin");
        String secretKey = System.getenv().getOrDefault("MINIO_SECRET_KEY", "password");
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
