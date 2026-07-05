package com.flowmova.backend.company.application;

public record SearchCompaniesQuery(
        String text,
        String businessType,
        String city,
        String region,
        String country) {
}
