package notificationservice.model;

import java.time.LocalDateTime;

public class Template {
    public Long id;
    public String code;
    public String name;
    public String type;
    public String titleTemplate;
    public String contentTemplate;
    public LocalDateTime createdAt;
}
