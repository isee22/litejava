package game.account.vo;

import java.util.Map;

public class CreateRoomReq {
    public long userId;
    public String gameType;
    public Map<String, Object> conf;  // 房间配置 (可选)
}

