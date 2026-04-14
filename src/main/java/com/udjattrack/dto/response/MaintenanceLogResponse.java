package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaintenanceLogResponse {
    private String date;
    private String mileage;
    private String details;
    private String status;
}
