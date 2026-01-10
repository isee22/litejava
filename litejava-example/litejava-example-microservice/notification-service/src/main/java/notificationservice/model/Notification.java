package notificationservice.model;

import java.time.LocalDateTime;

public class Notification {
    public Long id;
    public Long userId;
    public String type; // email/sms/push
    public String title;
    public String content;
    public String target; // 邮箱/手机号/设备ID
    public Integer status; // 0:待发送 1:发送中 2:成功 3:失败
    public String errorMsg;
    public LocalDateTime createdAt;
    public LocalDateTime sentAt;
    
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENDING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
}
