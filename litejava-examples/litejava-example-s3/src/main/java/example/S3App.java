package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.storage.S3Plugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * S3/MinIO 文件存储示例
 * 
 * <h2>前置条件</h2>
 * 启动 MinIO: docker run -d -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"
 * 创建 bucket: mc mb local/demo-bucket
 * 
 * <h2>测试</h2>
 * <pre>
 * # 上传文件
 * curl -X POST http://localhost:8080/upload -F "file=@test.txt"
 * 
 * # 列出文件
 * curl http://localhost:8080/files
 * 
 * # 下载文件
 * curl http://localhost:8080/files/{key}
 * </pre>
 */
public class S3App {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        S3Plugin s3 = new S3Plugin();
        s3.endpoint = "http://localhost:9000";
        s3.accessKey = "minioadmin";
        s3.secretKey = "minioadmin";
        s3.bucket = "demo-bucket";
        app.use(s3);
        
        // 上传文件
        app.post("/upload", ctx -> {
            byte[] data = ctx.getRawData();
            String key = UUID.randomUUID().toString();
            String contentType = ctx.header("Content-Type");
            s3.upload(key, data, contentType != null ? contentType : "application/octet-stream");
            ctx.json(Map.of("key", key, "size", data.length));
        });
        
        // 列出文件
        app.get("/files", ctx -> {
            String prefix = ctx.queryParam("prefix");
            List<String> files = s3.list(prefix != null ? prefix : "");
            ctx.json(Map.of("files", files));
        });
        
        // 下载文件
        app.get("/files/:key", ctx -> {
            String key = ctx.pathParam("key");
            if (!s3.exists(key)) {
                ctx.status(404).json(Map.of("error", "File not found"));
                return;
            }
            byte[] data = s3.download(key);
            ctx.header("Content-Type", "application/octet-stream");
            ctx.data(data);
        });
        
        // 获取预签名 URL
        app.get("/presign/:key", ctx -> {
            String key = ctx.pathParam("key");
            String url = s3.getPresignedUrl(key, Duration.ofHours(1));
            ctx.json(Map.of("url", url));
        });
        
        // 删除文件
        app.delete("/files/:key", ctx -> {
            String key = ctx.pathParam("key");
            s3.delete(key);
            ctx.json(Map.of("deleted", true));
        });
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "S3/MinIO Example",
            "endpoints", Map.of(
                "POST /upload", "Upload file",
                "GET /files", "List files",
                "GET /files/:key", "Download file",
                "GET /presign/:key", "Get presigned URL",
                "DELETE /files/:key", "Delete file"
            )
        )));
        
        app.run();
    }
}
