package game.common.vo;

/**
 * 健康检查响应
 */
public class HealthVO {
    public String status;
    
    public static HealthVO up() {
        HealthVO vo = new HealthVO();
        vo.status = "UP";
        return vo;
    }
}
