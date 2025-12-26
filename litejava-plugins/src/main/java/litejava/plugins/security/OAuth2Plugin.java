package litejava.plugins.security;

import litejava.Context;
import litejava.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * OAuth2 认证插件
 * 
 * <p>支持 OAuth2 授权码模式，可集成 GitHub、Google、微信等第三方登录。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * OAuth2Plugin oauth = new OAuth2Plugin()
 *     .provider("github", cfg -> {
 *         cfg.clientId = "your-client-id";
 *         cfg.clientSecret = "your-client-secret";
 *     })
 *     .onSuccess((ctx, user) -> {
 *         ctx.state.put("user", user);
 *         ctx.redirect("/dashboard");
 *     });
 * 
 * app.use(oauth);
 * }</pre>
 */
public class OAuth2Plugin extends Plugin {
    
    /** Provider 配置 */
    public Map<String, ProviderConfig> providers = new ConcurrentHashMap<>();
    
    /** 登录成功回调 */
    public BiConsumer<Context, OAuth2User> onSuccess;
    
    /** 登录失败回调 */
    public BiConsumer<Context, String> onError;
    
    /** 路由前缀 */
    public String prefix = "/oauth2";
    
    /** State 存储 */
    private Map<String, String> stateStore = new ConcurrentHashMap<>();
    
    public static class ProviderConfig {
        public String clientId;
        public String clientSecret;
        public String authorizeUrl;
        public String tokenUrl;
        public String userInfoUrl;
        public String scopes = "";
        public String redirectUri;
    }
    
    public static class OAuth2User {
        public String provider;
        public String id;
        public String name;
        public String email;
        public String avatar;
        public String accessToken;
        public Map<String, Object> raw;
    }
    
    public OAuth2Plugin provider(String name, Consumer<ProviderConfig> configurer) {
        ProviderConfig config = new ProviderConfig();
        configurer.accept(config);
        providers.put(name, config);
        return this;
    }
    
    public OAuth2Plugin github(String clientId, String clientSecret) {
        return provider("github", cfg -> {
            cfg.clientId = clientId;
            cfg.clientSecret = clientSecret;
            cfg.authorizeUrl = "https://github.com/login/oauth/authorize";
            cfg.tokenUrl = "https://github.com/login/oauth/access_token";
            cfg.userInfoUrl = "https://api.github.com/user";
            cfg.scopes = "user:email";
        });
    }
    
    public OAuth2Plugin google(String clientId, String clientSecret) {
        return provider("google", cfg -> {
            cfg.clientId = clientId;
            cfg.clientSecret = clientSecret;
            cfg.authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth";
            cfg.tokenUrl = "https://oauth2.googleapis.com/token";
            cfg.userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            cfg.scopes = "openid email profile";
        });
    }
    
    public OAuth2Plugin onSuccess(BiConsumer<Context, OAuth2User> handler) {
        this.onSuccess = handler;
        return this;
    }
    
    public OAuth2Plugin onError(BiConsumer<Context, String> handler) {
        this.onError = handler;
        return this;
    }
    
    @Override
    public void config() {
        for (String providerName : providers.keySet()) {
            app.get(prefix + "/" + providerName + "/login", ctx -> handleLogin(ctx, providerName));
            app.get(prefix + "/" + providerName + "/callback", ctx -> handleCallback(ctx, providerName));
        }
    }
    
    private void handleLogin(Context ctx, String providerName) {
        ProviderConfig config = providers.get(providerName);
        if (config == null) {
            ctx.status(404).json(Map.of("error", "Provider not found"));
            return;
        }
        
        String state = generateState();
        stateStore.put(state, providerName);
        
        String redirectUri = config.redirectUri;
        if (redirectUri == null || redirectUri.isEmpty()) {
            redirectUri = getBaseUrl(ctx) + prefix + "/" + providerName + "/callback";
        }
        
        StringBuilder url = new StringBuilder(config.authorizeUrl);
        url.append("?client_id=").append(encode(config.clientId));
        url.append("&redirect_uri=").append(encode(redirectUri));
        url.append("&response_type=code");
        url.append("&state=").append(encode(state));
        if (config.scopes != null && !config.scopes.isEmpty()) {
            url.append("&scope=").append(encode(config.scopes));
        }
        
        ctx.redirect(url.toString());
    }
    
    private void handleCallback(Context ctx, String providerName) {
        String code = ctx.queryParam("code");
        String state = ctx.queryParam("state");
        String error = ctx.queryParam("error");
        
        if (error != null) {
            handleError(ctx, "OAuth2 error: " + error);
            return;
        }
        
        if (state == null || !stateStore.containsKey(state)) {
            handleError(ctx, "Invalid state");
            return;
        }
        stateStore.remove(state);
        
        ProviderConfig config = providers.get(providerName);
        if (config == null) {
            handleError(ctx, "Provider not found");
            return;
        }
        
        try {
            String redirectUri = config.redirectUri;
            if (redirectUri == null || redirectUri.isEmpty()) {
                redirectUri = getBaseUrl(ctx) + prefix + "/" + providerName + "/callback";
            }
            
            String tokenResponse = exchangeToken(config, code, redirectUri);
            String accessToken = parseAccessToken(tokenResponse);
            
            if (accessToken == null) {
                handleError(ctx, "Failed to get access token");
                return;
            }
            
            OAuth2User user = fetchUserInfo(config, accessToken, providerName);
            user.provider = providerName;
            user.accessToken = accessToken;
            
            if (onSuccess != null) {
                onSuccess.accept(ctx, user);
            } else {
                ctx.json(Map.of("user", user));
            }
            
        } catch (Exception e) {
            handleError(ctx, "OAuth2 error: " + e.getMessage());
        }
    }
    
    private void handleError(Context ctx, String message) {
        if (onError != null) {
            onError.accept(ctx, message);
        } else {
            ctx.status(400).json(Map.of("error", message));
        }
    }
    
    private String exchangeToken(ProviderConfig config, String code, String redirectUri) throws Exception {
        String body = "client_id=" + encode(config.clientId)
                + "&client_secret=" + encode(config.clientSecret)
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";
        
        URL url = new URL(config.tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    private String parseAccessToken(String response) {
        if (response.contains("\"access_token\"")) {
            int start = response.indexOf("\"access_token\"") + 16;
            int end = response.indexOf("\"", start);
            if (end > start) {
                return response.substring(start, end);
            }
        }
        if (response.contains("access_token=")) {
            int start = response.indexOf("access_token=") + 13;
            int end = response.indexOf("&", start);
            if (end < 0) end = response.length();
            return response.substring(start, end);
        }
        return null;
    }
    
    private OAuth2User fetchUserInfo(ProviderConfig config, String accessToken, String provider) throws Exception {
        URL url = new URL(config.userInfoUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            
            OAuth2User user = new OAuth2User();
            user.raw = parseJson(body);
            
            switch (provider) {
                case "github":
                    user.id = getString(user.raw, "id");
                    user.name = getString(user.raw, "login");
                    user.email = getString(user.raw, "email");
                    user.avatar = getString(user.raw, "avatar_url");
                    break;
                case "google":
                    user.id = getString(user.raw, "id");
                    user.name = getString(user.raw, "name");
                    user.email = getString(user.raw, "email");
                    user.avatar = getString(user.raw, "picture");
                    break;
                default:
                    user.id = getString(user.raw, "id");
                    user.name = getString(user.raw, "name");
                    user.email = getString(user.raw, "email");
            }
            
            return user;
        }
    }
    
    private String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String getBaseUrl(Context ctx) {
        String host = ctx.header("Host");
        String proto = ctx.header("X-Forwarded-Proto");
        if (proto == null) proto = "http";
        return proto + "://" + host;
    }
    
    private String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
    
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> map = new ConcurrentHashMap<>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            for (String pair : json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("^\"|\"$", "");
                    String value = kv[1].trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        map.put(key, value.substring(1, value.length() - 1));
                    } else if (value.equals("null")) {
                        map.put(key, null);
                    } else {
                        try {
                            map.put(key, Long.parseLong(value));
                        } catch (NumberFormatException e) {
                            map.put(key, value);
                        }
                    }
                }
            }
        }
        return map;
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
