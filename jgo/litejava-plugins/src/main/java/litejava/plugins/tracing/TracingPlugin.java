package litejava.plugins.tracing;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 链路追踪插件
 * 
 * <p>提供分布式链路追踪能力，兼容 OpenTelemetry/Jaeger/Zipkin 格式。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * TracingPlugin tracing = new TracingPlugin()
 *     .serviceName("user-service")
 *     .consoleExporter();
 * 
 * app.use(tracing);
 * }</pre>
 */
public class TracingPlugin extends MiddlewarePlugin {
    
    /** 服务名称 */
    public String serviceName = "unknown";
    
    /** Span 导出器 */
    public Consumer<Span> exporter;
    
    /** 采样率 (0.0 - 1.0) */
    public double sampleRate = 1.0;
    
    private static final String SPAN_KEY = "_tracing_span";
    
    public static class Span implements AutoCloseable {
        public String traceId;
        public String spanId;
        public String parentSpanId;
        public String operationName;
        public long startTime;
        public long endTime;
        public Map<String, String> tags = new ConcurrentHashMap<>();
        public Map<String, Long> logs = new ConcurrentHashMap<>();
        public String status = "OK";
        
        private TracingPlugin plugin;
        private boolean finished = false;
        
        public Span(TracingPlugin plugin, String traceId, String parentSpanId, String operationName) {
            this.plugin = plugin;
            this.traceId = traceId;
            this.spanId = generateSpanId();
            this.parentSpanId = parentSpanId;
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
        }
        
        public Span tag(String key, String value) {
            tags.put(key, value);
            return this;
        }
        
        public Span log(String event) {
            logs.put(event, System.currentTimeMillis());
            return this;
        }
        
        public Span error(Throwable e) {
            this.status = "ERROR";
            tag("error", "true");
            tag("error.message", e.getMessage());
            tag("error.type", e.getClass().getName());
            return this;
        }
        
        public Span child(String operationName) {
            return new Span(plugin, traceId, spanId, operationName);
        }
        
        public void finish() {
            if (finished) return;
            finished = true;
            endTime = System.currentTimeMillis();
            
            if (plugin.exporter != null) {
                plugin.exporter.accept(this);
            }
        }
        
        @Override
        public void close() {
            finish();
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        @Override
        public String toString() {
            return String.format("Span{traceId=%s, spanId=%s, op=%s, duration=%dms, status=%s}",
                    traceId, spanId, operationName, getDuration(), status);
        }
        
        private static String generateSpanId() {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
    
    public TracingPlugin serviceName(String name) {
        this.serviceName = name;
        return this;
    }
    
    public TracingPlugin exporter(Consumer<Span> exporter) {
        this.exporter = exporter;
        return this;
    }
    
    public TracingPlugin sampleRate(double rate) {
        this.sampleRate = Math.max(0, Math.min(1, rate));
        return this;
    }
    
    public TracingPlugin consoleExporter() {
        return exporter(span -> System.out.println("[TRACE] " + span));
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (Math.random() > sampleRate) {
            next.run();
            return;
        }
        
        String traceId = extractTraceId(ctx);
        String parentSpanId = extractParentSpanId(ctx);
        
        Span span = new Span(this, traceId, parentSpanId, ctx.method + " " + ctx.path);
        span.tag("http.method", ctx.method);
        span.tag("http.url", ctx.path);
        span.tag("service.name", serviceName);
        
        ctx.state.put(SPAN_KEY, span);
        ctx.header("X-Trace-Id", traceId);
        
        try {
            next.run();
            span.tag("http.status_code", String.valueOf(ctx.getResponseStatus()));
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.finish();
        }
    }
    
    public Span currentSpan(Context ctx) {
        return (Span) ctx.state.get(SPAN_KEY);
    }
    
    public Span newSpan(String operationName) {
        return new Span(this, generateTraceId(), null, operationName);
    }
    
    private String extractTraceId(Context ctx) {
        String traceparent = ctx.header("traceparent");
        if (traceparent != null && traceparent.length() >= 55) {
            return traceparent.substring(3, 35);
        }
        
        String b3TraceId = ctx.header("X-B3-TraceId");
        if (b3TraceId != null) {
            return b3TraceId;
        }
        
        String uberTraceId = ctx.header("uber-trace-id");
        if (uberTraceId != null) {
            int idx = uberTraceId.indexOf(':');
            if (idx > 0) {
                return uberTraceId.substring(0, idx);
            }
        }
        
        return generateTraceId();
    }
    
    private String extractParentSpanId(Context ctx) {
        String traceparent = ctx.header("traceparent");
        if (traceparent != null && traceparent.length() >= 55) {
            return traceparent.substring(36, 52);
        }
        
        String b3SpanId = ctx.header("X-B3-SpanId");
        if (b3SpanId != null) {
            return b3SpanId;
        }
        
        return null;
    }
    
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
