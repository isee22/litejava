package game.hall.util;

import java.security.MessageDigest;

/**
 * 签名工具类
 */
public class SignUtil {
    
    private final String priKey;
    
    public SignUtil(String priKey) {
        this.priKey = priKey;
    }
    
    public String sign(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            sb.append(p);
        }
        sb.append(priKey);
        return md5(sb.toString());
    }
    
    public String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
    
    public static int toInt(Object o) {
        if (o == null) return -1;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return -1;
        }
    }
}
