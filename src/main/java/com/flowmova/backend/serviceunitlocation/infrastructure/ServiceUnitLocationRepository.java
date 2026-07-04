package com.flowmova.backend.serviceunitlocation.infrastructure;

import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceUnitLocationRepository extends JpaRepository<ServiceUnitLocation, UUID> {

    List<ServiceUnitLocation> findByServiceUnitIdOrderByDefaultLocationDescNameAsc(UUID serviceUnitId);

    Page<ServiceUnitLocation> findByServiceUnitIdOrderByDefaultLocationDescNameAsc(
            UUID serviceUnitId,
            Pageable pageable);

    List<ServiceUnitLocation> findByServiceUnitIdAndStatusOrderByDefaultLocationDescNameAsc(
            UUID serviceUnitId,
            ServiceUnitLocationStatus status);

    Optional<ServiceUnitLocation> findByServiceUnitIdAndDefaultLocationTrueAndStatus(
            UUID serviceUnitId,
            ServiceUnitLocationStatus status);

    Optional<ServiceUnitLocation> findByIdAndServiceUnitIdAndStatus(
            UUID id,
            UUID serviceUnitId,
            ServiceUnitLocationStatus status);

    Optional<ServiceUnitLocation> findByPublicAccessSlug(String publicAccessSlug);

    Optional<ServiceUnitLocation> findByPublicAccessSlugAndStatus(
            String publicAccessSlug,
            ServiceUnitLocationStatus status);
}
