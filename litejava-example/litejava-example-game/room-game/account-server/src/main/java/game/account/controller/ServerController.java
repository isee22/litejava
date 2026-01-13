package game.account.controller;

import game.account.vo.ServerInfoVO;
import game.account.vo.VersionVO;
import litejava.App;

/**
 * 服务器信息控制器
 * 
 * 迁移自 account_server.js: /get_version, /get_serverinfo
 */
public class ServerController {
    
    public static void register(App app) {
        // 获取版本
        app.get("/get_version", ctx -> {
            String version = app.conf.getString("app", "version", "1.0.0");
            VersionVO vo = new VersionVO();
            vo.version = version;
            ctx.ok(vo);
        });
        
        // 获取服务器信息
        app.get("/get_serverinfo", ctx -> {
            String version = app.conf.getString("app", "version", "1.0.0");
            String hallIp = app.conf.getString("hall", "ip", "localhost");
            int hallPort = app.conf.getInt("hall", "port", 7100);
            String appweb = app.conf.getString("app", "web", "");
            
            ServerInfoVO vo = new ServerInfoVO();
            vo.version = version;
            vo.hall = hallIp + ":" + hallPort;
            vo.appweb = appweb;
            ctx.ok(vo);
        });
    }
}

