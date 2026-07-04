package com.flowmova.backend.serviceunitlocation.application;

import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ServiceUnitLocationSlugGenerator {

    private static final char[] SLUG_ALPHABET = "23456789abcdefghjkmnpqrstuvwxyz".toCharArray();
    private static final int SLUG_RANDOM_LENGTH = 12;

    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ServiceUnitLocationSlugGenerator(ServiceUnitLocationRepository serviceUnitLocationRepository) {
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
    }

    public String generate() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String slug = "loc-" + randomSlugPart();
            if (serviceUnitLocationRepository.findByPublicAccessSlug(slug).isEmpty()) {
                return slug;
            }
        }

        return "loc-" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String randomSlugPart() {
        StringBuilder slug = new StringBuilder(SLUG_RANDOM_LENGTH);
        for (int index = 0; index < SLUG_RANDOM_LENGTH; index++) {
            slug.append(SLUG_ALPHABET[secureRandom.nextInt(SLUG_ALPHABET.length)]);
        }
        return slug.toString();
    }
}
