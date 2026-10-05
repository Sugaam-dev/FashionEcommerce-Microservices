package com.pmrgsolution.catalogservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
