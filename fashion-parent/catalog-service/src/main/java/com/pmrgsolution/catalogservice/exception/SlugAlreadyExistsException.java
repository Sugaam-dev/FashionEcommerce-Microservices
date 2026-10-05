package com.pmrgsolution.catalogservice.exception;

import com.pmrgsolution.shared.core.exception.ConflictException;

public class SlugAlreadyExistsException extends ConflictException {

    public SlugAlreadyExistsException(String message) {
        super(message);
    }
}
