package com.pmrgsolution.catalogservice.service.impl;

import com.pmrgsolution.catalogservice.dto.request.StoreBannerRequest;
import com.pmrgsolution.catalogservice.dto.response.StoreBannerResponse;
import com.pmrgsolution.catalogservice.entity.StoreBanner;
import com.pmrgsolution.catalogservice.enums.BannerPosition;
import com.pmrgsolution.catalogservice.mapper.StoreBannerMapper;
import com.pmrgsolution.catalogservice.repository.StoreBannerRepository;
import com.pmrgsolution.catalogservice.service.BannerService;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final StoreBannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoreBannerResponse> getBannersByPosition(BannerPosition position) {
        List<StoreBanner> banners = bannerRepository.findByPositionAndIsActiveTrueOrderByDisplayOrderAsc(position);
        return banners.stream()
                .map(StoreBannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreBannerResponse> getAllBanners() {
        List<StoreBanner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        return banners.stream()
                .map(StoreBannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreBannerResponse createBanner(StoreBannerRequest request) {
        StoreBanner banner = StoreBannerMapper.toEntity(request);
        StoreBanner saved = bannerRepository.save(banner);
        log.info("Created store banner id={}, position={}", saved.getId(), saved.getPosition());
        return StoreBannerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StoreBannerResponse updateBanner(UUID id, StoreBannerRequest request) {
        StoreBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        StoreBannerMapper.updateEntity(banner, request);
        StoreBanner updated = bannerRepository.save(banner);
        log.info("Updated store banner id={}, position={}", updated.getId(), updated.getPosition());
        return StoreBannerMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBanner(UUID id) {
        StoreBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        bannerRepository.delete(banner);
        log.info("Deleted store banner id={}", id);
    }

    @Override
    @Transactional
    public StoreBannerResponse toggleBannerStatus(UUID id) {
        StoreBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        banner.setActive(!banner.isActive());
        StoreBanner updated = bannerRepository.save(banner);
        log.info("Toggled store banner id={} active status to {}", id, updated.isActive());
        return StoreBannerMapper.toResponse(updated);
    }
}
