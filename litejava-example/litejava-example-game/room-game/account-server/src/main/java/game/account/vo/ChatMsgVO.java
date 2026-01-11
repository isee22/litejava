package game.account.vo;

import java.util.List;

/**
 * 聊天消息 VO
 */
public class ChatMsgVO {
    public long userId;
    public String name;
    public String content;
    public String roomId;
    public long toId;
    public long time;
    public boolean isPrivate;
    /** 需要接收此消息的用户列表 */
    public List<Long> recipients;
}
