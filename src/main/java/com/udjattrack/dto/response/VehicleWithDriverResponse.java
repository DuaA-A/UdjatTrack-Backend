package com.udjattrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleWithDriverResponse {
    private VehicleResponse vehicle;
    private DriverResponse currentDriver;
}
