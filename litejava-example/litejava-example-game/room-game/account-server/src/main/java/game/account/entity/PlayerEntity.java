package game.account.entity;

public class PlayerEntity {
    public long userId;
    public String name;
    public String avatar;
    public int sex;        // 0=未知, 1=男, 2=女
    public int coins;
    public int diamonds;
    public int level;
    public int exp;
    public int vipLevel;
    public int vipExp;
    public int totalGames;
    public int winGames;
    public int loginDays;
    public long createTime;
    public long lastLoginTime;
}
