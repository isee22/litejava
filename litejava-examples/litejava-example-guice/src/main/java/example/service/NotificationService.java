package example.service;

/**
 * 通知服务接口 - 演示 @Named 多实现绑定
 */
public interface NotificationService {
    
    void send(String to, String message);
}
