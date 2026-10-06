package com.pmrgsolution.notificationservice.service;

import java.util.Map;

public interface EmailSenderService {

    void sendEmail(String to, String subject, String templateId, Map<String, Object> variables);
}
