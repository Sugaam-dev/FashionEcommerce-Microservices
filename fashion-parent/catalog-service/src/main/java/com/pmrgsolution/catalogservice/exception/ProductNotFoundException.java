package com.pmrgsolution.catalogservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
