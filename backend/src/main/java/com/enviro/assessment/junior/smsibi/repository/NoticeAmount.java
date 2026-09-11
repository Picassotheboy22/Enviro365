package com.enviro.assessment.junior.smsibi.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** When a notice was submitted and for how much: all the dashboard chart needs (an interface projection). */
public interface NoticeAmount {

    LocalDateTime getCreatedAt();

    BigDecimal getAmount();
}
