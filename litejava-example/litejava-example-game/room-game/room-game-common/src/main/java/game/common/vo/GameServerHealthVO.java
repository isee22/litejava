package game.common.vo;

/**
 * 游戏服务器健康检查
 */
public class GameServerHealthVO {
    public String status;
    public String serverId;
    
    public static GameServerHealthVO up(String serverId) {
        GameServerHealthVO vo = new GameServerHealthVO();
        vo.status = "UP";
        vo.serverId = serverId;
        return vo;
    }
}
