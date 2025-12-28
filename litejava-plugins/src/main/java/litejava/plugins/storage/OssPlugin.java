package litejava.plugins.storage;

import litejava.Plugin;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云 OSS 文件存储插件
 * 
 * <h2>配置</h2>
 * <pre>
 * oss:
 *   endpoint: oss-cn-hangzhou.aliyuncs.com
 *   accessKeyId: your-access-key
 *   accessKeySecret: your-secret-key
 *   bucket: my-bucket
 * </pre>
 */
public class OssPlugin extends Plugin {
    
    public String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    public String accessKeyId;
    public String accessKeySecret;
    public String bucket;
    
    public OSS client;
    
    public OssPlugin() {
    }
    
    /**
     * 构造函数 - 指定凭证和 bucket
     */
    public OssPlugin(String accessKeyId, String accessKeySecret, String bucket) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucket = bucket;
    }
    
    /**
     * 构造函数 - 指定 endpoint、凭证和 bucket
     */
    public OssPlugin(String endpoint, String accessKeyId, String accessKeySecret, String bucket) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucket = bucket;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        endpoint = app.conf.getString("oss", "endpoint", endpoint);
        accessKeyId = app.conf.getString("oss", "accessKeyId", accessKeyId);
        accessKeySecret = app.conf.getString("oss", "accessKeySecret", accessKeySecret);
        bucket = app.conf.getString("oss", "bucket", bucket);
        
        client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        app.log.info("OssPlugin: Connected to " + endpoint);
    }
    
    public void upload(String key, byte[] data) {
        client.putObject(bucket, key, new ByteArrayInputStream(data));
    }
    
    public void upload(String key, byte[] data, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(data.length);
        client.putObject(bucket, key, new ByteArrayInputStream(data), metadata);
    }
    
    public void upload(String key, InputStream stream, long length, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(length);
        client.putObject(bucket, key, stream, metadata);
    }
    
    public byte[] download(String key) {
        OSSObject object = client.getObject(bucket, key);
        try (InputStream is = object.getObjectContent()) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download: " + e.getMessage(), e);
        }
    }
    
    public void delete(String key) {
        client.deleteObject(bucket, key);
    }
    
    public boolean exists(String key) {
        return client.doesObjectExist(bucket, key);
    }
    
    public List<String> list(String prefix) {
        ObjectListing listing = client.listObjects(bucket, prefix);
        return listing.getObjectSummaries().stream()
            .map(OSSObjectSummary::getKey)
            .collect(Collectors.toList());
    }
    
    public String getPresignedUrl(String key, long expirationSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expirationSeconds * 1000);
        URL url = client.generatePresignedUrl(bucket, key, expiration);
        return url.toString();
    }
    
    public String getPublicUrl(String key) {
        return "https://" + bucket + "." + endpoint + "/" + key;
    }
    
    @Override
    public void uninstall() {
        if (client != null) {
            client.shutdown();
        }
    }
}
