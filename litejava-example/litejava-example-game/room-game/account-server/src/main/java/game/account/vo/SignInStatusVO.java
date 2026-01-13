package game.account.vo;

import game.account.entity.SignInConfig;
import java.util.List;

/**
 * 签到状态
 */
public class SignInStatusVO {
    public int signDay;
    public boolean todaySigned;
    public List<SignInConfig> rewards;
}

