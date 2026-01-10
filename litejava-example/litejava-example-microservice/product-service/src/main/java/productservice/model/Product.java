package productservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    public Long id;
    public String name;
    public Long categoryId;
    public BigDecimal price;
    public Integer stock;
    public String description;
    public String imageUrl;
    public Integer status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    
    // 关联
    public Category category;
}
