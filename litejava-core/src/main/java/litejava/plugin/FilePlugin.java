package litejava.plugin;

import litejava.*;
import litejava.exception.LiteJavaException;
import litejava.util.Maps;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 文件处理插件 - 统一的文件上传下载处理
 * 
 * <p>FilePlugin 提供文件上传解析和下载响应的统一接口，支持：
 * <ul>
 *   <li>单文件/多文件上传解析</li>
 *   <li>文件大小限制</li>
 *   <li>文件类型白名单/黑名单</li>
 *   <li>文件下载（支持断点续传）</li>
 *   <li>临时文件自动清理</li>
 * </ul>
 * 
 * <h2>上传目录配置 (uploadDir)</h2>
 * 
 * <p>uploadDir 支持相对路径和绝对路径：
 * <ul>
 *   <li><b>相对路径</b>：相对于应用启动目录（user.dir），如 {@code ./uploads}</li>
 *   <li><b>绝对路径</b>：完整的文件系统路径</li>
 * </ul>
 * 
 * <h3>路径解析规则</h3>
 * <pre>
 * 假设应用启动目录为 /home/app（Linux）或 D:/app（Windows）
 * 
 * uploadDir = "./uploads"     -&gt; /home/app/uploads 或 D:/app/uploads
 * uploadDir = "uploads"       -&gt; /home/app/uploads 或 D:/app/uploads
 * uploadDir = "../data"       -&gt; /home/data 或 D:/data
 * uploadDir = "/var/uploads"  -&gt; /var/uploads（Linux 绝对路径）
 * uploadDir = "D:/uploads"    -&gt; D:/uploads（Windows 绝对路径）
 * </pre>
 * 
 * <h3>Windows 配置示例</h3>
 * <pre>
 * // application.properties
 * file.uploadDir=D:/myapp/uploads          // 推荐：使用正斜杠
 * file.uploadDir=D:\\myapp\\uploads        // 也可以：双反斜杠
 * file.uploadDir=./uploads                 // 相对路径：启动目录下的 uploads
 * 
 * // 代码配置
 * app.use(new FilePlugin().uploadDir("D:/myapp/uploads"));
 * </pre>
 * 
 * <h3>Linux 配置示例</h3>
 * <pre>
 * // application.properties
 * file.uploadDir=/var/www/uploads          // 绝对路径
 * file.uploadDir=/home/app/data/uploads    // 用户目录下
 * file.uploadDir=./uploads                 // 相对路径：启动目录下的 uploads
 * 
 * // 代码配置
 * app.use(new FilePlugin().uploadDir("/var/www/uploads"));
 * 
 * // 注意：确保应用对目录有读写权限
 * // chmod 755 /var/www/uploads
 * // chown www-data:www-data /var/www/uploads
 * </pre>
 * 
 * <h3>Docker 部署建议</h3>
 * <pre>
 * // 使用数据卷挂载
 * docker run -v /host/uploads:/app/uploads myapp
 * 
 * // 配置文件中使用容器内路径
 * file.uploadDir=/app/uploads
 * </pre>
 * 
 * <h2>完整配置 (application.properties)</h2>
 * <pre>
 * // 文件大小限制
 * file.maxFileSize=10485760       // 单文件最大 10MB (10*1024*1024)
 * file.maxRequestSize=52428800    // 请求最大 50MB (50*1024*1024)
 * 
 * // 存储目录
 * file.uploadDir=./uploads        // 上传文件存储目录
 * file.tempDir=/tmp               // 临时文件目录（默认系统临时目录）
 * 
 * // 文件类型限制（逗号分隔，支持通配符）
 * file.allowedTypes=image/*,application/pdf,application/msword
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 单文件上传
 * app.post("/upload", ctx -> {
 *     UploadedFile file = ctx.file("file");
 *     if (file == null) {
 *         ctx.fail("请选择文件");
 *         return;
 *     }
 *     
 *     // 保存文件（返回相对路径）
 *     String path = app.file.save(file, "avatars");
 *     ctx.ok(Map.of("url", "/files/" + path));
 * });
 * 
 * // 多文件上传
 * app.post("/upload/batch", ctx -> {
 *     List<UploadedFile> files = ctx.files("files");
 *     List<String> urls = new ArrayList<>();
 *     for (UploadedFile file : files) {
 *         urls.add(app.file.save(file, "documents"));
 *     }
 *     ctx.ok(Map.of("urls", urls));
 * });
 * 
 * // 文件下载
 * app.get("/download/:filename", ctx -> {
 *     String filename = ctx.pathParam("filename");
 *     app.file.download(ctx, new File("uploads/" + filename));
 * });
 * }</pre>
 * 
 * <h2>代码配置（链式 API）</h2>
 * <pre>{@code
 * // Windows
 * app.use(new FilePlugin()
 *     .uploadDir("D:/myapp/uploads")
 *     .maxFileSizeMB(20)
 *     .maxRequestSizeMB(100)
 *     .allowTypes("image/*", "application/pdf")
 *     .denyExtensions("exe", "bat", "sh"));
 * 
 * // Linux
 * app.use(new FilePlugin()
 *     .uploadDir("/var/www/uploads")
 *     .maxFileSizeMB(20)
 *     .allowTypes("image/*"));
 * }</pre>
 * 
 * <h2>自定义实现</h2>
 * <pre>{@code
 * // 替换为支持流式处理的实现（大文件场景）
 * public class StreamingFilePlugin extends FilePlugin {
 *     @Override
 *     public Map<String, List<UploadedFile>> parseMultipart(Context ctx) {
 *         // 使用 Apache Commons FileUpload 等库实现流式解析
 *     }
 * }
 * 
 * app.use(new StreamingFilePlugin());
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see UploadedFile 上传文件对象
 * @see Context#file(String) 获取上传文件
 * @see Context#files(String) 获取多个上传文件
 */
public class FilePlugin extends Plugin {
    
    /** 单例插件，同类型自动替换 */
    @Override
    public boolean singleton() {
        return true;
    }
    
    // ==================== 配置项 ====================
    
    /** 单文件最大大小（字节），默认 10MB */
    public long maxFileSize = 10 * 1024 * 1024;
    
    /** 请求最大大小（字节），默认 50MB */
    public long maxRequestSize = 50 * 1024 * 1024;
    
    /**
     * 上传文件存储目录
     * 
     * <p>支持相对路径和绝对路径：
     * <ul>
     *   <li>相对路径（如 {@code ./uploads}）：相对于应用启动目录（user.dir）</li>
     *   <li>绝对路径（如 {@code /var/uploads} 或 {@code D:/uploads}）：直接使用</li>
     * </ul>
     * 
     * <p>Windows 路径建议使用正斜杠 {@code /} 或双反斜杠 {@code \\}
     * 
     * @see #getPath(String) 获取文件完整路径
     */
    public String uploadDir = "./uploads";
    
    /**
     * 临时文件目录
     * 
     * <p>默认使用系统临时目录（java.io.tmpdir）：
     * <ul>
     *   <li>Windows: {@code C:\Users\xxx\AppData\Local\Temp}</li>
     *   <li>Linux: {@code /tmp}</li>
     *   <li>macOS: {@code /var/folders/...}</li>
     * </ul>
     */
    public String tempDir = System.getProperty("java.io.tmpdir");
    
    /** 允许的 MIME 类型（支持通配符如 image/*），null 表示允许所有 */
    public Set<String> allowedTypes = null;
    
    /** 禁止的 MIME 类型 */
    public Set<String> deniedTypes = new HashSet<>(Arrays.asList(
        "application/x-msdownload",  // .exe
        "application/x-sh",          // .sh
        "application/x-bat"          // .bat
    ));
    
    /** 允许的文件扩展名（不含点），null 表示不限制 */
    public Set<String> allowedExtensions = null;
    
    /** 禁止的文件扩展名 */
    public Set<String> deniedExtensions = new HashSet<>(Arrays.asList(
        "exe", "sh", "bat", "cmd", "com", "msi", "dll", "scr"
    ));
    
    /** 是否使用 UUID 重命名文件，默认 true */
    public boolean useUuidFilename = true;
    
    /** 是否保留原始文件扩展名，默认 true */
    public boolean keepExtension = true;
    
    /** 内存阈值，超过此大小写入临时文件（字节），默认 1MB */
    public int memoryThreshold = 1024 * 1024;
    
    // ==================== 构造函数 ====================
    
    public FilePlugin() {
    }
    
    // ==================== 链式配置 ====================
    
    public FilePlugin maxFileSize(long bytes) {
        this.maxFileSize = bytes;
        return this;
    }
    
    public FilePlugin maxFileSizeMB(int mb) {
        this.maxFileSize = mb * 1024L * 1024L;
        return this;
    }
    
    public FilePlugin maxRequestSize(long bytes) {
        this.maxRequestSize = bytes;
        return this;
    }
    
    public FilePlugin maxRequestSizeMB(int mb) {
        this.maxRequestSize = mb * 1024L * 1024L;
        return this;
    }
    
    public FilePlugin uploadDir(String dir) {
        this.uploadDir = dir;
        return this;
    }
    
    public FilePlugin allowTypes(String... types) {
        if (this.allowedTypes == null) {
            this.allowedTypes = new HashSet<>();
        }
        this.allowedTypes.addAll(Arrays.asList(types));
        return this;
    }
    
    public FilePlugin denyTypes(String... types) {
        this.deniedTypes.addAll(Arrays.asList(types));
        return this;
    }
    
    public FilePlugin allowExtensions(String... extensions) {
        if (this.allowedExtensions == null) {
            this.allowedExtensions = new HashSet<>();
        }
        for (String ext : extensions) {
            this.allowedExtensions.add(ext.toLowerCase().replace(".", ""));
        }
        return this;
    }
    
    public FilePlugin denyExtensions(String... extensions) {
        for (String ext : extensions) {
            this.deniedExtensions.add(ext.toLowerCase().replace(".", ""));
        }
        return this;
    }
    
    // ==================== 插件生命周期 ====================
    
    @Override
    public void config() {
        // 从配置文件读取
        maxFileSize = app.conf.getLong("file", "maxFileSize", maxFileSize);
        maxRequestSize = app.conf.getLong("file", "maxRequestSize", maxRequestSize);
        uploadDir = app.conf.getString("file", "uploadDir", uploadDir);
        tempDir = app.conf.getString("file", "tempDir", tempDir);
        
        String allowedTypesStr = app.conf.getString("file", "allowedTypes", null);
        if (allowedTypesStr != null && !allowedTypesStr.isEmpty()) {
            allowTypes(allowedTypesStr.split(","));
        }
        
        // 确保上传目录存在
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            app.log.warn("Failed to create upload directory: " + uploadDir);
        }
    }
    
    // ==================== 文件上传解析 ====================
    
    /**
     * 解析 multipart/form-data 请求
     * 
     * @param ctx 请求上下文
     * @return 文件 Map，key 为字段名，value 为文件列表（支持同名多文件）
     * @throws FileUploadException 解析失败或超出限制
     */
    public Map<String, List<UploadedFile>> parseMultipart(Context ctx) {
        Map<String, List<UploadedFile>> result = new LinkedHashMap<>();
        
        String contentType = ctx.header("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return result;
        }
        
        // 检查请求大小
        String contentLengthStr = ctx.header("Content-Length");
        if (contentLengthStr != null) {
            long contentLength = Long.parseLong(contentLengthStr);
            if (contentLength > maxRequestSize) {
                throw new FileUploadException("Request size " + contentLength + 
                    " exceeds limit " + maxRequestSize);
            }
        }
        
        // 解析 boundary
        int boundaryIdx = contentType.indexOf("boundary=");
        if (boundaryIdx < 0) return result;
        
        String boundary = contentType.substring(boundaryIdx + 9);
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        
        // 使用字节数组解析，避免字符串转换损坏二进制数据
        byte[] bodyBytes = ctx.getRawData();
        byte[] boundaryBytes = ("--" + boundary).getBytes(Context.charset);
        
        int pos = 0;
        while (pos < bodyBytes.length) {
            // 找到 boundary 开始位置
            int boundaryStart = indexOf(bodyBytes, boundaryBytes, pos);
            if (boundaryStart < 0) break;
            
            // 跳过 boundary
            int partStart = boundaryStart + boundaryBytes.length;
            if (partStart >= bodyBytes.length) break;
            
            // 检查是否是结束标记 --boundary--
            if (partStart + 2 <= bodyBytes.length && 
                bodyBytes[partStart] == '-' && bodyBytes[partStart + 1] == '-') {
                break;
            }
            
            // 跳过 CRLF
            if (partStart < bodyBytes.length && bodyBytes[partStart] == '\r') partStart++;
            if (partStart < bodyBytes.length && bodyBytes[partStart] == '\n') partStart++;
            
            // 找到下一个 boundary（作为当前 part 的结束）
            int nextBoundary = indexOf(bodyBytes, boundaryBytes, partStart);
            if (nextBoundary < 0) nextBoundary = bodyBytes.length;
            
            // 解析这个 part
            parseMultipartPartBytes(bodyBytes, partStart, nextBoundary, result);
            
            pos = nextBoundary;
        }
        
        return result;
    }
    
    /**
     * 在字节数组中查找子数组
     */
    private int indexOf(byte[] data, byte[] pattern, int start) {
        outer:
        for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
    
    /**
     * 解析单个 multipart 部分（字节数组版本）
     */
    private void parseMultipartPartBytes(byte[] data, int start, int end, 
                                          Map<String, List<UploadedFile>> result) {
        // 找到头部和内容的分隔（\r\n\r\n 或 \n\n）
        int headerEnd = -1;
        int contentStart = -1;
        
        for (int i = start; i < end - 3; i++) {
            if (data[i] == '\r' && data[i+1] == '\n' && data[i+2] == '\r' && data[i+3] == '\n') {
                headerEnd = i;
                contentStart = i + 4;
                break;
            }
            if (data[i] == '\n' && data[i+1] == '\n') {
                headerEnd = i;
                contentStart = i + 2;
                break;
            }
        }
        
        if (headerEnd < 0 || contentStart < 0) return;
        
        // 解析头部（头部是文本，可以转字符串）
        String headerSection = new String(data, start, headerEnd - start, Context.charset);
        
        String name = null, filename = null;
        String fileContentType = "application/octet-stream";
        
        for (String line : headerSection.split("\r?\n")) {
            String lineLower = line.toLowerCase();
            if (lineLower.startsWith("content-disposition:")) {
                int nameIdx = line.indexOf("name=\"");
                if (nameIdx >= 0) {
                    int nameEnd = line.indexOf("\"", nameIdx + 6);
                    if (nameEnd > nameIdx) name = line.substring(nameIdx + 6, nameEnd);
                }
                int filenameIdx = line.indexOf("filename=\"");
                if (filenameIdx >= 0) {
                    int filenameEnd = line.indexOf("\"", filenameIdx + 10);
                    if (filenameEnd > filenameIdx) filename = line.substring(filenameIdx + 10, filenameEnd);
                }
            } else if (lineLower.startsWith("content-type:")) {
                fileContentType = line.substring(13).trim();
            }
        }
        
        // 只处理文件字段
        if (name == null || filename == null || filename.isEmpty()) return;
        
        // 计算内容结束位置（去除尾部的 \r\n）
        int contentEnd = end;
        if (contentEnd > contentStart && data[contentEnd - 1] == '\n') contentEnd--;
        if (contentEnd > contentStart && data[contentEnd - 1] == '\r') contentEnd--;
        
        // 直接复制字节数组，不经过字符串转换
        int contentLength = contentEnd - contentStart;
        byte[] fileContent = new byte[contentLength];
        System.arraycopy(data, contentStart, fileContent, 0, contentLength);
        
        // 检查文件大小
        if (fileContent.length > maxFileSize) {
            throw new FileUploadException("File '" + filename + "' size " + fileContent.length + 
                " exceeds limit " + maxFileSize);
        }
        
        // 检查文件类型
        validateFileType(filename, fileContentType);
        
        // 创建 UploadedFile
        UploadedFile file = new UploadedFile();
        file.name = filename;
        file.contentType = fileContentType;
        file.content = fileContent;
        file.size = fileContent.length;
        file.fieldName = name;
        
        // 添加到结果（支持同名多文件）
        result.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
    }
    
    /**
     * 验证文件类型
     */
    private void validateFileType(String filename, String contentType) {
        // 检查扩展名
        String ext = getExtension(filename).toLowerCase();
        
        if (deniedExtensions.contains(ext)) {
            throw new FileUploadException("File extension '" + ext + "' is not allowed");
        }
        
        if (allowedExtensions != null && !allowedExtensions.isEmpty() && !allowedExtensions.contains(ext)) {
            throw new FileUploadException("File extension '" + ext + "' is not in allowed list");
        }
        
        // 检查 MIME 类型
        if (deniedTypes.contains(contentType)) {
            throw new FileUploadException("Content type '" + contentType + "' is not allowed");
        }
        
        if (allowedTypes != null && !allowedTypes.isEmpty()) {
            boolean allowed = false;
            for (String pattern : allowedTypes) {
                if (matchMimeType(contentType, pattern)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new FileUploadException("Content type '" + contentType + "' is not in allowed list");
            }
        }
    }
    
    /**
     * MIME 类型匹配（支持通配符）
     */
    private boolean matchMimeType(String type, String pattern) {
        if (pattern.equals("*/*") || pattern.equals(type)) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return type.startsWith(prefix);
        }
        return false;
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 获取单个上传文件
     * 
     * @param ctx 请求上下文
     * @param fieldName 表单字段名
     * @return 上传文件，不存在返回 null
     */
    public UploadedFile getFile(Context ctx, String fieldName) {
        Map<String, List<UploadedFile>> files = parseMultipart(ctx);
        List<UploadedFile> list = files.get(fieldName);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
    
    /**
     * 获取多个上传文件（同一字段名）
     * 
     * @param ctx 请求上下文
     * @param fieldName 表单字段名
     * @return 文件列表，不存在返回空列表
     */
    public List<UploadedFile> getFiles(Context ctx, String fieldName) {
        Map<String, List<UploadedFile>> files = parseMultipart(ctx);
        return files.getOrDefault(fieldName, Collections.emptyList());
    }
    
    /**
     * 获取所有上传文件（扁平化）
     * 
     * @param ctx 请求上下文
     * @return 所有文件列表
     */
    public List<UploadedFile> getAllFiles(Context ctx) {
        Map<String, List<UploadedFile>> files = parseMultipart(ctx);
        List<UploadedFile> result = new ArrayList<>();
        for (List<UploadedFile> list : files.values()) {
            result.addAll(list);
        }
        return result;
    }
    
    // ==================== 文件保存 ====================
    
    /**
     * 保存上传文件到默认目录
     * 
     * @param file 上传文件
     * @return 保存后的相对路径
     */
    public String save(UploadedFile file) throws IOException {
        return save(file, "");
    }
    
    /**
     * 保存上传文件到指定子目录
     * 
     * @param file 上传文件
     * @param subDir 子目录
     * @return 保存后的相对路径
     */
    public String save(UploadedFile file, String subDir) throws IOException {
        String filename = generateFilename(file);
        
        Path dir = Paths.get(uploadDir, subDir);
        Files.createDirectories(dir);
        
        Path filePath = dir.resolve(filename);
        Files.write(filePath, file.content);
        
        return subDir.isEmpty() ? filename : subDir + "/" + filename;
    }
    
    /**
     * 保存上传文件到指定完整路径
     * 
     * @param file 上传文件
     * @param targetPath 目标路径
     */
    public void saveTo(UploadedFile file, String targetPath) throws IOException {
        Path path = Paths.get(targetPath);
        Files.createDirectories(path.getParent());
        Files.write(path, file.content);
    }
    
    /**
     * 生成文件名
     */
    public String generateFilename(UploadedFile file) {
        if (!useUuidFilename) {
            return sanitizeFilename(file.name);
        }
        
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (keepExtension) {
            String ext = getExtension(file.name);
            return ext.isEmpty() ? uuid : uuid + ext;
        }
        return uuid;
    }
    
    /**
     * 清理文件名（移除危险字符）
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        // 移除路径分隔符和特殊字符
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_");
    }
    
    /**
     * 获取文件扩展名（含点）
     */
    public String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }
    
    // ==================== 文件下载 ====================
    
    /**
     * 发送文件下载响应
     * 
     * @param ctx 请求上下文
     * @param file 文件
     */
    public void download(Context ctx, File file) throws IOException {
        download(ctx, file, file.getName());
    }
    
    /**
     * 发送文件下载响应（自定义文件名）
     * 
     * @param ctx 请求上下文
     * @param file 文件
     * @param filename 下载时显示的文件名
     */
    public void download(Context ctx, File file, String filename) throws IOException {
        if (!file.exists()) {
            ctx.status(404).json(Maps.of("error", "File not found"));
            return;
        }
        
        long fileSize = file.length();
        String contentType = guessContentType(filename);
        
        // 检查是否支持断点续传
        String rangeHeader = ctx.header("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            handleRangeRequest(ctx, file, filename, rangeHeader, fileSize, contentType);
        } else {
            // 普通下载
            byte[] content = Files.readAllBytes(file.toPath());
            ctx.header("Content-Type", contentType);
            ctx.header("Content-Disposition", "attachment; filename=\"" + encodeFilename(filename) + "\"");
            ctx.header("Content-Length", String.valueOf(fileSize));
            ctx.header("Accept-Ranges", "bytes");
            ctx.data(content);
        }
    }
    
    /**
     * 发送字节数组作为文件下载
     */
    public void download(Context ctx, byte[] content, String filename) {
        String contentType = guessContentType(filename);
        ctx.header("Content-Type", contentType);
        ctx.header("Content-Disposition", "attachment; filename=\"" + encodeFilename(filename) + "\"");
        ctx.header("Content-Length", String.valueOf(content.length));
        ctx.data(content);
    }
    
    /**
     * 发送输入流作为文件下载
     */
    public void download(Context ctx, InputStream stream, String filename, long size) throws IOException {
        String contentType = guessContentType(filename);
        ctx.header("Content-Type", contentType);
        ctx.header("Content-Disposition", "attachment; filename=\"" + encodeFilename(filename) + "\"");
        if (size > 0) {
            ctx.header("Content-Length", String.valueOf(size));
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = stream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        ctx.data(baos.toByteArray());
    }
    
    /**
     * 内联显示文件（如图片、PDF）
     */
    public void inline(Context ctx, File file) throws IOException {
        inline(ctx, file, file.getName());
    }
    
    /**
     * 内联显示文件
     */
    public void inline(Context ctx, File file, String filename) throws IOException {
        if (!file.exists()) {
            ctx.status(404).json(Maps.of("error", "File not found"));
            return;
        }
        
        byte[] content = Files.readAllBytes(file.toPath());
        String contentType = guessContentType(filename);
        ctx.header("Content-Type", contentType);
        ctx.header("Content-Disposition", "inline; filename=\"" + encodeFilename(filename) + "\"");
        ctx.data(content);
    }
    
    /**
     * 处理断点续传请求
     */
    private void handleRangeRequest(Context ctx, File file, String filename, 
                                     String rangeHeader, long fileSize, String contentType) throws IOException {
        // 解析 Range: bytes=start-end
        String range = rangeHeader.substring(6);
        String[] parts = range.split("-");
        
        long start = 0;
        long end = fileSize - 1;
        
        if (!parts[0].isEmpty()) {
            start = Long.parseLong(parts[0]);
        }
        if (parts.length > 1 && !parts[1].isEmpty()) {
            end = Long.parseLong(parts[1]);
        }
        
        // 验证范围
        if (start > end || start >= fileSize) {
            ctx.status(416);  // Range Not Satisfiable
            ctx.header("Content-Range", "bytes */" + fileSize);
            return;
        }
        
        end = Math.min(end, fileSize - 1);
        long contentLength = end - start + 1;
        
        // 读取指定范围
        byte[] content = new byte[(int) contentLength];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            raf.readFully(content);
        }
        
        ctx.status(206);  // Partial Content
        ctx.header("Content-Type", contentType);
        ctx.header("Content-Disposition", "attachment; filename=\"" + encodeFilename(filename) + "\"");
        ctx.header("Content-Length", String.valueOf(contentLength));
        ctx.header("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        ctx.header("Accept-Ranges", "bytes");
        ctx.data(content);
    }
    
    /**
     * 编码文件名（处理中文等特殊字符）
     */
    private String encodeFilename(String filename) {
        try {
            // RFC 5987 编码
            return java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return filename;
        }
    }
    
    /**
     * 猜测文件 MIME 类型
     */
    public String guessContentType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        switch (ext) {
            // 图片
            case ".jpg": case ".jpeg": return "image/jpeg";
            case ".png": return "image/png";
            case ".gif": return "image/gif";
            case ".webp": return "image/webp";
            case ".svg": return "image/svg+xml";
            case ".ico": return "image/x-icon";
            case ".bmp": return "image/bmp";
            
            // 文档
            case ".pdf": return "application/pdf";
            case ".doc": return "application/msword";
            case ".docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls": return "application/vnd.ms-excel";
            case ".xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt": return "application/vnd.ms-powerpoint";
            case ".pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            
            // 文本
            case ".txt": return "text/plain";
            case ".html": case ".htm": return "text/html";
            case ".css": return "text/css";
            case ".js": return "application/javascript";
            case ".json": return "application/json";
            case ".xml": return "application/xml";
            case ".csv": return "text/csv";
            case ".md": return "text/markdown";
            
            // 压缩
            case ".zip": return "application/zip";
            case ".rar": return "application/x-rar-compressed";
            case ".7z": return "application/x-7z-compressed";
            case ".tar": return "application/x-tar";
            case ".gz": return "application/gzip";
            
            // 音视频
            case ".mp3": return "audio/mpeg";
            case ".wav": return "audio/wav";
            case ".mp4": return "video/mp4";
            case ".webm": return "video/webm";
            case ".avi": return "video/x-msvideo";
            case ".mov": return "video/quicktime";
            
            // 字体
            case ".woff": return "font/woff";
            case ".woff2": return "font/woff2";
            case ".ttf": return "font/ttf";
            case ".otf": return "font/otf";
            case ".eot": return "application/vnd.ms-fontobject";
            
            default: return "application/octet-stream";
        }
    }
    
    // ==================== 文件删除 ====================
    
    /**
     * 删除上传目录中的文件
     * 
     * @param relativePath 相对路径
     * @return 是否删除成功
     */
    public boolean delete(String relativePath) {
        if (relativePath == null || relativePath.contains("..")) {
            return false;  // 防止路径遍历攻击
        }
        
        try {
            Path path = Paths.get(uploadDir, relativePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.contains("..")) {
            return false;
        }
        return Files.exists(Paths.get(uploadDir, relativePath));
    }
    
    /**
     * 获取文件完整路径
     */
    public Path getPath(String relativePath) {
        return Paths.get(uploadDir, relativePath);
    }
    
    // ==================== 异常类 ====================
    
    /**
     * 文件上传异常
     */
    public static class FileUploadException extends LiteJavaException {
        public FileUploadException(String message) {
            super(message, 400);
        }
    }
}
