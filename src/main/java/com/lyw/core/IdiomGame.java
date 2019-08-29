package com.lyw.core;

import com.lyw.bo.GameNode;
import com.lyw.bo.Idiom;
import com.lyw.config.GameStatus;
import com.lyw.config.LocalConfig;
import com.lyw.config.ThreadPoolConfig;
import com.sobte.cqp.jcq.event.JcqApp;
import com.sobte.cqp.jcq.message.CQCode;

import java.util.*;
import java.util.stream.Collectors;

public class IdiomGame implements Runnable {

    private IdiomCollection idiomCollection;

    /**
     * 保存当前游戏进度
     */
    private final List<GameNode> GAME_LIST;

    private String currentIdiom() {
        return GAME_LIST.get(GAME_LIST.size() - 1).getIdiom().getWord();
    }

    /**
     * 群ID
     */
    private final long groupId;

    /**
     * 游戏超时时间
     */
    private final long timeout;

    /**
     * 后台计时任务
     */
    private final Timer taskHalf, taskFull;

    /**
     * 消息来源qq
     */
    private volatile long qq;

    /**
     * 消息内容
     */
    private volatile String message;

    public static void start(long groupId) {
        new IdiomGame(groupId);
    }

    private IdiomGame(long groupId) {
        this.idiomCollection = IdiomCollection.getInstance();
        this.GAME_LIST = new ArrayList<>();
        this.groupId = groupId;
        this.timeout = 30000L;
        this.taskHalf = new Timer();
        this.taskFull = new Timer();
        ThreadPoolConfig.gamePool.submit(this);
    }

    public synchronized void setMessage(long qq, String message) {
        this.qq = qq;
        this.message = message;
        notify();
    }

    @Override
    public synchronized void run() {
        GameStatus.startGame(groupId, this);
        Idiom first = idiomCollection.getIdiom();
        GAME_LIST.add(new GameNode(LocalConfig.ROBOT_QQ, first));
        JcqApp.CQ.sendGroupMsg(groupId, first.getWord());
        startCounter();
        while (true) {
            try {
                wait();
            } catch (InterruptedException e) {
                JcqApp.CQ.logError("saifu-bot", e.getMessage());
                break;
            }
            if (message.equals("!游戏结束")) {
                break;
            }
            if (idiomCollection.getAllFollow(currentIdiom())
                    .stream().map(Idiom::getWord).anyMatch(word -> word.equals(message))) {
                if (GAME_LIST.stream().anyMatch(gameNode -> gameNode.getIdiom().getWord().equals(message))) {
                    JcqApp.CQ.sendGroupMsg(groupId, "[" + message + "]已经使用过了喔~");
                    continue;
                }
                resetCounter();
                GAME_LIST.add(new GameNode(qq, idiomCollection.getIdiom(message)));
                Idiom next = idiomCollection.getFollow(message,
                        GAME_LIST.stream().map(GameNode::getIdiom).collect(Collectors.toSet()));
                if (next == null) {
                    JcqApp.CQ.sendGroupMsg(groupId, "找不到接龙词，你赢了~");
                    break;
                }
                GAME_LIST.add(new GameNode(LocalConfig.ROBOT_QQ, next));
                JcqApp.CQ.sendGroupMsg(groupId, next.getWord());
            }
        }
        cancelCounter();
        GameStatus.endGame(groupId);
        JcqApp.CQ.sendGroupMsg(groupId, buildGameResult());
    }

    private String buildGameResult() {
        StringBuilder gameResult = new StringBuilder("游戏成绩：\n");
        Map<Long, List<GameNode>> resultCol = GAME_LIST.stream()
                .filter(gameNode -> gameNode.getQq() != LocalConfig.ROBOT_QQ)
                .collect(Collectors.groupingBy(GameNode::getQq));
        resultCol.forEach((qq, idioms) -> gameResult.append(new CQCode().at(qq)).append(idioms.size()).append("\n"));
        gameResult.append("发送[!成语接龙]重新开始游戏");
        return gameResult.toString();
    }

    /**
     * 后台计时器操作
     */
    private void startCounter() {
        taskHalf.schedule(new TimerTask() {
            @Override
            public void run() {
                JcqApp.CQ.sendGroupMsg(groupId, "还剩" + timeout / 2000 + "秒，当前成语是[" + currentIdiom() + "]");
            }
        }, timeout / 2);
        taskFull.schedule(new TimerTask() {
            @Override
            public void run() {
                JcqApp.CQ.sendGroupMsg(groupId, "时间到~");
                setMessage(LocalConfig.ROBOT_QQ, "!游戏结束");
            }
        }, timeout);
    }

    private void cancelCounter() {
        taskHalf.cancel();
        taskFull.cancel();
    }

    private void resetCounter() {
        cancelCounter();
        startCounter();
    }

}
