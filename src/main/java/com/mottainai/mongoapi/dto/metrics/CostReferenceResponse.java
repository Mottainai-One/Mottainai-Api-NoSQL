package com.mottainai.mongoapi.dto.metrics;

public record CostReferenceResponse(
        Double inputPerMillionUsd,
        Double outputPerMillionUsd
) {
}
