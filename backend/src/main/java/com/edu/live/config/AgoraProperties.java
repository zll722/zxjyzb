package com.edu.live.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agora")
public class AgoraProperties {
    private String appId;
    private String appCertificate;
    /** Token 有效期（秒），默认 86400（24小时）*/
    private Integer tokenExpirationSeconds = 86400;
}
