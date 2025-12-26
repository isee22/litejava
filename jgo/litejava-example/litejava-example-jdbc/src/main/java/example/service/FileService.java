package example.service;

import litejava.UploadedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文件服务
 */
public class FileService {
    
    private final String uploadDir;
    private final long maxSize;
    
    public FileService(String uploadDir, long maxSize) {
        this.uploadDir = uploadDir;
        this.maxSize = maxSize;
        
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, Object> saveImage(UploadedFile file) throws IOException {
        if (file.size > maxSize) {
            return Map.of("error", "文件大小超过限制");
        }
        
        if (!file.contentType.startsWith("image/")) {
            return Map.of("error", "只能上传图片文件");
        }
        
        String ext = getExtension(file.name);
        String filename = UUID.randomUUID().toString() + ext;
        Path filePath = Paths.get(uploadDir, filename);
        
        Files.write(filePath, file.content);
        
        return Map.of(
            "url", "/api/files/" + filename,
            "filename", filename,
            "size", file.size,
            "message", "上传成功"
        );
    }
    
    public Map<String, Object> getFile(String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Map.of("error", "非法文件名", "status", 400);
        }
        
        Path filePath = Paths.get(uploadDir, filename);
        if (!Files.exists(filePath)) {
            return Map.of("error", "文件不存在", "status", 404);
        }
        
        byte[] content = Files.readAllBytes(filePath);
        String contentType = getContentType(filename);
        
        return Map.of("content", content, "contentType", contentType);
    }
    
    public Map<String, Object> deleteFile(String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Map.of("error", "非法文件名", "status", 400);
        }
        
        Path filePath = Paths.get(uploadDir, filename);
        if (Files.deleteIfExists(filePath)) {
            return Map.of("message", "删除成功");
        } else {
            return Map.of("error", "文件不存在", "status", 404);
        }
    }
    
    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }
    
    private String getContentType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        switch (ext) {
            case ".jpg":
            case ".jpeg": return "image/jpeg";
            case ".png": return "image/png";
            case ".gif": return "image/gif";
            case ".svg": return "image/svg+xml";
            case ".pdf": return "application/pdf";
            default: return "application/octet-stream";
        }
    }
}
