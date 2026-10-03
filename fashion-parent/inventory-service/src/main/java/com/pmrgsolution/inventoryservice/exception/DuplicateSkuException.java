package com.pmrgsolution.inventoryservice.exception;

import com.pmrgsolution.shared.core.exception.ConflictException;

public class DuplicateSkuException extends ConflictException {
    public DuplicateSkuException(String message) {
        super(message);
    }
}
