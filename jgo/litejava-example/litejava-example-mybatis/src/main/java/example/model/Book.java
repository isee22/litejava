package example.model;

import java.sql.Timestamp;

/**
 * 图书模型
 */
public class Book {
    public Long id;
    public String title;
    public String author;
    public String isbn;
    public String description;
    public String coverImage;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
