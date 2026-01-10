package common;

/**
 * 统一错误码定义
 * 
 * 规则：
 * - 5位数字：XXYYY
 * - XX: 模块编号 (40=通用, 41=用户, 42=订单, 43=商品, 44=支付, 45=通知)
 * - YYY: 具体错误 (001-999)
 * 
 * 前端 i18n 根据 code 查本地翻译，msg 仅供服务端调试
 */
public class Err {
    
    // ==================== 通用错误 400xx ====================
    public static final int PARAM_REQUIRED = 40001;    // 参数不能为空
    public static final int PARAM_INVALID = 40002;     // 参数格式错误
    public static final int UNAUTHORIZED = 40003;      // 未登录
    public static final int FORBIDDEN = 40004;         // 无权限
    public static final int NOT_FOUND = 40005;         // 资源不存在
    public static final int SERVER_ERROR = 40006;      // 服务器错误
    
    // ==================== 用户模块 410xx ====================
    public static final int USER_NOT_FOUND = 41001;    // 用户不存在
    public static final int USER_EXISTS = 41002;       // 用户已存在
    public static final int USER_DISABLED = 41003;     // 用户已禁用
    public static final int PASSWORD_ERROR = 41004;    // 密码错误
    public static final int TOKEN_EXPIRED = 41005;     // Token 过期
    
    // ==================== 订单模块 420xx ====================
    public static final int ORDER_NOT_FOUND = 42001;   // 订单不存在
    public static final int ORDER_STATUS_ERROR = 42002; // 订单状态错误
    public static final int ORDER_ITEM_EMPTY = 42003;  // 订单项为空
    public static final int ORDER_CANCEL_FAILED = 42004; // 取消订单失败
    
    // ==================== 商品模块 430xx ====================
    public static final int PRODUCT_NOT_FOUND = 43001; // 商品不存在
    public static final int STOCK_NOT_ENOUGH = 43002;  // 库存不足
    public static final int PRODUCT_OFF_SHELF = 43003; // 商品已下架
    public static final int CATEGORY_NOT_FOUND = 43004; // 分类不存在
    
    // ==================== 支付模块 440xx ====================
    public static final int PAYMENT_NOT_FOUND = 44001; // 支付单不存在
    public static final int PAYMENT_FAILED = 44002;    // 支付失败
    public static final int PAYMENT_EXPIRED = 44003;   // 支付已过期
    public static final int REFUND_FAILED = 44004;     // 退款失败
    public static final int REFUND_AMOUNT_ERROR = 44005; // 退款金额错误
    
    // ==================== 通知模块 450xx ====================
    public static final int TEMPLATE_NOT_FOUND = 45001; // 模板不存在
    public static final int SEND_FAILED = 45002;       // 发送失败
    public static final int TARGET_INVALID = 45003;    // 目标地址无效
}
