package litejava;

import java.io.*;
import java.nio.file.*;

/**
 * Represents an uploaded file from multipart form data.
 */
public class UploadedFile {
    
    public String name;
    public String contentType;
    public long size;
    public byte[] content;
    
    /**
     * Save the uploaded file to the specified path.
     */
    public void saveTo(String path) throws IOException {
        Files.write(Paths.get(path), content);
    }
    
    /**
     * Get the file content as a string (UTF-8).
     */
    public String asString() {
        try {
            return new String(content, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(content);
        }
    }
    
    /**
     * Get an input stream for reading the file content.
     */
    public InputStream asInputStream() {
        return new ByteArrayInputStream(content);
    }
}
