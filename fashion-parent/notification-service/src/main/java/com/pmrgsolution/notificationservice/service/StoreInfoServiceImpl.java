package com.pmrgsolution.notificationservice.service;

import com.pmrgsolution.notificationservice.dto.request.StoreInfoSettingsUpdateRequest;
import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;
import com.pmrgsolution.notificationservice.entity.StoreInfoSettings;
import com.pmrgsolution.notificationservice.repository.StoreInfoSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreInfoServiceImpl implements StoreInfoService {

    private static final Logger log = LoggerFactory.getLogger(StoreInfoServiceImpl.class);

    private final StoreInfoSettingsRepository storeInfoSettingsRepository;

    public StoreInfoServiceImpl(StoreInfoSettingsRepository storeInfoSettingsRepository) {
        this.storeInfoSettingsRepository = storeInfoSettingsRepository;
    }

    @Override
    @Transactional
    public StoreInfoSettingsResponse getStoreInfo() {
        StoreInfoSettings settings = storeInfoSettingsRepository.findFirstByOrderByUpdatedAtDesc()
                .orElseGet(this::createDefaultSettings);
        return StoreInfoSettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional
    public StoreInfoSettingsResponse updateStoreInfo(StoreInfoSettingsUpdateRequest request) {
        StoreInfoSettings settings = storeInfoSettingsRepository.findFirstByOrderByUpdatedAtDesc()
                .orElseGet(this::createDefaultSettings);

        if (request.announcementText() != null) {
            settings.setAnnouncementText(request.announcementText());
        }
        if (request.announcementLink() != null) {
            settings.setAnnouncementLink(request.announcementLink());
        }
        if (request.isAnnouncementActive() != null) {
            settings.setAnnouncementActive(request.isAnnouncementActive());
        }
        if (request.announcementsJson() != null) {
            settings.setAnnouncementsJson(request.announcementsJson());
        }
        if (request.returnPolicyText() != null) {
            settings.setReturnPolicyText(request.returnPolicyText());
        }
        if (request.deliveryPolicyNotice() != null) {
            settings.setDeliveryPolicyNotice(request.deliveryPolicyNotice());
        }
        if (request.contactAddress() != null) {
            settings.setContactAddress(request.contactAddress());
        }
        if (request.operatingHours() != null) {
            settings.setOperatingHours(request.operatingHours());
        }
        if (request.supportEmail() != null) {
            settings.setSupportEmail(request.supportEmail());
        }
        if (request.supportPhone() != null) {
            settings.setSupportPhone(request.supportPhone());
        }

        StoreInfoSettings saved = storeInfoSettingsRepository.save(settings);
        log.info("Store info settings updated");
        return StoreInfoSettingsResponse.fromEntity(saved);
    }

    private StoreInfoSettings createDefaultSettings() {
        StoreInfoSettings defaultSettings = StoreInfoSettings.builder()
                .announcementText("Welcome to Fashion E-Commerce! Free shipping on orders over ₹999.")
                .announcementLink("/products")
                .isAnnouncementActive(true)
                .announcementsJson("[]")
                .returnPolicyText("Hassle-free 7-day returns on eligible items.")
                .deliveryPolicyNotice("Standard delivery within 3-5 business days.")
                .contactAddress("123 Fashion Street, Cyber City, Hyderabad, India")
                .operatingHours("Mon - Sat: 9:00 AM - 8:00 PM IST")
                .supportEmail("support@fashionecommerce.com")
                .supportPhone("+91 9876543210")
                .build();
        return storeInfoSettingsRepository.save(defaultSettings);
    }
}
