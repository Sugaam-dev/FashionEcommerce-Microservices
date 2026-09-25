package com.pmrgsolution.orderservice.dto.response;

import java.util.List;
import java.util.UUID;

public record KeysetPageResponse<T>(
        List<T> content,
        UUID nextCursorId,
        boolean hasMore
) {}
