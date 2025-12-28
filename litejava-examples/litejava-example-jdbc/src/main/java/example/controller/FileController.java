package example.controller;

import litejava.Context;
import litejava.Routes;
import litejava.UploadedFile;

import java.io.File;
import java.util.Map;

/**
 * 文件上传下载控制器
 * 
 * <p>演示使用 FilePlugin 处理文件上传下载。
 */
public class FileController {
    
    private final String uploadSubDir;
    
    public FileController(String uploadSubDir) {
        this.uploadSubDir = uploadSubDir;
    }
    
    public Routes routes() {
        return new Routes()
            .post("/api/upload/cover", this::upload)
            .get("/api/files/:filename", this::download)
            .delete("/api/files/:filename", this::delete)
            .end();
    }
    
    /**
     * 文件上传
     */
    void upload(Context ctx) throws Exception {
        // 使用新的便捷 API
        UploadedFile file = ctx.file("file");
        
        if (file == null) {
            ctx.status(400).json(Map.of("error", "请选择文件"));
            return;
        }
        
        // 检查是否为图片
        if (!file.isImage()) {
            ctx.status(400).json(Map.of("error", "只能上传图片文件"));
            return;
        }
        
        // 使用 FilePlugin 保存文件
        String path = ctx.app.file.save(file, uploadSubDir);
        
        ctx.ok(Map.of(
            "url", "/api/files/" + path,
            "filename", path,
            "size", file.size,
            "message", "上传成功"
        ));
    }
    
    /**
     * 文件下载
     */
    void download(Context ctx) throws Exception {
        String filename = ctx.pathParam("filename");
        
        // 安全检查
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            ctx.status(400).json(Map.of("error", "非法文件名"));
            return;
        }
        
        // 使用 FilePlugin 下载
        File file = ctx.app.file.getPath(uploadSubDir + "/" + filename).toFile();
        if (!file.exists()) {
            ctx.status(404).json(Map.of("error", "文件不存在"));
            return;
        }
        
        // 图片内联显示，其他文件下载
        String contentType = ctx.app.file.guessContentType(filename);
        if (contentType.startsWith("image/")) {
            ctx.app.file.inline(ctx, file);
        } else {
            ctx.app.file.download(ctx, file);
        }
    }
    
    /**
     * 文件删除
     */
    void delete(Context ctx) throws Exception {
        String filename = ctx.pathParam("filename");
        
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            ctx.status(400).json(Map.of("error", "非法文件名"));
            return;
        }
        
        if (ctx.app.file.delete(uploadSubDir + "/" + filename)) {
            ctx.ok(Map.of("message", "删除成功"));
        } else {
            ctx.status(404).json(Map.of("error", "文件不存在"));
        }
    }
}
