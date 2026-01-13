package game.robot.ai;

import game.robot.Cmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 斗地主 AI
 */
public class DoudizhuAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 机器人手牌
    private final Map<Long, List<Integer>> robotCards = new ConcurrentHashMap<>();
    // 机器人座位
    private final Map<Long, Integer> robotSeats = new ConcurrentHashMap<>();
    // 当前叫地主座位
    private final Map<Long, Integer> currentBidSeat = new ConcurrentHashMap<>();
    // 地主座位
    private final Map<Long, Integer> landlordSeat = new ConcurrentHashMap<>();
    // 上一手牌
    private final Map<Long, List<Integer>> lastPlayedCards = new ConcurrentHashMap<>();
    // 上一手牌的出牌者座位
    private final Map<Long, Integer> lastPlaySeat = new ConcurrentHashMap<>();
    
    @Override
    protected void onSeatAssigned(Robot robot, int seatIndex) {
        robotSeats.put(robot.userId, seatIndex);
    }
    
    @Override
    public void onGameStart(Robot robot, Map<String, Object> data) {
        if (data != null && data.containsKey("bidSeat")) {
            int bidSeat = ((Number) data.get("bidSeat")).intValue();
            currentBidSeat.put(robot.userId, bidSeat);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void onGameCmd(Robot robot, int cmd, int code, Map<String, Object> data) {
        switch (cmd) {
            case Cmd.USER_READY:
                if (data != null && data.containsKey("userId") && data.containsKey("seatIndex")) {
                    long uid = ((Number) data.get("userId")).longValue();
                    if (uid == robot.userId) {
                        robotSeats.put(robot.userId, ((Number) data.get("seatIndex")).intValue());
                    }
                }
                break;
                
            case Cmd.DEAL:
                List<Number> cards = (List<Number>) data.get("cards");
                List<Integer> cardList = new ArrayList<>();
                for (Number n : cards) cardList.add(n.intValue());
                robotCards.put(robot.userId, cardList);
                
                Integer bidSeat = currentBidSeat.get(robot.userId);
                Integer mySeat = robotSeats.get(robot.userId);
                if (bidSeat != null && mySeat != null && bidSeat.equals(mySeat)) {
                    scheduleBid(robot);
                }
                break;
                
            case Cmd.DDZ_BID_RESULT:
                int nextBidSeat = data.containsKey("nextSeat") 
                    ? ((Number) data.get("nextSeat")).intValue() : -1;
                int landlord = data.containsKey("landlordSeat") 
                    ? ((Number) data.get("landlordSeat")).intValue() : -1;
                
                if (landlord >= 0) {
                    landlordSeat.put(robot.userId, landlord);
                    if (data.containsKey("bottomCards")) {
                        List<Number> bottom = (List<Number>) data.get("bottomCards");
                        Integer myS = robotSeats.get(robot.userId);
                        if (myS != null && myS == landlord) {
                            List<Integer> myCards = robotCards.get(robot.userId);
                            if (myCards != null) {
                                for (Number n : bottom) myCards.add(n.intValue());
                            }
                            schedulePlay(robot, null);
                        }
                    }
                } else if (nextBidSeat >= 0) {
                    currentBidSeat.put(robot.userId, nextBidSeat);
                    Integer myS = robotSeats.get(robot.userId);
                    if (myS != null && myS == nextBidSeat) {
                        scheduleBid(robot);
                    }
                }
                break;
                
            case Cmd.DDZ_PLAY_RESULT:
                int playSeat = ((Number) data.get("seatIndex")).intValue();
                boolean pass = data.containsKey("pass") && (Boolean) data.get("pass");
                int nextSeat = data.containsKey("nextSeat") 
                    ? ((Number) data.get("nextSeat")).intValue() : -1;
                boolean gameOver = data.containsKey("gameOver") && (Boolean) data.get("gameOver");
                boolean clearLast = data.containsKey("clearLast") && (Boolean) data.get("clearLast");
                
                Integer myS = robotSeats.get(robot.userId);
                
                if (!pass && data.containsKey("cards")) {
                    Object cardsObj = data.get("cards");
                    List<Integer> played = toIntList(cardsObj);
                    lastPlayedCards.put(robot.userId, played);
                    lastPlaySeat.put(robot.userId, playSeat);
                    
                    if (myS != null && myS == playSeat) {
                        List<Integer> myCards = robotCards.get(robot.userId);
                        if (myCards != null) myCards.removeAll(played);
                    }
                }
                
                // 清空上一手牌（两人都 pass 后，新一轮开始）
                if (clearLast) {
                    lastPlayedCards.remove(robot.userId);
                    lastPlaySeat.remove(robot.userId);
                }
                
                if (!gameOver && nextSeat >= 0 && myS != null && myS == nextSeat) {
                    // 判断是否需要主动出牌（新一轮）
                    Integer lastSeat = lastPlaySeat.get(robot.userId);
                    boolean isNewRound = (lastSeat == null || lastSeat.equals(myS));
                    List<Integer> last = isNewRound ? null : lastPlayedCards.get(robot.userId);
                    schedulePlay(robot, last);
                }
                break;
        }
    }
    
    private List<Integer> toIntList(Object obj) {
        List<Integer> result = new ArrayList<>();
        if (obj instanceof int[]) {
            for (int c : (int[]) obj) result.add(c);
        } else if (obj instanceof List) {
            for (Object o : (List<?>) obj) result.add(((Number) o).intValue());
        }
        return result;
    }
    
    private void scheduleBid(Robot robot) {
        scheduler.schedule(() -> {
            boolean wantBid = decideWantBid(robot.userId);
            Map<String, Object> bidData = new HashMap<>();
            bidData.put("bid", wantBid);
            robot.sendGameCmd(Cmd.DDZ_BID, bidData);
        }, 500 + new Random().nextInt(1000), TimeUnit.MILLISECONDS);
    }
    
    private void schedulePlay(Robot robot, List<Integer> lastCards) {
        scheduler.schedule(() -> {
            List<Integer> playCards = decidePlay(robot.userId, lastCards);
            // 如果是新一轮 (lastCards 为空)，必须出牌，不能 pass
            boolean mustPlay = (lastCards == null || lastCards.isEmpty());
            
            if (playCards == null || playCards.isEmpty()) {
                if (mustPlay) {
                    // 必须出牌时，出最小的一张
                    List<Integer> myCards = robotCards.get(robot.userId);
                    if (myCards != null && !myCards.isEmpty()) {
                        myCards.sort(Comparator.comparingInt(this::getCardValue));
                        Map<String, Object> playData = new HashMap<>();
                        playData.put("cards", Collections.singletonList(myCards.get(0)));
                        robot.sendGameCmd(Cmd.DDZ_PLAY, playData);
                    }
                } else {
                    robot.sendGameCmd(Cmd.DDZ_PASS, null);
                }
            } else {
                Map<String, Object> playData = new HashMap<>();
                playData.put("cards", playCards);
                robot.sendGameCmd(Cmd.DDZ_PLAY, playData);
            }
        }, 800 + new Random().nextInt(1200), TimeUnit.MILLISECONDS);
    }
    
    private boolean decideWantBid(long robotId) {
        List<Integer> cards = robotCards.get(robotId);
        if (cards == null) return false;
        
        int bigCards = 0;
        for (int card : cards) {
            int value = card % 13;
            if (value >= 11 || card >= 52) bigCards++;
        }
        return bigCards >= 2;
    }
    
    private List<Integer> decidePlay(long robotId, List<Integer> lastCards) {
        List<Integer> myCards = robotCards.get(robotId);
        if (myCards == null || myCards.isEmpty()) return Collections.emptyList();
        
        myCards.sort(Comparator.comparingInt(this::getCardValue));
        
        if (lastCards == null || lastCards.isEmpty()) {
            return Collections.singletonList(myCards.get(0));
        }
        
        int lastValue = getCardValue(lastCards.get(0));
        int lastCount = lastCards.size();
        
        if (lastCount == 1) {
            for (int card : myCards) {
                if (getCardValue(card) > lastValue) {
                    return Collections.singletonList(card);
                }
            }
        } else if (lastCount == 2) {
            Map<Integer, List<Integer>> groups = groupByValue(myCards);
            for (Map.Entry<Integer, List<Integer>> e : groups.entrySet()) {
                if (e.getKey() > lastValue && e.getValue().size() >= 2) {
                    return e.getValue().subList(0, 2);
                }
            }
        }
        return Collections.emptyList();
    }
    
    private int getCardValue(int card) {
        if (card >= 52) return card;
        return card % 13;
    }
    
    private Map<Integer, List<Integer>> groupByValue(List<Integer> cards) {
        Map<Integer, List<Integer>> groups = new TreeMap<>();
        for (int card : cards) {
            groups.computeIfAbsent(getCardValue(card), k -> new ArrayList<>()).add(card);
        }
        return groups;
    }
    
    @Override
    public void onGameOver(Robot robot, Map<String, Object> data) {
        robotCards.remove(robot.userId);
        robotSeats.remove(robot.userId);
        currentBidSeat.remove(robot.userId);
        landlordSeat.remove(robot.userId);
        lastPlayedCards.remove(robot.userId);
        lastPlaySeat.remove(robot.userId);
    }
}
