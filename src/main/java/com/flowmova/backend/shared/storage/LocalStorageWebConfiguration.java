package com.flowmova.backend.shared.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class LocalStorageWebConfiguration implements WebMvcConfigurer {

    private final StorageProperties properties;

    LocalStorageWebConfiguration(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(properties.localRoot().toAbsolutePath().normalize().toUri().toString());
    }
}
