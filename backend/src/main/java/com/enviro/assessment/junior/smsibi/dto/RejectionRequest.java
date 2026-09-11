package com.enviro.assessment.junior.smsibi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for rejecting a withdrawal notice. The investor is shown the reason, so it is required. */
public record RejectionRequest(
        @Schema(
                description = "Why the notice is rejected. The investor sees this.",
                example = "We could not verify your bank account details. Please contact us before submitting again.")
        @NotBlank(message = "A reason is required")
        @Size(max = 500, message = "The reason can be at most 500 characters")
        String reason) {}
