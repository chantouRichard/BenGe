package com.bengebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xfyun")
public class XfyunConfig {
    private String appid;
    private String apiPassword;
    private String apiUrl;
}
