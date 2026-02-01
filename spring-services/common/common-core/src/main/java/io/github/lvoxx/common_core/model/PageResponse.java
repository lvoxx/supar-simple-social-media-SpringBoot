package io.github.lvoxx.common_core.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard paginated response wrapper.
 * 
 * @param <T> Content type
 * @usage Wrap paginated API responses
 * @reusable Yes - used by all services
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    
    private List<T> content;
    
    private Integer page;
    
    private Integer size;
    
    private Long totalElements;
    
    private Integer totalPages;
    
    @Builder.Default
    private Boolean hasNext = false;
    
    @Builder.Default
    private Boolean hasPrevious = false;
    
    @Builder.Default
    private Boolean isFirst = true;
    
    @Builder.Default
    private Boolean isLast = true;
    
    /**
     * Create page response from content and pagination info
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .isFirst(page == 0)
                .isLast(page >= totalPages - 1)
                .build();
    }
}