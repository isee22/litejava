package game.account.dao;

import game.account.DB;
import game.account.entity.Friend;
import game.account.entity.FriendRequest;
import game.account.mapper.FriendMapper;
import game.account.mapper.FriendRequestMapper;

import java.util.List;

/**
 * 好友 DAO
 */
public class FriendDao {

    // ========== 好友关系 ==========

    public List<Friend> findByUserId(long userId) {
        return DB.execute(FriendMapper.class, m -> m.findByUserId(userId));
    }

    public Friend findByUserAndFriend(long userId, long friendId) {
        return DB.execute(FriendMapper.class, m -> m.findByUserAndFriend(userId, friendId));
    }

    public int countByUserId(long userId) {
        return DB.execute(FriendMapper.class, m -> m.countByUserId(userId));
    }

    public void insertFriend(Friend friend) {
        DB.execute(FriendMapper.class, m -> {
            m.insert(friend);
            return null;
        });
    }

    public void deleteFriend(long userId, long friendId) {
        DB.execute(FriendMapper.class, m -> {
            m.delete(userId, friendId);
            return null;
        });
    }

    // ========== 好友请求 ==========

    public FriendRequest findRequestById(long id) {
        return DB.execute(FriendRequestMapper.class, m -> m.findById(id));
    }

    public List<FriendRequest> findPendingRequests(long toId) {
        return DB.execute(FriendRequestMapper.class, m -> m.findPendingByToId(toId));
    }

    public FriendRequest findPendingRequest(long fromId, long toId) {
        return DB.execute(FriendRequestMapper.class, m -> m.findPending(fromId, toId));
    }

    public void insertRequest(FriendRequest request) {
        DB.execute(FriendRequestMapper.class, m -> {
            m.insert(request);
            return null;
        });
    }

    public void updateRequestStatus(long id, int status, long updateTime) {
        DB.execute(FriendRequestMapper.class, m -> {
            m.updateStatus(id, status, updateTime);
            return null;
        });
    }
}

