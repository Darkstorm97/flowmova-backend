package com.flowmova.backend.shared.storage;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowmova.storage")
public record StorageProperties(
        Path localRoot,
        String publicUrlPrefix,
        long maxImageBytes) {
}
