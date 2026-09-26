package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.dto.request.AddToCartRequest;
import com.pmrgsolution.orderservice.dto.response.CartItemResponse;
import com.pmrgsolution.orderservice.entity.CartItem;
import com.pmrgsolution.orderservice.exception.CartItemNotFoundException;
import com.pmrgsolution.orderservice.mapper.OrderMapper;
import com.pmrgsolution.orderservice.repository.CartItemRepository;
import com.pmrgsolution.orderservice.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID userId;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sku("TSHIRT-RED-L")
                .productId(UUID.randomUUID().toString())
                .quantity(2)
                .addedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getCart - returns mapped list of cart items")
    void getCart_returnsMappedResponses() {
        when(cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(cartItem));

        List<CartItemResponse> result = cartService.getCart(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("TSHIRT-RED-L");
        assertThat(result.get(0).quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("addItem - new SKU creates new cart item")
    void addItem_newItem_createsCartItem() {
        AddToCartRequest req = new AddToCartRequest("TSHIRT-BLUE-M", "prod-123", 3);
        when(cartItemRepository.findByUserIdAndSku(userId, "TSHIRT-BLUE-M")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem ci = inv.getArgument(0);
            ci.setId(UUID.randomUUID());
            return ci;
        });

        CartItemResponse response = cartService.addItem(userId, req);

        assertThat(response.sku()).isEqualTo("TSHIRT-BLUE-M");
        assertThat(response.quantity()).isEqualTo(3);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItem - existing SKU increments quantity")
    void addItem_existingItem_updatesQuantity() {
        AddToCartRequest req = new AddToCartRequest("TSHIRT-RED-L", cartItem.getProductId(), 3);
        when(cartItemRepository.findByUserIdAndSku(userId, "TSHIRT-RED-L")).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        CartItemResponse response = cartService.addItem(userId, req);

        assertThat(cartItem.getQuantity()).isEqualTo(5); // 2 + 3
        assertThat(response.quantity()).isEqualTo(5);
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    @DisplayName("updateQuantity - updates quantity and timestamp")
    void updateQuantity_updatesQuantityAndTimestamp() {
        when(cartItemRepository.findById(cartItem.getId())).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        CartItemResponse response = cartService.updateQuantity(userId, cartItem.getId(), 4);

        assertThat(cartItem.getQuantity()).isEqualTo(4);
        assertThat(response.quantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("updateQuantity - non-existent or other user's item throws CartItemNotFoundException")
    void updateQuantity_notFound_throwsException() {
        when(cartItemRepository.findById(cartItem.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(userId, cartItem.getId(), 4))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    @DisplayName("removeItem - removes item from repository")
    void removeItem_deletesItem() {
        when(cartItemRepository.findById(cartItem.getId())).thenReturn(Optional.of(cartItem));

        cartService.removeItem(userId, cartItem.getId());

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("clearCart - deletes all items for user")
    void clearCart_removesAllItems() {
        cartService.clearCart(userId);

        verify(cartItemRepository).deleteByUserId(userId);
    }
}
