package com.pmrgsolution.notificationservice.exception;

import com.pmrgsolution.shared.core.exception.BaseException;
import com.pmrgsolution.shared.core.exception.ErrorCode;

public class EmailDeliveryException extends BaseException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message, cause);
    }

    public EmailDeliveryException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }
}
