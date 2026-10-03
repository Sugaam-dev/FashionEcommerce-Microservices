package com.pmrgsolution.inventoryservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class ReservationNotFoundException extends ResourceNotFoundException {
    public ReservationNotFoundException(String message) {
        super(message);
    }
}
