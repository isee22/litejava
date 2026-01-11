package game.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间管理器
 * 
 * 管理游戏房间的创建、销毁、玩家进出
 * 
 * 数据结构:
 * - rooms: roomId -> Room
 * - userLocations: userId -> [roomId, seatIndex]
 * 
 * 使用示例:
 * <pre>
 * RoomMgr<DoudizhuGame> roomMgr = new RoomMgr<>();
 * roomMgr.maxPlayers = 3;
 * 
 * // 创建房间
 * Room room = roomMgr.createRoom(userId, config);
 * 
 * // 进入房间
 * roomMgr.enterRoom(roomId, userId, "玩家名");
 * 
 * // 准备
 * roomMgr.setReady(userId, true);
 * if (roomMgr.isAllReady(room)) {
 *     // 开始游戏
 * }
 * </pre>
 * 
 * @param <G> 游戏状态类型
 */
public class RoomMgr<G> {
    
    /** 所有房间: roomId -> Room */
    private final Map<String, Room<G>> rooms = new ConcurrentHashMap<>();
    
    /** 用户位置: userId -> [roomId(int), seatIndex] */
    private final Map<Long, int[]> userLocations = new ConcurrentHashMap<>();
    
    /** 房间总数 */
    private int totalRooms = 0;
    
    /** 每个房间最大玩家数 */
    public int maxPlayers = 2;
    
    /**
     * 生成6位数字房间号
     */
    private String generateRoomId() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /**
     * 创建房间
     * 
     * @param creator 创建者ID（房主）
     * @param conf 房间配置，可以是 RoomConfVO 或 Map
     * @return 创建的房间，失败返回 null
     */
    public Room<G> createRoom(long creator, Object conf) {
        return createRoom(creator, conf, null);
    }
    
    /**
     * 创建房间 (支持预生成房间号)
     * 
     * @param creator 创建者ID（房主）
     * @param conf 房间配置
     * @param roomId 预生成的房间号，null 则自动生成
     * @return 创建的房间，失败返回 null
     */
    public Room<G> createRoom(long creator, Object conf, String roomId) {
        // 使用预生成的房间号或自动生成
        if (roomId == null || roomId.isEmpty()) {
            int retry = 0;
            do {
                roomId = generateRoomId();
                if (++retry > 100) return null;
            } while (rooms.containsKey(roomId));
        } else if (rooms.containsKey(roomId)) {
            return null;  // 房间号已存在
        }
        
        // 创建房间
        Room<G> room = new Room<>();
        room.id = roomId;
        room.ownerId = creator;
        room.conf = conf;
        room.createTime = System.currentTimeMillis();
        
        // 确定玩家数
        int players = maxPlayers;
        if (conf != null) {
            if (conf instanceof Map) {
                Map<String, Object> confMap = (Map<String, Object>) conf;
                if (confMap.containsKey("players")) {
                    players = ((Number) confMap.get("players")).intValue();
                }
            } else if (conf instanceof game.common.vo.RoomConfVO) {
                game.common.vo.RoomConfVO confVO = (game.common.vo.RoomConfVO) conf;
                if (confVO.players > 0) {
                    players = confVO.players;
                }
            }
        }
        
        // 初始化座位
        for (int i = 0; i < players; i++) {
            Seat seat = new Seat();
            seat.seatIndex = i;
            room.seats.add(seat);
        }
        
        rooms.put(roomId, room);
        totalRooms++;
        return room;
    }
    
    /**
     * 进入房间
     * 
     * @param roomId 房间ID
     * @param userId 用户ID
     * @param userName 用户名
     * @return 0=成功, 1=房间已满, 2=房间不存在
     */
    public int enterRoom(String roomId, long userId, String userName) {
        // 已在该房间
        if (roomId.equals(getUserRoom(userId))) return 0;
        
        Room<G> room = rooms.get(roomId);
        if (room == null) return 2;
        
        // 找空座位
        for (Seat seat : room.seats) {
            if (seat.userId == 0) {
                seat.userId = userId;
                seat.name = userName;
                seat.ready = false;
                seat.online = true;
                userLocations.put(userId, new int[]{Integer.parseInt(roomId), seat.seatIndex});
                return 0;
            }
        }
        return 1;  // 房间已满
    }
    
    /**
     * 退出房间
     * 
     * 如果房间空了会自动销毁
     */
    public void exitRoom(long userId) {
        int[] loc = userLocations.remove(userId);
        if (loc == null) return;
        
        String roomId = String.valueOf(loc[0]);
        Room<G> room = rooms.get(roomId);
        if (room == null) return;
        
        // 清空座位
        for (Seat seat : room.seats) {
            if (seat.userId == userId) {
                seat.userId = 0;
                seat.name = "";
                seat.ready = false;
                break;
            }
        }
        
        // 房间空了，销毁
        if (getPlayerCount(room) == 0) {
            destroy(roomId);
        }
    }
    
    /**
     * 销毁房间
     */
    public void destroy(String roomId) {
        Room<G> room = rooms.remove(roomId);
        if (room == null) return;
        
        // 清理所有玩家的位置记录
        for (Seat seat : room.seats) {
            if (seat.userId > 0) {
                userLocations.remove(seat.userId);
            }
        }
        totalRooms--;
    }
    
    /**
     * 获取用户所在房间ID
     * 
     * @return 房间ID，不在房间返回 null
     */
    public String getUserRoom(long userId) {
        int[] loc = userLocations.get(userId);
        return loc != null ? String.valueOf(loc[0]) : null;
    }
    
    /**
     * 获取用户座位号
     * 
     * @return 座位号，不在房间返回 -1
     */
    public int getUserSeat(long userId) {
        int[] loc = userLocations.get(userId);
        return loc != null ? loc[1] : -1;
    }
    
    /**
     * 获取房间
     */
    public Room<G> getRoom(String roomId) {
        return rooms.get(roomId);
    }
    
    /**
     * 设置准备状态
     */
    public void setReady(long userId, boolean ready) {
        String roomId = getUserRoom(userId);
        if (roomId == null) return;
        
        Room<G> room = getRoom(roomId);
        if (room == null) return;
        
        int seatIndex = getUserSeat(userId);
        if (seatIndex >= 0 && seatIndex < room.seats.size()) {
            room.seats.get(seatIndex).ready = ready;
        }
    }
    
    /**
     * 检查是否所有人都准备了
     * 
     * 条件: 所有座位都有人且都准备
     */
    public boolean isAllReady(Room<G> room) {
        int readyCount = 0;
        for (Seat seat : room.seats) {
            if (seat.userId > 0) {
                if (!seat.ready) return false;
                readyCount++;
            }
        }
        return readyCount == room.seats.size();
    }
    
    /**
     * 获取房间玩家数
     */
    public int getPlayerCount(Room<G> room) {
        int count = 0;
        for (Seat seat : room.seats) {
            if (seat.userId > 0) count++;
        }
        return count;
    }
    
    /**
     * 获取房间总数
     */
    public int getTotalRooms() { 
        return totalRooms; 
    }
    
    /**
     * 获取所有房间
     */
    public Collection<Room<G>> getAllRooms() { 
        return rooms.values(); 
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 房间
     * 
     * @param <G> 游戏状态类型
     */
    public static class Room<G> {
        /** 房间ID (6位数字) */
        public String id;
        
        /** 房主ID */
        public long ownerId;
        
        /** 房间配置 */
        public Object conf;
        
        /** 创建时间 */
        public long createTime;
        
        /** 已进行的游戏局数 */
        public int numOfGames;
        
        /** 座位列表 */
        public List<Seat> seats = new ArrayList<>();
        
        /** 游戏状态对象，null 表示未开始 */
        public G game;
    }
    
    /**
     * 座位
     */
    public static class Seat {
        /** 座位号 (0开始) */
        public int seatIndex;
        
        /** 玩家ID (0表示空座) */
        public long userId;
        
        /** 玩家名 */
        public String name;
        
        /** 是否准备 */
        public boolean ready;
        
        /** 是否在线 */
        public boolean online = true;
        
        /** 断线时间 (毫秒时间戳，0表示在线) */
        public long disconnectTime;
        
        /** 是否托管中 */
        public boolean trusteeship;
    }
}
