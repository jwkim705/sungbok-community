package com.sungbok.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OCI Object Storage 설정 Properties
 * ConfigurationProperties 전용 클래스
 *
 * @since 0.0.1
 */
@Component
@ConfigurationProperties(prefix = "oci.storage")
@Getter
@Setter
public class OciStorageProperties {

    private String namespace;
    private String bucketName;
    private String region;
    private String endpoint;
    private Auth auth;
    private Cdn cdn;
    private Integer presignedUrlExpiration;
    private MaxFileSize maxFileSize;
    private List<String> allowedMimeTypes;

    @Getter
    @Setter
    public static class Auth {
        private String accessKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class Cdn {
        private boolean enabled = true;  // primitive + 기본값
        private String provider;
        private String domain;
    }

    @Getter
    @Setter
    public static class MaxFileSize {
        private Long defaultSize;
        private Long video;
    }
}
