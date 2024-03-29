package com.lyw;

import com.lyw.bo.Idiom;
import com.lyw.config.GameStatus;
import com.lyw.core.IdiomCollection;
import com.lyw.core.IdiomGame;
import com.sobte.cqp.jcq.entity.CQDebug;
import com.sobte.cqp.jcq.entity.IMsg;

import java.util.*;

public class Saifubot extends SaifuAppAbstract {

    private IdiomCollection idiomCollection;

    public int startup() {
        CQ.logInfo("saifu-bot", "初始化成语词典...");
        idiomCollection = IdiomCollection.getInstance();
        CQ.logInfo("saifu-bot", "初始化完毕");
        return 0;
    }

    public int groupMsg(int subType, int msgId, long fromGroup, long fromQQ, String fromAnonymous, String msg, int font) {
        if (idiomCollection == null) {
            return IMsg.MSG_IGNORE;
        }
        if (GameStatus.isRunning(fromGroup)) {
            GameStatus.getGame(fromGroup).setMessage(fromQQ, msg);
        } else {
            if (msg.equals("!成语接龙")) {
                IdiomGame.start(fromGroup);
            } else if (msg.startsWith("!成语查询")) {
                String[] splitMsg = msg.split(" ");
                if (splitMsg.length == 2) {
                    String start = splitMsg[1];
                    Idiom idiom = idiomCollection.getFollow(start);
                    if (idiom == null) {
                        CQ.sendGroupMsg(fromGroup, "找不到成语");
                    } else {
                        CQ.sendGroupMsg(fromGroup, idiom.getWord());
                    }
                } else if (splitMsg.length == 3) {
                    String start = splitMsg[1];
                    String end = splitMsg[2];
                    List<Idiom> idiomChain = idiomCollection.getFollowChain(start, end);
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
