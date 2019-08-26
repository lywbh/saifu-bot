package com.lyw;

import com.lyw.bo.Idiom;
import com.lyw.core.IdiomFollow;
import com.sobte.cqp.jcq.entity.CQDebug;
import com.sobte.cqp.jcq.entity.IMsg;

import java.util.*;

public class Saifubot extends SaifuAppAbstract {

    private IdiomFollow idiomFollow;

    private volatile Map<Long, List<String>> GAME_MAP = new HashMap<>();

    public int startup() {
        CQ.logInfo("App Info", "初始化成语词典...");
        idiomFollow = new IdiomFollow();
        CQ.logInfo("App Info", "初始化完毕");
        return 0;
    }

    public int groupMsg(int subType, int msgId, long fromGroup, long fromQQ, String fromAnonymous, String msg, int font) {
        if (idiomFollow == null) {
            return IMsg.MSG_IGNORE;
        }
        if (GAME_MAP.containsKey(fromGroup)) {
            if (msg.equals("!游戏结束")) {
                CQ.sendGroupMsg(fromGroup, "游戏已结束~");
                GAME_MAP.remove(fromGroup);
            } else if (idiomFollow.isIdiom(msg)) {
                List<String> gameList = GAME_MAP.get(fromGroup);
                if (gameList.stream().anyMatch(word -> word.equals(msg))) {
                    CQ.sendGroupMsg(fromGroup, "[" + msg + "]已经使用过了喔~");
                } else {
                    Set<Idiom> allNext = idiomFollow.getAllIdiom(gameList.get(gameList.size() - 1));
                    boolean followSuccess = allNext.stream().map(Idiom::getWord).anyMatch(word -> word.equals(msg));
                    if (followSuccess) {
                        gameList.add(msg);
                        Idiom next = idiomFollow.getIdiom(msg, new HashSet<>(gameList));
                        if (next != null) {
                            gameList.add(next.getWord());
                            CQ.sendGroupMsg(fromGroup, next.getWord());
                        } else {
                            CQ.sendGroupMsg(fromGroup, "找不到接龙词，你赢了~");
                            GAME_MAP.remove(fromGroup);
                        }
                    }
                }
            }
        } else {
            if (msg.equals("!成语接龙")) {
                Idiom next = idiomFollow.getIdiom();
                GAME_MAP.put(fromGroup, new ArrayList<>());
                GAME_MAP.get(fromGroup).add(next.getWord());
                CQ.sendGroupMsg(fromGroup, next.getWord());
            } else if (msg.startsWith("!idiom")) {
                String[] splitMsg = msg.split(" ");
                if (splitMsg.length == 2) {
                    String start = splitMsg[1];
                    Idiom idiom = idiomFollow.getIdiom(start);
                    if (idiom == null) {
                        CQ.sendGroupMsg(fromGroup, "找不到成语");
                    } else {
                        CQ.sendGroupMsg(fromGroup, idiom.getWord());
                    }
                } else if (splitMsg.length == 3) {
                    String start = splitMsg[1];
                    String end = splitMsg[2];
                    List<Idiom> idiomChain = idiomFollow.getIdiomChain(start, end);
                    if (idiomChain.isEmpty()) {
                        CQ.sendGroupMsg(fromGroup, "找不到接龙路径");
                    } else {
                        StringBuilder result = new StringBuilder();
                        for (Idiom idiom : idiomChain) {
                            result.append(idiom.getWord()).append(" ");
                        }
                        CQ.sendGroupMsg(fromGroup, result.toString());
                    }
                } else {
                    CQ.sendGroupMsg(fromGroup, "指令错误，请检查是否有多余的空格或换行符");
                }
            }
        }
        return IMsg.MSG_IGNORE;
    }

    public static void main(String[] args) {
        CQ = new CQDebug();
        Saifubot demo = new Saifubot();
        demo.startup();
        demo.enable();
        demo.groupMsg(0, 10006, 3456789012L, 3333333334L, "", "!idiom 锲而不舍 舍己为人", 0);
        demo.exit();
    }

}
