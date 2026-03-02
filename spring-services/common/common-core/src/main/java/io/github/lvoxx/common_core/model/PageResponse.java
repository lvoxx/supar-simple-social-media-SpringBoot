package io.github.lvoxx.common_core.model;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    String nextCursor,
    boolean hasMore,
    Long total
) {
    public static <T> PageResponse<T> of(List<T> items, String nextCursor, boolean hasMore, Long total) {
        return new PageResponse<>(items, nextCursor, hasMore, total);
    }

    public static <T> PageResponse<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return new PageResponse<>(items, nextCursor, hasMore, null);
    }
}
