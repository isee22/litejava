package litejava.plugins.security;

import litejava.Plugin;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 插件 - JSON Web Token 认证
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * jwt.secret=your-secret-key-at-least-32-chars
 * jwt.expireMs=3600000
 * }</pre>
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.jsonwebtoken</groupId>
 *     <artifactId>jjwt-api</artifactId>
 *     <version>0.11.5</version>
 * </dependency>
 * <dependency>
 *     <groupId>io.jsonwebtoken</groupId>
 *     <artifactId>jjwt-impl</artifactId>
 *     <version>0.11.5</version>
 * </dependency>
 * <dependency>
 *     <groupId>io.jsonwebtoken</groupId>
 *     <artifactId>jjwt-jackson</artifactId>
 *     <version>0.11.5</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 自动生成密钥
 * app.use(new JwtPlugin());
 * 
 * // 指定密钥（至少32字符）
 * app.use(new JwtPlugin("your-secret-key-at-least-32-chars"));
 * 
 * // 生成 token
 * String token = JwtPlugin.instance.sign(Map.of("userId", 123, "role", "admin"));
 * 
 * // 验证 token
 * Claims claims = JwtPlugin.instance.verify(token);
 * int userId = claims.get("userId", Integer.class);
 * }</pre>
 */
public class JwtPlugin extends Plugin {
    
    public static JwtPlugin instance;
    private Key key;
    public long expireMs = 3600000; // 1小时
    
    public JwtPlugin() {
        instance = this;
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }
    
    public JwtPlugin(String secret) {
        instance = this;
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    @Override
    public void config() {
        // 从配置加载
        String secret = app.conf.getString("jwt", "secret", null);
        if (secret != null && !secret.isEmpty()) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes());
        }
        expireMs = app.conf.getLong("jwt", "expireMs", expireMs);
    }
    
    public String sign(Map<String, Object> claims) {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expireMs))
            .signWith(key)
            .compact();
    }
    
    public Claims verify(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
