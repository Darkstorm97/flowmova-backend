package com.flowmova.backend.serviceunitlocation.application;

public record CreateServiceUnitLocationCommand(
        String name,
        String description) {
}
