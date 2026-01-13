package game.account.service;

import game.common.GameException;
import game.account.dao.FriendDao;
import game.account.dao.PlayerDao;
import game.account.entity.Friend;
import game.account.entity.FriendRequest;
import game.account.entity.Player;
import game.account.vo.FriendRequestVO;
import game.account.vo.FriendVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友服务
 */
public class FriendService {

    public static final int MAX_FRIENDS = 100;

    private final FriendDao friendDao = new FriendDao();
    private final PlayerDao playerDao = new PlayerDao();

    public List<FriendVO> getFriends(long userId) {
        List<Friend> friends = friendDao.findByUserId(userId);
        List<FriendVO> result = new ArrayList<>();

        for (Friend f : friends) {
            Player player = playerDao.findById(f.friendId);
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

    public List<FriendRequestVO> getPendingRequests(long userId) {
        List<FriendRequest> requests = friendDao.findPendingRequests(userId);
        List<FriendRequestVO> result = new ArrayList<>();

        for (FriendRequest r : requests) {
            Player player = playerDao.findById(r.fromId);
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

    public void sendRequest(long fromId, long toId, String message) {
        if (fromId == toId) {
            GameException.error(1, "不能添加自己为好友");
        }

        Player target = playerDao.findById(toId);
        if (target == null) {
            GameException.error(2, "玩家不存在");
        }

        Friend existing = friendDao.findByUserAndFriend(fromId, toId);
        if (existing != null) {
            GameException.error(3, "已经是好友了");
        }

        FriendRequest pending = friendDao.findPendingRequest(fromId, toId);
        if (pending != null) {
            GameException.error(4, "已发送过请求，请等待对方处理");
        }

        FriendRequest reverse = friendDao.findPendingRequest(toId, fromId);
        if (reverse != null) {
            acceptRequest(fromId, reverse.id);
            return;
        }

        long now = System.currentTimeMillis();
        FriendRequest request = new FriendRequest();
        request.fromId = fromId;
        request.toId = toId;
        request.message = message != null ? message : "";
        request.status = 0;
        request.createTime = now;
        request.updateTime = now;

        friendDao.insertRequest(request);
    }

    public void acceptRequest(long userId, long requestId) {
        FriendRequest request = friendDao.findRequestById(requestId);
        if (request == null) {
            GameException.error(1, "请求不存在");
        }
        if (request.toId != userId) {
            GameException.error(2, "无权操作");
        }
        if (request.status != 0) {
            GameException.error(3, "请求已处理");
        }

        int count1 = friendDao.countByUserId(userId);
        int count2 = friendDao.countByUserId(request.fromId);
        if (count1 >= MAX_FRIENDS || count2 >= MAX_FRIENDS) {
            GameException.error(4, "好友数量已达上限");
        }

        long now = System.currentTimeMillis();
        friendDao.updateRequestStatus(requestId, 1, now);

        Friend f1 = new Friend();
        f1.userId = userId;
        f1.friendId = request.fromId;
        f1.createTime = now;

        Friend f2 = new Friend();
        f2.userId = request.fromId;
        f2.friendId = userId;
        f2.createTime = now;

        friendDao.insertFriend(f1);
        friendDao.insertFriend(f2);
    }

    public void rejectRequest(long userId, long requestId) {
        FriendRequest request = friendDao.findRequestById(requestId);
        if (request == null) {
            GameException.error(1, "请求不存在");
        }
        if (request.toId != userId) {
            GameException.error(2, "无权操作");
        }
        if (request.status != 0) {
            GameException.error(3, "请求已处理");
        }

        friendDao.updateRequestStatus(requestId, 2, System.currentTimeMillis());
    }

    public void deleteFriend(long userId, long friendId) {
        Friend existing = friendDao.findByUserAndFriend(userId, friendId);
        if (existing == null) {
            GameException.error(1, "不是好友关系");
        }

        friendDao.deleteFriend(userId, friendId);
        friendDao.deleteFriend(friendId, userId);
    }
}

