package litejava.plugins.storage;

import litejava.Plugin;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS S3 / MinIO 文件存储插件
 * 
 * <h2>配置</h2>
 * <pre>
 * s3:
 *   endpoint: http://localhost:9000  # MinIO 或留空用 AWS
 *   region: us-east-1
 *   accessKey: minioadmin
 *   secretKey: minioadmin
 *   bucket: my-bucket
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式一：使用配置文件
 * S3Plugin s3 = new S3Plugin();
 * app.use(s3);
 * 
 * // 方式二：代码指定
 * S3Plugin s3 = new S3Plugin();
 * s3.accessKey = "minioadmin";
 * s3.secretKey = "minioadmin";
 * s3.bucket = "my-bucket";
 * app.use(s3);
 * 
 * // 方式三：构造函数
 * S3Plugin s3 = new S3Plugin("minioadmin", "minioadmin", "my-bucket");
 * app.use(s3);
 * }</pre>
 */
public class S3Plugin extends Plugin {
    
    public String endpoint;
    public String region = "us-east-1";
    public String accessKey;
    public String secretKey;
    public String bucket;
    
    public S3Client client;
    public S3Presigner presigner;
    
    public S3Plugin() {
    }
    
    /**
     * 构造函数 - 指定凭证和 bucket
     */
    public S3Plugin(String accessKey, String secretKey, String bucket) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
    }
    
    /**
     * 构造函数 - 指定 endpoint（MinIO）、凭证和 bucket
     */
    public S3Plugin(String endpoint, String accessKey, String secretKey, String bucket) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        endpoint = app.conf.getString("s3", "endpoint", endpoint);
        region = app.conf.getString("s3", "region", region);
        accessKey = app.conf.getString("s3", "accessKey", accessKey);
        secretKey = app.conf.getString("s3", "secretKey", secretKey);
        bucket = app.conf.getString("s3", "bucket", bucket);
        
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(Region.of(region));
        
        if (accessKey != null && secretKey != null) {
            StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            );
            builder.credentialsProvider(credentials);
            presignerBuilder.credentialsProvider(credentials);
        }
        
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            presignerBuilder.endpointOverride(URI.create(endpoint));
            builder.forcePathStyle(true); // MinIO 需要
        }
        
        client = builder.build();
        presigner = presignerBuilder.build();
        
        app.log.info("S3Plugin: Connected to " + (endpoint != null ? endpoint : "AWS S3"));
    }
    
    public void upload(String key, byte[] data, String contentType) {
        client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(data)
        );
    }
    
    public void upload(String key, InputStream stream, long length, String contentType) {
        client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromInputStream(stream, length)
        );
    }
    
    public byte[] download(String key) {
        return client.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        ).asByteArray();
    }
    
    public void delete(String key) {
        client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        );
    }
    
    public boolean exists(String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
    
    public List<String> list(String prefix) {
        ListObjectsV2Response response = client.listObjectsV2(
            ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build()
        );
        return response.contents().stream()
            .map(S3Object::key)
            .collect(Collectors.toList());
    }
    
    public String getPresignedUrl(String key, Duration expiration) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build();
        return presigner.presignGetObject(request).url().toString();
    }
    
    @Override
    public void uninstall() {
        if (client != null) client.close();
        if (presigner != null) presigner.close();
    }
}
