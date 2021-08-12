package io.opensaber.registry.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {
    @Value("${filestorage.url}")
    String url;
    @Value("${filestorage.accesskey}")
    String accessKey;
    @Value("${filestorage.secretkey}")
    String secretKey;
    @Value("${filestorage.bucketname}")
    String bucketName;

    @Bean("minioClient")
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
