package litejava.plugins.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.*;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

/**
 * Prometheus 指标插件 - 基于 Micrometer
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.micrometer</groupId>
 *     <artifactId>micrometer-registry-prometheus</artifactId>
 *     <version>1.12.2</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * MetricsPlugin metrics = new MetricsPlugin();
 * app.use(metrics);
 * 
 * // 自定义指标
 * metrics.counter("orders_total", "type", "online").increment();
 * metrics.timer("api_latency").record(() -> doSomething());
 * 
 * // 访问 /metrics 获取 Prometheus 格式数据
 * }</pre>
 * 
 * @see <a href="https://micrometer.io/docs">Micrometer Documentation</a>
 */
public class MetricsPlugin extends MiddlewarePlugin {
    
    public static MetricsPlugin instance;
    public String path = "/metrics";
    public boolean collectJvm = true;
    public PrometheusMeterRegistry registry;
    
    private Timer.Builder requestTimer;
    
    public MetricsPlugin() {
        instance = this;
    }
    
    @Override
    public void config() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        if (collectJvm) {
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);
        }
        
        requestTimer = Timer.builder("http_server_requests_seconds")
            .description("HTTP request latency");
        
        app.get(path, ctx -> {
            ctx.header("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            ctx.text(registry.scrape());
        });
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (ctx.path.equals(path)) {
            next.run();
            return;
        }
        
        Timer.Sample sample = Timer.start(registry);
        String method = ctx.method;
        String uri = normalizePath(ctx.path);
        
        try {
            next.run();
        } finally {
            String status = String.valueOf(ctx.getResponseStatus());
            sample.stop(requestTimer
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", status)
                .register(registry));
        }
    }
    
    @Override
    public void uninstall() {
        if (registry != null) registry.close();
    }
    
    public Counter counter(String name, String... tags) {
        return registry.counter(name, tags);
    }
    
    public <T> T gauge(String name, T obj, java.util.function.ToDoubleFunction<T> f) {
        return registry.gauge(name, obj, f);
    }
    
    public Timer timer(String name, String... tags) {
        return registry.timer(name, tags);
    }
    
    public DistributionSummary summary(String name, String... tags) {
        return registry.summary(name, tags);
    }
    
    public MeterRegistry getRegistry() {
        return registry;
    }
    
    private String normalizePath(String path) {
        return path.replaceAll("/\\d+", "/{id}").replaceAll("/[a-f0-9-]{36}", "/{uuid}");
    }
}
