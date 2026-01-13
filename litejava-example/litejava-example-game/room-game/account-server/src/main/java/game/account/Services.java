package game.account;

import game.account.service.*;

/**
 * 服务定位器 (类似 Spring 的 Bean 容器)
 */
public class Services {

    public static PlayerService player;
    public static AuthService auth;
    public static FriendService friend;
    public static ItemService item;
    public static SignInService signIn;
    public static RankService rank;
    public static GiftService gift;
    public static ChatService chat;
    public static WorldChatService worldChat;
    public static TokenService token;
    public static ConfigService config;
    public static ReconnectService reconnect;

    public static void init() {
        player = new PlayerService();
        auth = new AuthService();
        friend = new FriendService();
        item = new ItemService();
        signIn = new SignInService();
        rank = new RankService();
        gift = new GiftService();
        chat = new ChatService();
        worldChat = new WorldChatService();
        token = new TokenService();
        config = new ConfigService();
        reconnect = new ReconnectService();
    }
}

