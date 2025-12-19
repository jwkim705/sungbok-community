package com.sungbok.community.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * OCI Object Storage S3 Client 설정
 * Bean 생성 전용 Configuration
 *
 * @since 0.0.1
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OciStorageClientConfig {

    private final OciStorageProperties properties;

    /**
     * AWS 자격 증명 생성 (공통)
     *
     * @return StaticCredentialsProvider
     */
    private StaticCredentialsProvider createCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAuth().getAccessKey(),
                properties.getAuth().getSecretKey()
        );
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * S3 Configuration 생성 (공통)
     * OCI Object Storage 호환성 설정
     *
     * @param includeChunkedEncoding chunkedEncodingEnabled 설정 포함 여부
     * @return S3Configuration
     */
    private S3Configuration createS3Configuration(boolean includeChunkedEncoding) {
        S3Configuration.Builder builder = S3Configuration.builder()
                .pathStyleAccessEnabled(true);  // OCI 필수

        if (includeChunkedEncoding) {
            builder.chunkedEncodingEnabled(false);  // 성능 최적화
        }

        return builder.build();
    }

    /**
     * S3 Client Bean (OCI S3 호환 API)
     */
    @Bean
    public S3Client s3Client() {
        log.info("[OCI S3] S3Client 초기화: endpoint={}, bucket={}, region={}",
                properties.getEndpoint(), properties.getBucketName(), properties.getRegion());

        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(createCredentialsProvider())
                .serviceConfiguration(createS3Configuration(true))
                .build();
    }

    /**
     * S3 Presigner Bean (Pre-signed URL 생성용)
     */
    @Bean
    public S3Presigner s3Presigner() {
        log.info("[OCI S3] S3Presigner 초기화");

        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(createCredentialsProvider())
                .serviceConfiguration(createS3Configuration(false))
                .build();
    }
}
