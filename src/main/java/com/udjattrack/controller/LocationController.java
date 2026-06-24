package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
@Tag(name = "Locations & Maps", description = "Endpoints for location-based services and map integration")
public class LocationController {

    @GetMapping("/nearest-help")
    @Operation(summary = "Get nearest help locations (hospitals, police, mechanics) to a coordinate")
    public ResponseEntity<ApiResponse<List<NearestHelpResponse>>> getNearestHelp(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "20") double radius) {
        
        // MOCK DATA for now, since external Maps API is not yet integrated.
        // In a real implementation, this would query Google Places or OpenStreetMap.
        List<NearestHelpResponse> mockResponse = List.of(
                NearestHelpResponse.builder()
                        .name("Aswan General Hospital")
                        .type("HOSPITAL")
                        .distanceKm(2.5)
                        .build(),
                NearestHelpResponse.builder()
                        .name("Highway Patrol Station")
                        .type("POLICE")
                        .distanceKm(5.0)
                        .build(),
                NearestHelpResponse.builder()
                        .name("Speedy Truck Repairs")
                        .type("MECHANIC")
                        .distanceKm(8.2)
                        .build()
        );

        return ResponseEntity.ok(ApiResponse.ok("Nearest help locations retrieved", mockResponse));
    }

    @Getter
    @Builder
    public static class NearestHelpResponse {
        private String name;
        private String type;
        private Double distanceKm;
    }
}
