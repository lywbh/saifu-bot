package com.lyw.core;

import com.lyw.bo.Idiom;
import com.lyw.config.GameStatus;
import com.lyw.config.ThreadPoolConfig;
import com.sobte.cqp.jcq.event.JcqApp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IdiomGame implements Runnable {

    private IdiomFollow idiomFollow;

    /**
     * 保存当次游戏已使用的成语
     */
    private volatile List<String> GAME_LIST;

    /**
     * 群ID
     */
    private long groupId;

    /**
     * 消息内容
     */
    private String message;

    public IdiomGame(long groupId) {
        this.idiomFollow = IdiomFollow.getInstance();
        this.GAME_LIST = new ArrayList<>();
        this.message = null;
        this.groupId = groupId;
        Idiom next = idiomFollow.getIdiom();
        GAME_LIST.add(next.getWord());
        ThreadPoolConfig.gamePool.submit(this);
        JcqApp.CQ.sendGroupMsg(groupId, next.getWord());
    }

    public synchronized void setMessage(String message) {
        this.message = message;
        notify();
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    wait();
                    if (message.equals("!游戏结束")) {
                        GameStatus.endGame(groupId);
                        JcqApp.CQ.sendGroupMsg(groupId, "游戏已结束~");
                        break;
                    }
                    if (idiomFollow.isIdiom(message)) {
                        if (GAME_LIST.contains(message)) {
                            JcqApp.CQ.sendGroupMsg(groupId, "[" + message + "]已经使用过了喔~");
                        } else {
                            Set<Idiom> allNext = idiomFollow.getAllIdiom(GAME_LIST.get(GAME_LIST.size() - 1));
                            boolean followSuccess = allNext.stream().map(Idiom::getWord).anyMatch(word -> word.equals(message));
                            if (followSuccess) {
                                GAME_LIST.add(message);
                                Idiom next = idiomFollow.getIdiom(message, new HashSet<>(GAME_LIST));
                                if (next != null) {
                                    GAME_LIST.add(next.getWord());
                                    JcqApp.CQ.sendGroupMsg(groupId, next.getWord());
                                } else {
                                    GameStatus.endGame(groupId);
                                    JcqApp.CQ.sendGroupMsg(groupId, "找不到接龙词，你赢了~");
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                JcqApp.CQ.logError("App Info", e.getMessage());
            }
        }
    }

}
