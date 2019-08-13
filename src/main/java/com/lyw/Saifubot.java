package com.lyw;

import com.lyw.bo.Idiom;
import com.lyw.core.IdiomFollow;
import com.sobte.cqp.jcq.entity.CQDebug;
import com.sobte.cqp.jcq.entity.ICQVer;
import com.sobte.cqp.jcq.entity.IMsg;
import com.sobte.cqp.jcq.entity.IRequest;

import java.util.List;

public class Saifubot extends SaifuAppAbstract implements ICQVer, IMsg, IRequest {

    private IdiomFollow idiomFollow;

    public int startup() {
        CQ.logInfo("App Info", "初始化成语词典...");
        idiomFollow = new IdiomFollow();
        CQ.logInfo("App Info", "初始化完毕");
        return 0;
    }

    public int groupMsg(int subType, int msgId, long fromGroup, long fromQQ, String fromAnonymous, String msg, int font) {
        if (msg.startsWith("!idiom")) {
            if (idiomFollow == null) {
                CQ.sendGroupMsg(fromGroup, "组件正在初始化，请稍候...");
                return IMsg.MSG_IGNORE;
            }
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
