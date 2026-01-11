package game.common.vo;

/**
 * 被踢通知
 */
public class KickResultVO {
    public boolean kicked;
    
    public static KickResultVO kicked() {
        KickResultVO vo = new KickResultVO();
        vo.kicked = true;
        return vo;
    }
}
