package game.hall.vo;

/**
 * 玩家信息 (从 Redis 读取，与 AccountServer 的 Player 缓存对应)
 */
public class PlayerInfo {
    public long userId;
    public String name;
    public String avatar;
    public int sex;
    public int coins;
    public int level;
    public int vipLevel;
}
