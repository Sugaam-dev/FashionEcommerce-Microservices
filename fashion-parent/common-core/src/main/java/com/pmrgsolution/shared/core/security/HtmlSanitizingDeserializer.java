package com.pmrgsolution.shared.core.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

/**
 * Enterprise HTML-escaping deserializer to defend against XSS injection in user-supplied text fields.
 *
 * Applied explicitly to display-facing string fields (e.g. user names, comments, reviews, notes)
 * using @JsonDeserialize(using = HtmlSanitizingDeserializer.class), ensuring passwords and cryptographic tokens
 * containing '&' or '<' are never mutated.
 */
public class HtmlSanitizingDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(value.trim());
    }
}