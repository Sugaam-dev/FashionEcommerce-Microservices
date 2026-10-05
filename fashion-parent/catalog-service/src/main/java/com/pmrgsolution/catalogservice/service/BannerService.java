package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.dto.request.StoreBannerRequest;
import com.pmrgsolution.catalogservice.dto.response.StoreBannerResponse;
import com.pmrgsolution.catalogservice.enums.BannerPosition;

import java.util.List;
import java.util.UUID;

public interface BannerService {

    List<StoreBannerResponse> getBannersByPosition(BannerPosition position);

    List<StoreBannerResponse> getAllBanners();

    StoreBannerResponse createBanner(StoreBannerRequest request);

    StoreBannerResponse updateBanner(UUID id, StoreBannerRequest request);

    void deleteBanner(UUID id);

    StoreBannerResponse toggleBannerStatus(UUID id);
}
