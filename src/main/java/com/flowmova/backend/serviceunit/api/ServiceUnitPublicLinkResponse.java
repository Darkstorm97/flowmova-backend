package com.flowmova.backend.serviceunit.api;

import java.util.UUID;

public record ServiceUnitPublicLinkResponse(
        UUID serviceUnitId,
        UUID locationId,
        String publicAccessSlug,
        String publicUrl) {
}
