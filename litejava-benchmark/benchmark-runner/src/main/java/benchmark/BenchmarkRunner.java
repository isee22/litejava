package benchmark;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Benchmark 运行器 - 使用 HttpClient 进行高性能测试
 */
public class BenchmarkRunner {
    
    static Properties conf = new Properties();
    
    static int WARMUP, REQUESTS, CONCURRENCY;
    static String[] FRAMEWORKS = {"Spring Boot", "Javalin", "LiteJava", "LiteJava-VT", "Gin"};
    static int[] PORTS;
    static String[] ENDPOINTS;
    static String[] ENDPOINT_NAMES;
    
    static Map<String, Map<String, long[]>> results = new LinkedHashMap<>();
    static List<Process> processes = new ArrayList<>();
    static HttpClient httpClient;
    
    public static void main(String[] args) throws Exception {
        loadConfig();
        
        WARMUP = getInt("benchmark.warmup", 1000);
        REQUESTS = getInt("benchmark.requests", 10000);
        CONCURRENCY = getInt("benchmark.concurrency", 100);
        PORTS = new int[] {
            getInt("server.springboot.port", 8183),
            getInt("server.javalin.port", 8182),
            getInt("server.litejava.port", 8181),
            getInt("server.litejava-vt.port", 8185),
            getInt("server.gin.port", 8184)
        };
        ENDPOINTS = conf.getProperty("endpoints", "/static/index.html,/dynamic,/text,/json,/users,/posts").split(",");
        ENDPOINT_NAMES = conf.getProperty("endpoint.names", "Static HTML,Dynamic,Text,JSON,DB Query,JOIN Query").split(",");
        
        boolean skipStart = args.length > 0 && "--skip-start".equals(args[0]);
        
        httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newFixedThreadPool(CONCURRENCY))
            .build();
        
        System.out.println("==========================================");
        System.out.println("     LiteJava Benchmark Runner");
        System.out.println("==========================================");
        System.out.println("Requests: " + REQUESTS + ", Concurrency: " + CONCURRENCY);
        System.out.println();
        
        if (!skipStart) startServers();
        waitForServers();
        runBenchmarks();
        printResults();
        generateHtmlReport();
        if (!skipStart) stopServers();
    }
    
    static void loadConfig() {
        String[] paths = { "benchmark.properties", "../benchmark.properties" };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) { conf.load(fis); return; } catch (IOException e) {}
            }
        }
    }
    
    static int getInt(String key, int def) {
        String v = conf.getProperty(key);
        return v != null ? Integer.parseInt(v) : def;
    }
    
    static void startServers() throws Exception {
        String java = System.getProperty("java.home") + "/bin/java";
        String baseDir = new File(".").getCanonicalPath();
        
        String[] modules = { "benchmark-springboot", "benchmark-javalin", "benchmark-litejava", "benchmark-litejava-jdkvt" };
        
        for (String module : modules) {
            String jarPath = baseDir + "/" + module + "/target/" + module + "-1.0.0-SNAPSHOT.jar";
            File jar = new File(jarPath);
            if (!jar.exists()) {
                System.out.println("JAR not found: " + jarPath);
                continue;
            }
            ProcessBuilder pb = new ProcessBuilder(java, "--enable-preview", "-jar", jarPath);
            pb.inheritIO();
            processes.add(pb.start());
            System.out.println("Started: " + module);
        }
    }
    
    static void waitForServers() throws Exception {
        System.out.println("Waiting for servers...");
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);
            int ready = 0;
            for (int port : PORTS) if (checkServer(port)) ready++;
            if (ready >= 4) { System.out.println("Servers ready!"); return; }
        }
        System.out.println("Warning: Not all servers started");
    }
    
    static boolean checkServer(int port) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/text")).timeout(Duration.ofSeconds(2)).GET().build();
            return httpClient.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }
    
    static void runBenchmarks() throws Exception {
        for (int e = 0; e < ENDPOINTS.length; e++) {
            String endpoint = ENDPOINTS[e];
            String name = ENDPOINT_NAMES[e];
            System.out.println("\nTesting: " + name + " (" + endpoint + ")");
            
            Map<String, long[]> endpointResults = new LinkedHashMap<>();
            
            for (int f = 0; f < FRAMEWORKS.length; f++) {
                String framework = FRAMEWORKS[f];
                int port = PORTS[f];
                
                if (!checkServer(port)) {
                    System.out.println("  " + framework + ": not available");
                    endpointResults.put(framework, new long[]{0, 0, 0});
                    continue;
                }
                
                System.out.print("  " + framework + ": warmup... ");
                runTest(port, endpoint, WARMUP);
                
                System.out.print("test... ");
                long[] result = runTest(port, endpoint, REQUESTS);
                endpointResults.put(framework, result);
                
                System.out.println("QPS: " + String.format("%,d", result[0]) + ", Avg: " + String.format("%.2f", result[1] / 100.0) + "ms");
            }
            results.put(name, endpointResults);
        }
    }
    
    static long[] runTest(int port, String endpoint, int requests) throws Exception {
        String url = "http://localhost:" + port + endpoint;
        AtomicInteger success = new AtomicInteger();
        AtomicLong totalTime = new AtomicLong();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        Semaphore semaphore = new Semaphore(CONCURRENCY);
        CountDownLatch latch = new CountDownLatch(requests);
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < requests; i++) {
            semaphore.acquire();
            long t0 = System.nanoTime();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                long elapsed = (System.nanoTime() - t0) / 1_000_000;
                if (err == null && resp.statusCode() == 200) {
                    success.incrementAndGet();
                    totalTime.addAndGet(elapsed);
                    latencies.add(elapsed);
                }
                semaphore.release();
                latch.countDown();
            });
        }
        latch.await();
        
        long elapsed = System.currentTimeMillis() - start;
        int qps = elapsed > 0 ? (int) (success.get() * 1000L / elapsed) : 0;
        int avg = success.get() > 0 ? (int) (totalTime.get() * 100 / success.get()) : 0;
        Collections.sort(latencies);
        long p99 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.99));
        
        return new long[]{qps, avg, p99};
    }
    
    static void printResults() {
        System.out.println("\n==========================================");
        System.out.println("           BENCHMARK RESULTS");
        System.out.println("==========================================\n");
        System.out.println("## QPS (Higher is Better)\n");
        printTable(0);
        System.out.println("\n## Avg Latency ms (Lower is Better)\n");
        printTable(1);
        System.out.println("\n## P99 Latency ms (Lower is Better)\n");
        printTable(2);
    }
    
    static void printTable(int idx) {
        System.out.printf("| %-12s |", "Endpoint");
        for (String f : FRAMEWORKS) System.out.printf(" %11s |", f);
        System.out.println(" Winner |");
        System.out.print("|--------------|");
        for (int i = 0; i < FRAMEWORKS.length; i++) System.out.print("-------------|");
        System.out.println("--------|");
        
        for (String endpoint : results.keySet()) {
            Map<String, long[]> data = results.get(endpoint);
            System.out.printf("| %-12s |", endpoint);
            String winner = ""; long best = idx == 0 ? 0 : Long.MAX_VALUE;
            for (String f : FRAMEWORKS) {
                long[] r = data.get(f);
                long val = r != null ? r[idx] : 0;
                if (idx == 1) System.out.printf(" %11.2f |", val / 100.0);
                else System.out.printf(" %,11d |", val);
                if (val > 0) {
                    if (idx == 0 && val > best) { best = val; winner = f; }
                    if (idx > 0 && val < best) { best = val; winner = f; }
                }
            }
            System.out.println(" " + winner + " |");
        }
    }
    
    static void generateHtmlReport() {
        System.out.println("\n==========================================");
        System.out.println("HTML report: benchmark-results.html");
    }
    
    static void stopServers() {
        System.out.println("\nStopping servers...");
        for (Process p : processes) p.destroyForcibly();
    }
}
