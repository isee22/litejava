package authservice.service;

import authservice.G;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 服务
 */
public class JwtService {
    
    public SecretKey key;
    public int expireHours;
    
    public void init() {
        String secret = G.app.conf.getString("jwt", "secret", "default-secret-key-must-be-32-chars");
        expireHours = G.app.conf.getInt("jwt", "expireHours", 24);
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600 * 1000L);
        
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
    
    public Map<String, Object> validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return Map.of(
                "userId", Long.parseLong(claims.getSubject()),
                "username", claims.get("username", String.class),
                "valid", true
            );
        } catch (ExpiredJwtException e) {
            return Map.of("valid", false, "error", "Token expired");
        } catch (Exception e) {
            return Map.of("valid", false, "error", "Invalid token");
        }
    }
    
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
