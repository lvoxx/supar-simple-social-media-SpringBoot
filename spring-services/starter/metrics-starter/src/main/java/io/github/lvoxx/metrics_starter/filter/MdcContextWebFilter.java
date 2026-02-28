package io.github.lvoxx.metrics_starter.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.github.f4b6a3.ulid.UlidCreator;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * WebFilter that enriches every request with structured logging and tracing context.
 *
 * <p>For every incoming request, this filter:</p>
 * <ol>
 *   <li>Generates a unique {@code requestId} (ULID) for this request</li>
 *   <li>Extracts {@code X-User-Id} from the request header (injected by K8S gateway)</li>
 *   <li>Extracts Zipkin {@code traceId} and {@code spanId} from the Micrometer observation context</li>
 *   <li>Writes all values into SLF4J {@link MDC} for structured log output</li>
 *   <li>Writes all values into the Reactor {@link Context} for downstream propagation</li>
 * </ol>
 *
 * <p>After request completion, MDC keys are removed to prevent thread-local leaks.</p>
 *
 * <h3>Resulting log entry fields:</h3>
 * <pre>{@code
 * {
 *   "traceId":   "...",  <- from Zipkin/OTel
 *   "spanId":    "...",  <- from Zipkin/OTel
 *   "userId":    "...",  <- X-User-Id header
 *   "requestId": "..."   <- ULID generated per request
 * }
 * }</pre>
 */
public class MdcContextWebFilter implements WebFilter, Ordered {

    public static final String MDC_TRACE_ID   = "traceId";
    public static final String MDC_SPAN_ID    = "spanId";
    public static final String MDC_USER_ID    = "userId";
    public static final String MDC_REQUEST_ID = "requestId";

    private static final String HEADER_USER_ID  = "X-User-Id";
    private static final String HEADER_TRACE_ID = "X-B3-TraceId";
    private static final String HEADER_SPAN_ID  = "X-B3-SpanId";

    /**
     * Run after security filters but before business filters.
     * Negative order puts this early in the chain.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var headers   = exchange.getRequest().getHeaders();

        String requestId = UlidCreator.getMonotonicUlid().toString();
        String userId    = headers.getFirst(HEADER_USER_ID);
        String traceId   = headers.getFirst(HEADER_TRACE_ID);
        String spanId    = headers.getFirst(HEADER_SPAN_ID);

        return chain.filter(exchange)
                .contextWrite(ctx -> {
                    Context enriched = ctx
                            .put(MDC_REQUEST_ID, requestId);
                    if (userId   != null) enriched = enriched.put(MDC_USER_ID, userId);
                    if (traceId  != null) enriched = enriched.put(MDC_TRACE_ID, traceId);
                    if (spanId   != null) enriched = enriched.put(MDC_SPAN_ID, spanId);
                    return enriched;
                })
                .doFirst(() -> {
                    MDC.put(MDC_REQUEST_ID, requestId);
                    if (userId  != null) MDC.put(MDC_USER_ID, userId);
                    if (traceId != null) MDC.put(MDC_TRACE_ID, traceId);
                    if (spanId  != null) MDC.put(MDC_SPAN_ID, spanId);
                })
                .doFinally(signal -> {
                    MDC.remove(MDC_REQUEST_ID);
                    MDC.remove(MDC_USER_ID);
                    MDC.remove(MDC_TRACE_ID);
                    MDC.remove(MDC_SPAN_ID);
                });
    }
}
