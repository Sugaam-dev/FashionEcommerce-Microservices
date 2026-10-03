package com.pmrgsolution.inventoryservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class InventoryItemNotFoundException extends ResourceNotFoundException {
    public InventoryItemNotFoundException(String message) {
        super(message);
    }
}
