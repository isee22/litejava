package game.account.entity;

/**
 * 玩家道具表
 */
public class PlayerItemEntity {
    public long id;
    public long userId;
    public int itemId;
    public int count;
    public long expireTime;   // 过期时间戳, 0表示永久
    public long createTime;
    public long updateTime;
}
