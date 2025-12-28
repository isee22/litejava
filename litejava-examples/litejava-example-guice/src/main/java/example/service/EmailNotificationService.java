package example.service;

import javax.inject.Singleton;

/**
 * 邮件通知实现
 */
@Singleton
public class EmailNotificationService implements NotificationService {
    
    @Override
    public void send(String to, String message) {
        System.out.println("[EMAIL] To: " + to + ", Message: " + message);
    }
}
