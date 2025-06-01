package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports") // Base path for all reports
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Endpoint for an authenticated service provider to retrieve their dashboard summary.
     *
     * @return ResponseEntity containing the standard Response object with their dashboard summary.
     */
    @GetMapping("/provider/dashboard-summary")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> getMyProviderDashboardSummary() {
        Response serviceResponse = reportService.getMyProviderDashboardSummary();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // You can add more report endpoints here later
    // e.g., @GetMapping("/provider/bookings-detailed") for a more detailed booking report
}