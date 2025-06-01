package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.OfferedServiceDTO;
import com.cedric.Eventra.dto.Response;
import org.springframework.web.multipart.MultipartFile;

public interface OfferedServiceService {

    Response createOfferedService(OfferedServiceDTO offeredServiceDTO);

    Response deleteOfferedService(Long serviceId);

    Response updateOfferedService(Long serviceId, OfferedServiceDTO offeredServiceDTO);

    Response getOfferedServiceByProviderId(Long ServiceProviderId);

    Response getOfferedServiceById(Long serviceId);

    Response getMyOfferedServices();

    Response uploadOfferedServiceImage(Long serviceId, MultipartFile imageFile);

    Response deleteOfferedServiceImage(Long serviceId);
}
