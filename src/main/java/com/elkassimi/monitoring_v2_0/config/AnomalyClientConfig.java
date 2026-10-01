package com.elkassimi.monitoring_v2_0.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AnomalyClientConfig {

    @Bean
    RestClient anomalyRestClient(@Value("${anomaly.base-url}") String baseUrl,
                                 @Value("${anomaly.api-key}") String apiKey){
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeaders(h -> { if (!apiKey.isBlank()) h.setBearerAuth(apiKey); })
                .build();
    }
}
