package productservice.model;

import java.time.LocalDateTime;

public class Category {
    public Long id;
    public String name;
    public Long parentId;
    public Integer sortOrder;
    public LocalDateTime createdAt;
}
