package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.Response;

public interface ReportService {

    /**
     * Retrieves a dashboard summary for the currently authenticated service provider.
     * @return Response object containing the ProviderDashboardSummaryDTO.
     */
    Response getMyProviderDashboardSummary();
}