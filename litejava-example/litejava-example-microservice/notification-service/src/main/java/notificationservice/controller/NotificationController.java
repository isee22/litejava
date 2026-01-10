package notificationservice.controller;

import common.BizException;
import common.vo.ListResult;
import litejava.Context;
import litejava.Routes;
import notificationservice.model.Notification;
import notificationservice.model.Template;
import notificationservice.service.NotificationService;

import java.util.List;
import java.util.Map;

public class NotificationController {
    
    public static Routes routes() {
        return new Routes()
            .post("/notification/send", NotificationController::send)
            .post("/notification/template", NotificationController::sendByTemplate)
            .post("/notification/templates", NotificationController::listTemplates)
            .post("/notification/user", NotificationController::listByUser)
            .post("/notification/retry", NotificationController::retry)
            .end();
    }
    
    static void send(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("userId") == null) BizException.paramRequired("userId");
        String type = (String) body.get("type");
        String content = (String) body.get("content");
        if (type == null || content == null) BizException.paramInvalid("type, content 不能为空");
        
        Long userId = ((Number) body.get("userId")).longValue();
        String title = (String) body.get("title");
        String target = (String) body.get("target");
        Notification notification = NotificationService.send(userId, type, title, content, target);
        ctx.ok(notification);
    }
    
    @SuppressWarnings("unchecked")
    static void sendByTemplate(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("userId") == null) BizException.paramRequired("userId");
        String templateCode = (String) body.get("templateCode");
        if (templateCode == null) BizException.paramRequired("templateCode");
        
        Long userId = ((Number) body.get("userId")).longValue();
        String target = (String) body.get("target");
        Map<String, String> params = (Map<String, String>) body.get("params");
        Notification notification = NotificationService.sendByTemplate(userId, templateCode, target, params);
        ctx.ok(notification);
    }
    
    static void listTemplates(Context ctx) {
        List<Template> templates = NotificationService.findAllTemplates();
        ctx.ok(ListResult.of(templates));
    }
    
    static void listByUser(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("userId") == null) BizException.paramRequired("userId");
        
        Long userId = ((Number) body.get("userId")).longValue();
        List<Notification> notifications = NotificationService.findByUserId(userId);
        ctx.ok(ListResult.of(notifications));
    }
    
    static void retry(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        int limit = body.get("limit") != null ? ((Number) body.get("limit")).intValue() : 100;
        int count = NotificationService.retryFailed(limit);
        ctx.ok(Map.of("retried", count));
    }
}
