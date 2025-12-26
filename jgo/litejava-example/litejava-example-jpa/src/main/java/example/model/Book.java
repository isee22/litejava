package example.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * 图书实体
 */
@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
