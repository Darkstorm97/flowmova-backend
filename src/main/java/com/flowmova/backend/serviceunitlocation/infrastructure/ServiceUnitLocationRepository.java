package com.flowmova.backend.serviceunitlocation.infrastructure;

import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceUnitLocationRepository extends JpaRepository<ServiceUnitLocation, UUID> {

    List<ServiceUnitLocation> findByServiceUnitIdOrderByDefaultLocationDescNameAsc(UUID serviceUnitId);

    Optional<ServiceUnitLocation> findByServiceUnitIdAndDefaultLocationTrueAndStatus(
            UUID serviceUnitId,
            ServiceUnitLocationStatus status);

    Optional<ServiceUnitLocation> findByPublicAccessSlug(String publicAccessSlug);

    Optional<ServiceUnitLocation> findByPublicAccessSlugAndStatus(
            String publicAccessSlug,
            ServiceUnitLocationStatus status);
}
