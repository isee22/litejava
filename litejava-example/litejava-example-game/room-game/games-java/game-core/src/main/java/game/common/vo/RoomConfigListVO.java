package game.common.vo;

import java.util.List;

/**
 * 房间配置列表响应
 * 
 * 用于解析 HTTP 响应 {code, msg, data}
 */
public class RoomConfigListVO {
    public int code;
    public String msg;
    public List<RoomConfigVO> data;
}
