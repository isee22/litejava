package notificationservice.service;

import common.BizException;
import common.Err;
import notificationservice.G;
import notificationservice.model.Notification;
import notificationservice.model.Template;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知服务
 */
public class NotificationService {
    
    public static Notification send(Long userId, String type, String title, String content, String target) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        notification.target = target;
        notification.status = Notification.STATUS_PENDING;
        
        G.notificationMapper.insert(notification);
        doSend(notification);
        
        return notification;
    }
    
    public static Notification sendByTemplate(Long userId, String templateCode, String target, Map<String, String> params) {
        Template template = G.templateMapper.findByCode(templateCode);
        if (template == null) BizException.error(Err.TEMPLATE_NOT_FOUND, "模板不存在：" + templateCode);
        
        String title = renderTemplate(template.titleTemplate, params);
        String content = renderTemplate(template.contentTemplate, params);
        
        return send(userId, template.type, title, content, target);
    }
    
    public static int retryFailed(int limit) {
        List<Notification> failed = G.notificationMapper.findByStatus(Notification.STATUS_FAILED, limit);
        int count = 0;
        for (Notification notification : failed) {
            notification.status = Notification.STATUS_PENDING;
            doSend(notification);
            count++;
        }
        return count;
    }
    
    public static List<Notification> findByUserId(Long userId) {
        return G.notificationMapper.findByUserId(userId);
    }
    
    public static List<Template> findAllTemplates() {
        return G.templateMapper.findAll();
    }
    
    private static void doSend(Notification notification) {
        notification.status = Notification.STATUS_SENDING;
        G.notificationMapper.updateStatus(notification);
        
        try {
            Thread.sleep(100);
            notification.status = Notification.STATUS_SUCCESS;
            notification.errorMsg = null;
        } catch (Exception e) {
            notification.status = Notification.STATUS_FAILED;
            notification.errorMsg = e.getMessage();
        }
        
        G.notificationMapper.updateStatus(notification);
    }
    
    private static String renderTemplate(String template, Map<String, String> params) {
        if (template == null || params == null) {
            return template;
        }
        
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = params.getOrDefault(key, "");
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}
