package com.pmrgsolution.inventoryservice.exception;

import com.pmrgsolution.shared.core.exception.ConflictException;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
