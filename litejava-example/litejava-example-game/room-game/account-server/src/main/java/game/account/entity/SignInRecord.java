package game.account.entity;

import javax.persistence.*;

/**
 * 玩家签到记录
 */
@Entity
@Table(name = "sign_in_record")
public class SignInRecord {
    @Id
    @Column(name = "userId")
    public long userId;
    
    @Column(name = "signDay")
    public int signDay;           // 当前签到天数 (1-7循环)
    
    @Column(name = "lastSignTime")
    public long lastSignTime;     // 上次签到时间
}
