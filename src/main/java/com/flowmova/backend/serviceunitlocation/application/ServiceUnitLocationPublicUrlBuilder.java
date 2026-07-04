package com.flowmova.backend.serviceunitlocation.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceUnitLocationPublicUrlBuilder {

    private final String publicBaseUrl;

    public ServiceUnitLocationPublicUrlBuilder(@Value("${flowmova.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String build(String publicAccessSlug) {
        String normalizedBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return "%s/public/locations/%s".formatted(normalizedBaseUrl, publicAccessSlug);
    }
}
