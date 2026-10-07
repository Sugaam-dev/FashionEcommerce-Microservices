package com.pmrgsolution.notificationservice;

import com.pmrgsolution.notificationservice.dto.request.StoreInfoSettingsUpdateRequest;
import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;
import com.pmrgsolution.notificationservice.entity.StoreInfoSettings;
import com.pmrgsolution.notificationservice.repository.StoreInfoSettingsRepository;
import com.pmrgsolution.notificationservice.service.StoreInfoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreInfoServiceImplTest {

    @Mock
    private StoreInfoSettingsRepository storeInfoSettingsRepository;

    @InjectMocks
    private StoreInfoServiceImpl storeInfoService;

    @Test
    @DisplayName("Should retrieve store info settings or create default")
    void getStoreInfo_ShouldReturnSettings() {
        StoreInfoSettings settings = StoreInfoSettings.builder()
                .id(UUID.randomUUID())
                .announcementText("Flash Sale 50% Off!")
                .isAnnouncementActive(true)
                .supportEmail("support@fashionecommerce.com")
                .updatedAt(Instant.now())
                .build();

        when(storeInfoSettingsRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(settings));

        StoreInfoSettingsResponse response = storeInfoService.getStoreInfo();

        assertThat(response).isNotNull();
        assertThat(response.announcementText()).isEqualTo("Flash Sale 50% Off!");
        assertThat(response.supportEmail()).isEqualTo("support@fashionecommerce.com");
    }

    @Test
    @DisplayName("Should update store info settings successfully")
    void updateStoreInfo_ShouldUpdateAndPersist() {
        StoreInfoSettings settings = StoreInfoSettings.builder()
                .id(UUID.randomUUID())
                .announcementText("Old Announcement")
                .isAnnouncementActive(false)
                .supportEmail("old@fashionecommerce.com")
                .updatedAt(Instant.now())
                .build();

        when(storeInfoSettingsRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(settings));
        when(storeInfoSettingsRepository.save(any(StoreInfoSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        StoreInfoSettingsUpdateRequest updateRequest = new StoreInfoSettingsUpdateRequest(
                "New Announcement 2026",
                "/sale",
                true,
                "[]",
                "New return policy",
                "2 days delivery",
                "New Address",
                "10am-7pm",
                "help@fashionecommerce.com",
                "+919876543210"
        );

        StoreInfoSettingsResponse updated = storeInfoService.updateStoreInfo(updateRequest);

        assertThat(updated.announcementText()).isEqualTo("New Announcement 2026");
        assertThat(updated.supportEmail()).isEqualTo("help@fashionecommerce.com");
        verify(storeInfoSettingsRepository).save(settings);
    }
}
