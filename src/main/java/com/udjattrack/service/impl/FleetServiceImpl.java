package com.udjattrack.service.impl;

import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.service.FleetService;
import com.udjattrack.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetServiceImpl implements FleetService {

    private final TripService tripService;

    @Override
    public List<TripStateResponse> getFleetStatus(UUID managerId) {
        return tripService.getActiveTripStates(managerId);
    }

    @Override
    public List<TripResponse> getFleetReports(UUID managerId) {
        return tripService.getTripsByFleetManager(managerId);
    }
}
