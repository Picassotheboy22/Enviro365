package com.enviro.assessment.junior.smsibi.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** When a notice was paid and for how much: all the dashboard chart needs (an interface projection). */
public interface PaidAmount {

    LocalDateTime getPaidAt();

    BigDecimal getAmount();
}
