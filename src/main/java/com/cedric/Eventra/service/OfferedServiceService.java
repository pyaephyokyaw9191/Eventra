package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.OfferedServiceDTO;
import com.cedric.Eventra.dto.Response;

public interface OfferedServiceService {

    Response creatOfferedService(OfferedServiceDTO offeredServiceDTO);

    Response deleteOfferedService(Long serviceId);

    Response updateOfferedService(OfferedServiceDTO offeredServiceDTO);

    Response getOfferedServiceByProviderId(Long ServiceProviderId);

    Response getOfferedServiceById(Long serviceId);

    Response getMyOfferedServices();
}
