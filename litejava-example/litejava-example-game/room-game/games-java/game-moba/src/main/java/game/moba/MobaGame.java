package game.moba;

import java.util.*;
import java.util.concurrent.*;

/**
 * MOBA 游戏逻辑 (简化版 5v5)
 * 
 * 特点：
 * - 帧同步：每秒 15 帧 tick
 * - 实时移动、技能、攻击
 * - 简化地图：一条路 + 两个基地
 */
public class MobaGame {
    
    // 游戏配置
    public static final int TICK_RATE = 15;  // 每秒帧数
    public static final int MAP_WIDTH = 2000;
    public static final int MAP_HEIGHT = 1000;
    
    // 游戏状态
    public boolean isOver;
    public int winner = -1;  // 0=蓝方, 1=红方
    public int frameId;
    
    // 玩家 (userId -> Hero)
    public Map<Long, Hero> heroes = new ConcurrentHashMap<>();
    
    // 队伍 (0=蓝方, 1=红方)
    public List<Long> blueTeam = new ArrayList<>();
    public List<Long> redTeam = new ArrayList<>();
    
    // 基地
    public Base blueBase = new Base(0, 100, MAP_HEIGHT / 2, 5000);
    public Base redBase = new Base(1, MAP_WIDTH - 100, MAP_HEIGHT / 2, 5000);
    
    // 游戏循环
    private ScheduledExecutorService scheduler;
    private Runnable onTick;
    
    public MobaGame(long[] bluePlayers, long[] redPlayers) {
        // 初始化蓝方
        for (int i = 0; i < bluePlayers.length; i++) {
            long uid = bluePlayers[i];
            blueTeam.add(uid);
            Hero hero = new Hero(uid, 0);
            hero.x = 200;
            hero.y = 300 + i * 100;
            heroes.put(uid, hero);
        }
        
        // 初始化红方
        for (int i = 0; i < redPlayers.length; i++) {
            long uid = redPlayers[i];
            redTeam.add(uid);
            Hero hero = new Hero(uid, 1);
            hero.x = MAP_WIDTH - 200;
            hero.y = 300 + i * 100;
            heroes.put(uid, hero);
        }
    }
    
    /**
     * 启动游戏循环
     */
    public void start(Runnable onTick) {
        this.onTick = onTick;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 0, 1000 / TICK_RATE, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 停止游戏
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    /**
     * 每帧更新
     */
    private void tick() {
        if (isOver) return;
        
        frameId++;
        
        // 更新所有英雄
        for (Hero hero : heroes.values()) {
            updateHero(hero);
        }
        
        // 检查胜负
        checkGameOver();
        
        // 回调通知
        if (onTick != null) {
            onTick.run();
        }
    }
    
    private void updateHero(Hero hero) {
        if (hero.isDead) {
            // 复活倒计时
            if (frameId - hero.deathFrame > TICK_RATE * 5) {
                hero.respawn();
            }
            return;
        }
        
        // 移动
        if (hero.targetX != hero.x || hero.targetY != hero.y) {
            double dx = hero.targetX - hero.x;
            double dy = hero.targetY - hero.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist <= hero.speed) {
                hero.x = hero.targetX;
                hero.y = hero.targetY;
            } else {
                hero.x += (dx / dist) * hero.speed;
                hero.y += (dy / dist) * hero.speed;
            }
        }
        
        // 技能冷却
        if (hero.skillCooldown > 0) {
            hero.skillCooldown--;
        }
    }
    
    private void checkGameOver() {
        if (blueBase.hp <= 0) {
            isOver = true;
            winner = 1;  // 红方胜
        } else if (redBase.hp <= 0) {
            isOver = true;
            winner = 0;  // 蓝方胜
        }
        
        if (isOver) {
            stop();
        }
    }
    
    // ==================== 玩家操作 ====================
    
    /**
     * 移动
     */
    public void move(long userId, double x, double y) {
        Hero hero = heroes.get(userId);
        if (hero == null || hero.isDead) return;
        
        hero.targetX = Math.max(0, Math.min(MAP_WIDTH, x));
        hero.targetY = Math.max(0, Math.min(MAP_HEIGHT, y));
    }
    
    /**
     * 普通攻击
     */
    public Map<String, Object> attack(long userId, long targetId) {
        Hero hero = heroes.get(userId);
        Hero target = heroes.get(targetId);
        
        if (hero == null || hero.isDead) return null;
        if (target == null || target.isDead) return null;
        if (hero.team == target.team) return null;  // 不能攻击队友
        
        // 检查距离
        double dist = distance(hero, target);
        if (dist > hero.attackRange) return null;
        
        // 造成伤害
        target.hp -= hero.attack;
        
        Map<String, Object> result = new HashMap<>();
        result.put("attacker", userId);
        result.put("target", targetId);
        result.put("damage", hero.attack);
        result.put("targetHp", target.hp);
        
        if (target.hp <= 0) {
            target.die(frameId);
            hero.kills++;
            result.put("kill", true);
        }
        
        return result;
    }
    
    /**
     * 释放技能 (简化：AOE 伤害)
     */
    public List<Map<String, Object>> skill(long userId, double x, double y) {
        Hero hero = heroes.get(userId);
        if (hero == null || hero.isDead) return null;
        if (hero.skillCooldown > 0) return null;
        
        hero.skillCooldown = TICK_RATE * 5;  // 5秒冷却
        
        List<Map<String, Object>> hits = new ArrayList<>();
        
        // AOE 范围伤害
        for (Hero target : heroes.values()) {
            if (target.team == hero.team || target.isDead) continue;
            
            double dist = Math.sqrt(Math.pow(target.x - x, 2) + Math.pow(target.y - y, 2));
            if (dist <= 150) {  // 技能范围 150
                int damage = hero.attack * 2;
                target.hp -= damage;
                
                Map<String, Object> hit = new HashMap<>();
                hit.put("target", target.userId);
                hit.put("damage", damage);
                hit.put("targetHp", target.hp);
                
                if (target.hp <= 0) {
                    target.die(frameId);
                    hero.kills++;
                    hit.put("kill", true);
                }
                
                hits.add(hit);
            }
        }
        
        return hits;
    }
    
    /**
     * 攻击基地
     */
    public Map<String, Object> attackBase(long userId) {
        Hero hero = heroes.get(userId);
        if (hero == null || hero.isDead) return null;
        
        Base targetBase = hero.team == 0 ? redBase : blueBase;
        
        // 检查距离
        double dist = Math.sqrt(Math.pow(hero.x - targetBase.x, 2) + Math.pow(hero.y - targetBase.y, 2));
        if (dist > hero.attackRange) return null;
        
        targetBase.hp -= hero.attack;
        
        Map<String, Object> result = new HashMap<>();
        result.put("attacker", userId);
        result.put("baseTeam", targetBase.team);
        result.put("damage", hero.attack);
        result.put("baseHp", targetBase.hp);
        
        return result;
    }
    
    // ==================== 工具方法 ====================
    
    private double distance(Hero a, Hero b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }
    
    /**
     * 获取帧状态 (用于同步)
     */
    public Map<String, Object> getFrameState() {
        Map<String, Object> state = new HashMap<>();
        state.put("frameId", frameId);
        state.put("isOver", isOver);
        state.put("winner", winner);
        
        List<Map<String, Object>> heroList = new ArrayList<>();
        for (Hero h : heroes.values()) {
            heroList.add(h.toMap());
        }
        state.put("heroes", heroList);
        
        state.put("blueBase", blueBase.toMap());
        state.put("redBase", redBase.toMap());
        
        return state;
    }
    
    // ==================== 内部类 ====================
    
    public static class Hero {
        public long userId;
        public int team;  // 0=蓝, 1=红
        public double x, y;
        public double targetX, targetY;
        public int hp = 1000;
        public int maxHp = 1000;
        public int attack = 50;
        public int attackRange = 100;
        public double speed = 5;
        public int skillCooldown;
        public boolean isDead;
        public int deathFrame;
        public int kills;
        public int deaths;
        
        public Hero(long userId, int team) {
            this.userId = userId;
            this.team = team;
            this.targetX = x;
            this.targetY = y;
        }
        
        public void die(int frame) {
            isDead = true;
            deathFrame = frame;
            deaths++;
        }
        
        public void respawn() {
            isDead = false;
            hp = maxHp;
            // 回到出生点
            if (team == 0) {
                x = 200; y = 500;
            } else {
                x = MAP_WIDTH - 200; y = 500;
            }
            targetX = x;
            targetY = y;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            m.put("team", team);
            m.put("x", (int) x);
            m.put("y", (int) y);
            m.put("hp", hp);
            m.put("maxHp", maxHp);
            m.put("isDead", isDead);
            m.put("kills", kills);
            m.put("deaths", deaths);
            return m;
        }
    }
    
    public static class Base {
        public int team;
        public double x, y;
        public int hp;
        public int maxHp;
        
        public Base(int team, double x, double y, int hp) {
            this.team = team;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.maxHp = hp;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("team", team);
            m.put("x", (int) x);
            m.put("y", (int) y);
            m.put("hp", hp);
            m.put("maxHp", maxHp);
            return m;
        }
    }
}
