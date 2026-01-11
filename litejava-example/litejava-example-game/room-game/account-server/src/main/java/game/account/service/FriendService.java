package game.account.service;

import game.account.AccountException;
import game.account.DB;
import game.account.entity.FriendEntity;
import game.account.entity.FriendRequestEntity;
import game.account.entity.PlayerEntity;
import game.account.mapper.FriendMapper;
import game.account.mapper.FriendRequestMapper;
import game.account.mapper.PlayerMapper;
import game.account.vo.FriendRequestVO;
import game.account.vo.FriendVO;

import java.util.*;

/**
 * 好友服务
 */
public class FriendService {
    
    public static final int MAX_FRIENDS = 100;
    
    /**
     * 获取好友列表
     */
    public static List<FriendVO> getFriends(long userId) {
        List<FriendEntity> friends = DB.execute(FriendMapper.class, m -> m.findByUserId(userId));
        List<FriendVO> result = new ArrayList<>();
        
        for (FriendEntity f : friends) {
            PlayerEntity player = DB.execute(PlayerMapper.class, m -> m.findById(f.friendId));
            if (player == null) continue;
            
            FriendVO vo = new FriendVO();
            vo.userId = player.userId;
            vo.name = player.name;
            vo.avatar = player.avatar;
            vo.level = player.level;
            vo.vipLevel = player.vipLevel;
            vo.addTime = f.createTime;
            result.add(vo);
        }
        return result;
    }
    
    /**
     * 获取待处理的好友请求
     */
    public static List<FriendRequestVO> getPendingRequests(long userId) {
        List<FriendRequestEntity> requests = DB.execute(FriendRequestMapper.class, m -> m.findPendingByToId(userId));
        List<FriendRequestVO> result = new ArrayList<>();
        
        for (FriendRequestEntity r : requests) {
            PlayerEntity player = DB.execute(PlayerMapper.class, m -> m.findById(r.fromId));
            if (player == null) continue;
            
            FriendRequestVO vo = new FriendRequestVO();
            vo.requestId = r.id;
            vo.userId = player.userId;
            vo.name = player.name;
            vo.avatar = player.avatar;
            vo.level = player.level;
            vo.message = r.message;
            vo.createTime = r.createTime;
            result.add(vo);
        }
        return result;
    }
    
    /**
     * 发送好友请求
     */
    public static void sendRequest(long fromId, long toId, String message) {
        if (fromId == toId) {
            AccountException.error(1, "不能添加自己为好友");
        }
        
        PlayerEntity target = DB.execute(PlayerMapper.class, m -> m.findById(toId));
        if (target == null) {
            AccountException.error(2, "玩家不存在");
        }
        
        FriendEntity existing = DB.execute(FriendMapper.class, m -> m.findByUserAndFriend(fromId, toId));
        if (existing != null) {
            AccountException.error(3, "已经是好友了");
        }
        
        FriendRequestEntity pending = DB.execute(FriendRequestMapper.class, m -> m.findPending(fromId, toId));
        if (pending != null) {
            AccountException.error(4, "已发送过请求，请等待对方处理");
        }
        
        // 对方已向我发送请求，直接同意
        FriendRequestEntity reverse = DB.execute(FriendRequestMapper.class, m -> m.findPending(toId, fromId));
        if (reverse != null) {
            acceptRequest(fromId, reverse.id);
            return;
        }
        
        long now = System.currentTimeMillis();
        FriendRequestEntity request = new FriendRequestEntity();
        request.fromId = fromId;
        request.toId = toId;
        request.message = message != null ? message : "";
        request.status = 0;
        request.createTime = now;
        request.updateTime = now;
        
        DB.execute(FriendRequestMapper.class, m -> {
            m.insert(request);
            return null;
        });
    }
    
    /**
     * 接受好友请求
     */
    public static void acceptRequest(long userId, long requestId) {
        FriendRequestEntity request = DB.execute(FriendRequestMapper.class, m -> m.findById(requestId));
        if (request == null) {
            AccountException.error(1, "请求不存在");
        }
        
        if (request.toId != userId) {
            AccountException.error(2, "无权操作");
        }
        
        if (request.status != 0) {
            AccountException.error(3, "请求已处理");
        }
        
        int count1 = DB.execute(FriendMapper.class, m -> m.countByUserId(userId));
        int count2 = DB.execute(FriendMapper.class, m -> m.countByUserId(request.fromId));
        if (count1 >= MAX_FRIENDS || count2 >= MAX_FRIENDS) {
            AccountException.error(4, "好友数量已达上限");
        }
        
        long now = System.currentTimeMillis();
        
        DB.execute(FriendRequestMapper.class, m -> {
            m.updateStatus(requestId, 1, now);
            return null;
        });
        
        FriendEntity f1 = new FriendEntity();
        f1.userId = userId;
        f1.friendId = request.fromId;
        f1.createTime = now;
        
        FriendEntity f2 = new FriendEntity();
        f2.userId = request.fromId;
        f2.friendId = userId;
        f2.createTime = now;
        
        DB.execute(FriendMapper.class, m -> {
            m.insert(f1);
            m.insert(f2);
            return null;
        });
    }
    
    /**
     * 拒绝好友请求
     */
    public static void rejectRequest(long userId, long requestId) {
        FriendRequestEntity request = DB.execute(FriendRequestMapper.class, m -> m.findById(requestId));
        if (request == null) {
            AccountException.error(1, "请求不存在");
        }
        
        if (request.toId != userId) {
            AccountException.error(2, "无权操作");
        }
        
        if (request.status != 0) {
            AccountException.error(3, "请求已处理");
        }
        
        DB.execute(FriendRequestMapper.class, m -> {
            m.updateStatus(requestId, 2, System.currentTimeMillis());
            return null;
        });
    }
    
    /**
     * 删除好友
     */
    public static void deleteFriend(long userId, long friendId) {
        FriendEntity existing = DB.execute(FriendMapper.class, m -> m.findByUserAndFriend(userId, friendId));
        if (existing == null) {
            AccountException.error(1, "不是好友关系");
        }
        
        DB.execute(FriendMapper.class, m -> {
            m.delete(userId, friendId);
            m.delete(friendId, userId);
            return null;
        });
    }
}
