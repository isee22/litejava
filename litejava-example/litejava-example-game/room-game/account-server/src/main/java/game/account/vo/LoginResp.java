package game.account.vo;

import game.account.entity.GameConfig;
import game.account.entity.PlayerItem;
import java.util.List;

public class LoginResp {
    public long userId;
    public String username;
    public String name;
    public long coins;
    public long diamonds;
    public List<GameConfig> roomConfigs;  // 房间配置列表
    public List<PlayerItem> items;  // 道具列表
    
    // 断线重连信息 (BabyKylin: roomid)
    public String roomId;      // 如果在房间中，返回房间号
    public String serverId;    // GameServer ID (ip:wsPort)
    public String serverIp;    // GameServer IP
    public int serverPort;     // GameServer WebSocket 端口
    public String gameType;    // 游戏类型
}

