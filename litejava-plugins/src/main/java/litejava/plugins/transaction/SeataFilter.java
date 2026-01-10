package litejava.plugins.transaction;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

/**
 * Seata 中间件 - 自动传递/绑定 XID
 * 
 * 功能：
 * 1. 从请求头获取 X-Seata-Xid 并绑定到当前线程
 * 2. 响应时清理 XID
 */
public class SeataFilter extends MiddlewarePlugin {
    
    public static final String XID_HEADER = "X-Seata-Xid";
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String xid = ctx.header(XID_HEADER);
        
        if (xid != null && !xid.isEmpty()) {
            // 绑定上游传递的 XID
            SeataPlugin.bindXid(xid);
            ctx.state.put("seataXid", xid);
        }
        
        try {
            next.run();
        } finally {
            // 清理
            if (xid != null) {
                // ThreadLocal 会在请求结束后自动清理
            }
        }
    }
    
    /**
     * 获取当前 XID（用于传递给下游服务）
     */
    public static String getXid(Context ctx) {
        Object xid = ctx.state.get("seataXid");
        if (xid != null) return xid.toString();
        return SeataPlugin.getCurrentXid();
    }
}
