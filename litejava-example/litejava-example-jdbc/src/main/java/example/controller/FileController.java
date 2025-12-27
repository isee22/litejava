package example.controller;

import example.service.FileService;
import litejava.Context;
import litejava.Routes;
import litejava.UploadedFile;

import java.util.Map;

/**
 * 文件上传下载控制器
 */
public class FileController {
    
    private final FileService fileService;
    
    public FileController(String uploadDir, long maxSize) {
        this.fileService = new FileService(uploadDir, maxSize);
    }
    
    public Routes routes() {
        return new Routes()
            .post("/api/upload/cover", this::upload)
            .get("/api/files/:filename", this::download)
            .delete("/api/files/:filename", this::delete)
            .end();
    }
    
    void upload(Context ctx) throws Exception {
        Map<String, UploadedFile> files = ctx.getFiles();
        UploadedFile file = files.get("file");
        
        if (file == null) {
            ctx.status(400).json(Map.of("error", "请选择文件"));
            return;
        }
        
        Map<String, Object> result = fileService.saveImage(file);
        if (result.containsKey("error")) {
            ctx.status(400).json(result);
        } else {
            ctx.json(result);
        }
    }
    
    void download(Context ctx) throws Exception {
        String filename = ctx.pathParam("filename");
        Map<String, Object> result = fileService.getFile(filename);
        
        if (result.containsKey("error")) {
            ctx.status((int) result.get("status")).json(Map.of("error", result.get("error")));
        } else {
            ctx.header("Content-Type", (String) result.get("contentType"));
            ctx.data((byte[]) result.get("content"));
        }
    }
    
    void delete(Context ctx) throws Exception {
        String filename = ctx.pathParam("filename");
        Map<String, Object> result = fileService.deleteFile(filename);
        
        if (result.containsKey("error")) {
            ctx.status((int) result.get("status")).json(Map.of("error", result.get("error")));
        } else {
            ctx.json(result);
        }
    }
}
