package com.pmrgsolution.shared.core.response;

import java.util.List;
import java.util.UUID;

/**
 * Keyset (cursor-based) pagination envelope for large datasets.
 * Prevents full-table scans when navigating deep pages.
 *
 * @param content      page items
 * @param nextCursorId cursor pointing to the last item for next request (?cursor=)
 * @param hasMore      whether there are more items to fetch
 * @param <T>          element type
 */
public record KeysetPageResponse<T>(
        List<T> content,
        UUID nextCursorId,
        boolean hasMore
) {
}
