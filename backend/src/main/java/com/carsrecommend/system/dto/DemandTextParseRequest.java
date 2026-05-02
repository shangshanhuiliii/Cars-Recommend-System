package com.carsrecommend.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DemandTextParseRequest(
        @Positive Long userId,
        @NotBlank @Size(max = 1000) String text) {
}
