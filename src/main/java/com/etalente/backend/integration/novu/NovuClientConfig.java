package com.etalente.backend.integration.novu;

import co.novu.common.base.Novu;
import co.novu.common.base.NovuConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NovuClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(NovuClientConfig.class);

    @Value("${novu.api-key}")
    private String apiKey;

    @Value("${novu.backend-url:https://api.novu.co/v1/}") // Default to Novu's default API base URL
    private String backendUrl;

    @Bean
    public Novu novuClient() {
        logger.info("Initializing Novu client with backend URL: {}", backendUrl);
        NovuConfig novuConfig = new NovuConfig(apiKey);
        novuConfig.setBaseUrl(backendUrl); // Use setBaseUrl as per SDK source
        return new Novu(novuConfig);
    }
}
