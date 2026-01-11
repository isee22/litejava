package game.account.entity;

/**
 * 好友请求实体
 * 
 * status: 0=待处理, 1=已接受, 2=已拒绝
 */
public class FriendRequestEntity {
    public long id;
    public long fromId;
    public long toId;
    public String message;
    public int status;
    public long createTime;
    public long updateTime;
}
