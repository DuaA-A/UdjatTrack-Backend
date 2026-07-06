package com.udjattrack.scheduler;

import com.udjattrack.entity.enums.TripProgressState;
import com.udjattrack.repository.TripStateRepository;
import com.udjattrack.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripBreakScheduler {

    private final TripStateRepository tripStateRepository;
    private final TripService tripService;
    @Scheduled(fixedRate = 60000)
    public void checkAndResumePausedTrips() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        tripStateRepository.findAll().stream()
                .filter(ts -> ts.getTripProgressState() == TripProgressState.PAUSED)
                .filter(ts -> ts.getLastUpdatedAt() != null && ts.getLastUpdatedAt().isBefore(threshold))
                .forEach(ts -> {
                    try {
                        log.info("Auto-resuming trip {} after 15 minutes break.", ts.getTrip().getTripId());
                        tripService.resumeTrip(ts.getTrip().getTripId(), null);
                    } catch (Exception e) {
                        log.error("Failed to auto-resume trip {}: {}", ts.getTrip().getTripId(), e.getMessage());
                    }
                });
    }
}
