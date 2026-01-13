package game.common;

/**
 * 通信包结构: cmd + code + data
 */
public class VO {
    public int cmd;
    public int code;
    public Object data;
    
    public static VO ok(int cmd, Object data) {
        VO p = new VO();
        p.cmd = cmd;
        p.code = 0;
        p.data = data;
        return p;
    }
    
    public static VO fail(int cmd, int code) {
        VO p = new VO();
        p.cmd = cmd;
        p.code = code;
        return p;
    }
    
    public static VO of(int cmd) {
        VO p = new VO();
        p.cmd = cmd;
        p.code = 0;
        return p;
    }
}
