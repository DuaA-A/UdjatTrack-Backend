package com.udjattrack.service;

import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;

import java.util.List;
import java.util.UUID;
public interface FleetService {

    List<TripStateResponse> getFleetStatus(UUID managerId);

    List<TripResponse> getFleetReports(UUID managerId);
}
