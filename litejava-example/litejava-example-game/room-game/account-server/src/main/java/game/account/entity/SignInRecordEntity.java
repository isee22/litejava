package game.account.entity;

/**
 * 玩家签到记录
 */
public class SignInRecordEntity {
    public long userId;
    public int signDay;           // 当前签到天数 (1-7循环)
    public long lastSignTime;     // 上次签到时间
}
