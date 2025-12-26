package example.model;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * 图书模型
 */
@Table(name = "books")
public class Book {
    public Long id;
    public String title;
    public String author;
    public String isbn;
    public String description;
    
    @Column(name = "cover_image")
    public String coverImage;
    
    @Column(name = "created_at")
    public Timestamp createdAt;
    
    @Column(name = "updated_at")
    public Timestamp updatedAt;
}
