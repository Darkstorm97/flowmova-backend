package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.serviceunit.domain.ServiceUnitType;

public record CreateServiceUnitCommand(
        String name,
        String description,
        String location,
        ServiceUnitType type) {
}
