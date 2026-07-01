package com.udjattrack.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record AlertDailyCountResponse(
        LocalDate date,
        long count
) {}
