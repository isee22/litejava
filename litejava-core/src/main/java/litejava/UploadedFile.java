package litejava;

import java.io.*;
import java.nio.file.*;

/**
 * 上传文件对象
 * 
 * <p>表示从 multipart/form-data 请求中解析出的文件。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 获取上传文件
 * UploadedFile file = ctx.file("avatar");
 * 
 * // 检查文件
 * if (file == null) {
 *     ctx.fail("请选择文件");
 *     return;
 * }
 * 
 * // 文件信息
 * String name = file.name;           // 原始文件名
 * String type = file.contentType;    // MIME 类型
 * long size = file.size;             // 文件大小
 * 
 * // 保存文件
 * file.saveTo("/uploads/avatar.jpg");
 * 
 * // 或使用 FilePlugin
 * String path = app.file.save(file, "avatars");
 * }</pre>
 * 
 * @see litejava.plugin.FilePlugin 文件处理插件
 */
public class UploadedFile {
    
    /** 原始文件名 */
    public String name;
    
    /** MIME 类型 */
    public String contentType;
    
    /** 文件大小（字节） */
    public long size;
    
    /** 文件内容 */
    public byte[] content;
    
    /** 表单字段名 */
    public String fieldName;
    
    /**
     * 保存文件到指定路径
     * 
     * @param path 目标路径
     * @throws IOException 保存失败
     */
    public void saveTo(String path) throws IOException {
        Path target = Paths.get(path);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }
    
    /**
     * 保存文件到指定 Path
     */
    public void saveTo(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }
    
    /**
     * 获取文件内容字符串（UTF-8）
     */
    public String asString() {
        return new String(content, Context.charset);
    }
    
    /**
     * 获取文件内容字符串（指定编码）
     */
    public String asString(String charset) {
        try {
            return new String(content, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(content);
        }
    }
    
    /**
     * 获取文件输入流
     */
    public InputStream asInputStream() {
        return new ByteArrayInputStream(content);
    }
    
    /**
     * 获取文件扩展名（含点）
     */
    public String getExtension() {
        if (name == null) return "";
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(dotIndex) : "";
    }
    
    /**
     * 检查是否为图片
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * 检查是否为指定类型
     */
    public boolean isType(String type) {
        if (contentType == null) return false;
        if (type.endsWith("/*")) {
            return contentType.startsWith(type.substring(0, type.length() - 1));
        }
        return contentType.equals(type);
    }
    
    @Override
    public String toString() {
        return "UploadedFile{name='" + name + "', type='" + contentType + 
               "', size=" + size + ", field='" + fieldName + "'}";
    }
}
