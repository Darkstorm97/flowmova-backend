package com.flowmova.backend.item.infrastructure;

import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.domain.ItemStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findByServiceUnitIdAndCatalogId(UUID serviceUnitId, UUID catalogId);

    Optional<Item> findByIdAndServiceUnitId(UUID id, UUID serviceUnitId);

    boolean existsByServiceUnitIdAndCatalogId(UUID serviceUnitId, UUID catalogId);

    List<Item> findByServiceUnitIdOrderByDisplayOrderAscCatalogNameAsc(UUID serviceUnitId);

    List<Item> findByServiceUnitIdAndStatusAndAvailabilityOrderByDisplayOrderAscCatalogNameAsc(
            UUID serviceUnitId,
            ItemStatus status,
            ItemAvailability availability);
}
