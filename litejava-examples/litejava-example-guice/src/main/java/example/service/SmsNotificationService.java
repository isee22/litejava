package example.service;

import javax.inject.Singleton;

/**
 * 短信通知实现
 */
@Singleton
public class SmsNotificationService implements NotificationService {
    
    @Override
    public void send(String to, String message) {
        System.out.println("[SMS] To: " + to + ", Message: " + message);
    }
}
