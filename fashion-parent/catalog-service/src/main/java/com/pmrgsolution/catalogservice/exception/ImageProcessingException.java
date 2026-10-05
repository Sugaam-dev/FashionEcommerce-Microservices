package com.pmrgsolution.catalogservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class ImageProcessingException extends BadRequestException {

    public ImageProcessingException(String message) {
        super(message);
    }
}
