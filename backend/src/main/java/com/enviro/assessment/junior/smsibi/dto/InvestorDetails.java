package com.enviro.assessment.junior.smsibi.dto;

import java.time.LocalDate;

/**
 * Full investor details shown on the portfolio dashboard. {@code age} is calculated on request.
 */
public record InvestorDetails(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        int age) {}
