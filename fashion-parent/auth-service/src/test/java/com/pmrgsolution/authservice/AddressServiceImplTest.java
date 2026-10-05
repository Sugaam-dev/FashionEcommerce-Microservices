package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.dto.request.AddressRequest;
import com.pmrgsolution.authservice.dto.response.AddressResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.entity.UserAddress;
import com.pmrgsolution.authservice.enums.AddressType;
import com.pmrgsolution.authservice.repository.UserAddressRepository;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private UserAddressRepository userAddressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User sampleUser;
    private UUID userId;
    private UUID addressId;
    private UserAddress sampleAddress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        sampleUser = User.builder().id(userId).email("john@example.com").build();
        sampleAddress = UserAddress.builder()
                .id(addressId)
                .user(sampleUser)
                .addressType(AddressType.SHIPPING)
                .recipientName("John Doe")
                .recipientPhone("9876543210")
                .addressLine1("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .postalCode("560001")
                .country("IN")
                .isDefault(true)
                .build();
    }

    @Test
    @DisplayName("Get Addresses: Returns list of mapped responses")
    void testGetAddresses_Success() {
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(sampleAddress));

        List<AddressResponse> responses = addressService.getAddresses(userId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("John Doe", responses.get(0).recipientName());
        assertTrue(responses.get(0).isDefault());
    }

    @Test
    @DisplayName("Create Address: First address automatically becomes default")
    void testCreateAddress_FirstIsDefault() {
        AddressRequest request = new AddressRequest(
                AddressType.SHIPPING, "John Doe", "9876543210",
                "123 Main St", null, null, "Bangalore", "Karnataka", "560001", "IN", false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.countByUserId(userId)).thenReturn(0L);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> {
            UserAddress addr = i.getArgument(0);
            addr.setId(UUID.randomUUID());
            return addr;
        });

        AddressResponse response = addressService.createAddress(userId, request);

        assertNotNull(response);
        assertTrue(response.isDefault());
    }

    @Test
    @DisplayName("Set Default Address: Clears other defaults and sets this one")
    void testSetDefaultAddress_Success() {
        when(userAddressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(sampleAddress));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> i.getArgument(0));

        AddressResponse response = addressService.setDefaultAddress(userId, addressId);

        assertNotNull(response);
        assertTrue(response.isDefault());
        verify(userAddressRepository).clearAllDefaultForUser(userId);
    }
}
