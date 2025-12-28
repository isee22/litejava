package litejava.plugins.storage;

import io.minio.*;
import io.minio.messages.Item;
import litejava.Plugin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 对象存储插件
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * minio:
 *   endpoint: http://localhost:9000
 *   accessKey: minioadmin
 *   secretKey: minioadmin
 *   bucket: default
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new MinioPlugin());
 * 
 * // 上传文件
 * MinioPlugin.instance.put("file.txt", "Hello MinIO".getBytes());
 * MinioPlugin.instance.put("image.png", inputStream, "image/png");
 * 
 * // 下载文件
 * byte[] data = MinioPlugin.instance.get("file.txt");
 * InputStream stream = MinioPlugin.instance.getStream("file.txt");
 * 
 * // 获取文件 URL
 * String url = MinioPlugin.instance.getUrl("file.txt");
 * 
 * // 删除文件
 * MinioPlugin.instance.delete("file.txt");
 * 
 * // 列出文件
 * List<FileInfo> files = MinioPlugin.instance.list("prefix/");
 * }</pre>
 */
public class MinioPlugin extends Plugin {
    
    public static MinioPlugin instance;
    
    /** MinIO 客户端 */
    public MinioClient client;
    
    /** 服务端点 */
    public String endpoint = "http://localhost:9000";
    
    /** Access Key */
    public String accessKey = "minioadmin";
    
    /** Secret Key */
    public String secretKey = "minioadmin";
    
    /** 默认 Bucket */
    public String bucket = "default";
    
    /** URL 过期时间（秒） */
    public int urlExpiry = 3600;
    
    public static class FileInfo {
        public String name;
        public long size;
        public String etag;
        public boolean isDir;
    }
    
    @Override
    public void config() {
        instance = this;
        
        endpoint = app.conf.getString("minio", "endpoint", endpoint);
        accessKey = app.conf.getString("minio", "accessKey", accessKey);
        secretKey = app.conf.getString("minio", "secretKey", secretKey);
        bucket = app.conf.getString("minio", "bucket", bucket);
        urlExpiry = app.conf.getInt("minio", "urlExpiry", urlExpiry);
        
        client = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
        
        ensureBucket(bucket);
        
        app.log.info("MinioPlugin: Connected to " + endpoint + ", bucket=" + bucket);
    }
    
    /**
     * 确保 Bucket 存在
     */
    public void ensureBucket(String bucketName) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket", e);
        }
    }
    
    /**
     * 上传文件（字节数组）
     */
    public void put(String key, byte[] data) {
        put(key, data, "application/octet-stream");
    }
    
    /**
     * 上传文件（字节数组，指定 Content-Type）
     */
    public void put(String key, byte[] data, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to put object", e);
        }
    }
    
    /**
     * 上传文件（流）
     */
    public void put(String key, InputStream stream, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(stream, -1, 10485760)
                .contentType(contentType)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to put object", e);
        }
    }
    
    /**
     * 下载文件（字节数组）
     */
    public byte[] get(String key) {
        try (InputStream stream = getStream(key)) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object", e);
        }
    }
    
    /**
     * 下载文件（流）
     */
    public InputStream getStream(String key) {
        try {
            return client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object stream", e);
        }
    }
    
    /**
     * 获取预签名 URL
     */
    public String getUrl(String key) {
        return getUrl(key, urlExpiry);
    }
    
    /**
     * 获取预签名 URL（指定过期时间）
     */
    public String getUrl(String key, int expiry) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(key)
                .expiry(expiry)
                .method(io.minio.http.Method.GET)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned URL", e);
        }
    }
    
    /**
     * 删除文件
     */
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object", e);
        }
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean exists(String key) {
        try {
            client.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 列出文件
     */
    public List<FileInfo> list(String prefix) {
        List<FileInfo> result = new ArrayList<>();
        try {
            Iterable<Result<Item>> objects = client.listObjects(ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build());
            
            for (Result<Item> obj : objects) {
                Item item = obj.get();
                FileInfo info = new FileInfo();
                info.name = item.objectName();
                info.size = item.size();
                info.etag = item.etag();
                info.isDir = item.isDir();
                result.add(info);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to list objects", e);
        }
        return result;
    }
    
    /**
     * 复制文件
     */
    public void copy(String sourceKey, String destKey) {
        try {
            client.copyObject(CopyObjectArgs.builder()
                .bucket(bucket)
                .object(destKey)
                .source(CopySource.builder()
                    .bucket(bucket)
                    .object(sourceKey)
                    .build())
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object", e);
        }
    }
    
    @Override
    public void uninstall() {
        instance = null;
    }
}
