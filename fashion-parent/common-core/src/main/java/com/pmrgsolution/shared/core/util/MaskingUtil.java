package com.pmrgsolution.shared.core.util;

public final class MaskingUtil {

    private MaskingUtil() {}

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String namePart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        }

        return namePart.charAt(0) + "***" + namePart.charAt(namePart.length() - 1) + domainPart;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int length = phone.length();
        String prefix = phone.startsWith("+") ? phone.substring(0, 3) : phone.substring(0, 2);
        String suffix = phone.substring(length - 2);
        
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < length - prefix.length() - 2; i++) {
            masked.append("*");
        }
        masked.append(suffix);
        return masked.toString();
    }
}
