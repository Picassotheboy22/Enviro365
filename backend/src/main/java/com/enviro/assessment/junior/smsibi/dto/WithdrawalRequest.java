package com.enviro.assessment.junior.smsibi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request body for submitting a withdrawal notice.
 *
 * <p>These annotations check the <em>shape</em> of the input (present, positive, at most 2 decimals) and
 * are applied automatically by {@code @Valid} in the controller. The <em>business</em> rules (age, balance,
 * 90% limit) need data from the database, so they live in WithdrawalPolicy instead.
 */
public record WithdrawalRequest(
        @Schema(description = "ID of the product to withdraw from", example = "1")
        @NotNull(message = "Product is required")
        Long productId,

        @Schema(description = "Amount to withdraw in Rand", example = "5000.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least R 0.01")
        @Digits(integer = 15, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount) {}
