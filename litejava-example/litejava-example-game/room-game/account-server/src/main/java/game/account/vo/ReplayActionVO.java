package game.account.vo;

import java.util.Map;

public class ReplayActionVO {
    public int frame;
    public long userId;
    public int cmd;
    public Map<String, Object> data;
    public long timestamp;
}

