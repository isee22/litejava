package litejava.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 文件工具类 - 统一处理文件系统和 classpath 资源
 * 
 * <h2>路径格式</h2>
 * <ul>
 *   <li>{@code /data/uploads/file.txt} - 绝对路径 → 文件系统</li>
 *   <li>{@code D:/uploads/file.txt} - Windows 绝对路径 → 文件系统</li>
 *   <li>{@code static/file.txt} - 相对路径 → 先文件系统，没有再 classpath</li>
 *   <li>{@code classpath:static/file.txt} - 强制 classpath</li>
 *   <li>{@code file:/data/file.txt} - 强制文件系统</li>
 * </ul>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 */
public class Files {
    
    /** MIME 类型映射 */
    public static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        // 文本
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("htm", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("xml", "application/xml");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("csv", "text/csv");
        MIME_TYPES.put("md", "text/markdown");
        
        // 图片
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("bmp", "image/bmp");
        
        // 字体
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("otf", "font/otf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        
        // 文档
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("doc", "application/msword");
        MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPES.put("xls", "application/vnd.ms-excel");
        MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        
        // 压缩
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("rar", "application/x-rar-compressed");
        MIME_TYPES.put("7z", "application/x-7z-compressed");
        MIME_TYPES.put("tar", "application/x-tar");
        MIME_TYPES.put("gz", "application/gzip");
        
        // 音视频
        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("webm", "video/webm");
        MIME_TYPES.put("avi", "video/x-msvideo");
        MIME_TYPES.put("mov", "video/quicktime");
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("wav", "audio/wav");
        MIME_TYPES.put("ogg", "audio/ogg");
        MIME_TYPES.put("flac", "audio/flac");
    }
    
    /**
     * 读取文件（自动检测路径类型）
     * @param path 文件路径
     * @return 文件内容，不存在返回 null
     */
    public static byte[] read(String path) {
        if (path == null || path.isEmpty()) return null;
        
        // file: 前缀 - 强制文件系统
        if (path.startsWith("file:")) {
            return readFromFileSystem(path.substring(5));
        }
        
        // classpath: 前缀 - 强制 classpath
        if (path.startsWith("classpath:")) {
            return readFromClasspath(path.substring(10));
        }
        
        // 自动检测：先文件系统，没有再 classpath
        byte[] content = readFromFileSystem(path);
        if (content != null) return content;
        return readFromClasspath(path);
    }
    
    /**
     * 读取文件（指定基础目录）
     * @param directory 基础目录
     * @param relativePath 相对路径
     * @return 文件内容，不存在返回 null
     */
    public static byte[] read(String directory, String relativePath) {
        if (directory == null || relativePath == null) return null;
        
        String dir = directory.endsWith("/") ? directory : directory + "/";
        
        // file: 前缀
        if (dir.startsWith("file:")) {
            return readFromFileSystem(dir.substring(5), relativePath);
        }
        
        // classpath: 前缀
        if (dir.startsWith("classpath:")) {
            return readFromClasspath(dir.substring(10) + relativePath);
        }
        
        // 自动检测
        if (new File(dir).exists()) {
            return readFromFileSystem(dir, relativePath);
        }
        return readFromClasspath(dir + relativePath);
    }
    
    /**
     * 从文件系统读取
     */
    public static byte[] readFromFileSystem(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        } catch (IOException e) {
            // 文件读取失败，返回 null 让调用方处理
        } catch (Exception e) {
            // 其他异常（如权限问题）
        }
        return null;
    }
    
    /**
     * 从文件系统读取（带基础目录和路径安全检查）
     */
    public static byte[] readFromFileSystem(String directory, String relativePath) {
        try {
            Path basePath = Paths.get(directory).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(relativePath).normalize();
            
            // 确保路径在目录内（防止路径遍历攻击）
            if (!filePath.startsWith(basePath)) {
                return null;
            }
            
            File file = filePath.toFile();
            if (file.exists() && file.isFile()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        } catch (IOException e) {
            // 文件读取失败，返回 null 让调用方处理
        } catch (Exception e) {
            // 其他异常（如路径解析失败）
        }
        return null;
    }
    
    /**
     * 从 classpath 读取
     */
    public static byte[] readFromClasspath(String path) {
        try (InputStream is = Files.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 写入文件
     * @param path 文件路径
     * @param data 文件内容
     */
    public static void write(String path, byte[] data) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        java.nio.file.Files.write(file.toPath(), data);
    }
    
    /**
     * 写入文件（指定基础目录）
     */
    public static void write(String directory, String relativePath, byte[] data) throws IOException {
        String dir = directory.endsWith("/") ? directory : directory + "/";
        write(dir + relativePath, data);
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
    
    /**
     * 获取 MIME 类型
     */
    public static String getMimeType(String filename) {
        String ext = getExtension(filename);
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }
    
    /**
     * 检查文件是否存在
     */
    public static boolean exists(String path) {
        if (path == null || path.isEmpty()) return false;
        
        if (path.startsWith("file:")) {
            return new File(path.substring(5)).exists();
        }
        
        if (path.startsWith("classpath:")) {
            return Files.class.getClassLoader().getResource(path.substring(10)) != null;
        }
        
        // 自动检测
        if (new File(path).exists()) return true;
        return Files.class.getClassLoader().getResource(path) != null;
    }
    
    /**
     * 确保目录存在
     */
    public static void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
