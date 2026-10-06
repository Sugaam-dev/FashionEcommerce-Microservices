package com.pmrgsolution.notificationservice.service;

import com.pmrgsolution.notificationservice.dto.request.StoreInfoSettingsUpdateRequest;
import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;

public interface StoreInfoService {

    StoreInfoSettingsResponse getStoreInfo();

    StoreInfoSettingsResponse updateStoreInfo(StoreInfoSettingsUpdateRequest request);
}
