package com.lyw.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lyw.bo.Idiom;
import com.lyw.utils.RandomUtils;
import com.sobte.cqp.jcq.event.JcqApp;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;

public class IdiomCollection {

    private static IdiomCollection instance = new IdiomCollection();

    public static IdiomCollection getInstance() {
        return instance;
    }

    /**
     * 词典，包含成语之间的接龙关系图
     */
    private Map<String, Idiom> idiomBook;

    /**
     * 初始化词典
     */
    private IdiomCollection() {
        String bookStr = fetchBook("http://cdn.jsdelivr.net/gh/pwxcoo/chinese-xinhua/data/idiom.json");
        if (bookStr == null) {
            JcqApp.CQ.logError("saifu-bot", "获取成语词典异常");
            return;
        }
        // 存到内存中的哈希表里
        idiomBook = new HashMap<>();
        idiomBook.put("一个顶俩", new Idiom("一个顶俩", "yi", "lia"));
        JSONArray bookJson = JSONArray.parseArray(bookStr);
        for (Object obj : bookJson) {
            JSONObject jsonObject = (JSONObject) obj;
            fixIdiom(jsonObject);
            String word = jsonObject.getString("word");
            String py = jsonObject.getString("pinyin");
            // 可以接受音调不同的接龙
            String simplePy = py.replaceAll("[āáǎà]", "a").replaceAll("[ōóǒò]", "o")
                    .replaceAll("[ēéěèê]", "e").replaceAll("[īíǐì]", "i")
                    .replaceAll("[ūúǔù]", "u").replaceAll("[ǖǘǚǜü]", "v");
            String[] splitPy = simplePy.split(" ");
            String startPy = splitPy[0];
            String endPy = splitPy[splitPy.length - 1];
            idiomBook.put(word, new Idiom(word, startPy, endPy));
        }
        // 把表的value串起来，形成一张图
        for (Idiom vCurrent : idiomBook.values()) {
            for (Idiom vNext : idiomBook.values()) {
                if (vCurrent.getEndPy().equals(vNext.getStartPy())) {
                    vCurrent.getNext().add(vNext);
                }
            }
        }
    }

    /**
     * 获取网络成语词典
     */
    private String fetchBook(String idiomUrl) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = HttpClientBuilder.create().build();
            response = httpClient.execute(new HttpGet(idiomUrl));
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            JcqApp.CQ.logError("saifu-bot", e.getMessage());
            return null;
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                JcqApp.CQ.logError("saifu-bot", e.getMessage());
            }
        }
    }

    private void fixIdiom(JSONObject data) {
        String word = data.getString("word");
        String pinyin = data.getString("pinyin");
        if ("味同嚼蜡".equals(word)) {
            data.put("pinyin", pinyin.replace("cù", "là"));
        }
        if (word.endsWith("俩")) {
            data.put("pinyin", pinyin.replace("liǎng", "liǎ"));
        }
        data.put("pinyin", pinyin.replaceAll("yi([ēéěèêe])", "ye"));
    }

    /**
     * 随机获取一个成语
     */
    public Idiom getIdiom() {
        String key = RandomUtils.randomFromCollection(idiomBook.keySet());
        return idiomBook.get(key);
    }

    /**
     * 根据字符串获取成语对象
     */
    public Idiom getIdiom(String idiomStr) {
        return idiomBook.get(idiomStr);
    }

    /**
     * 随机获取一个接龙词
     */
    public Idiom getFollow(String startWord) {
        Set<Idiom> nextSet = getAllFollow(startWord);
        if (nextSet.isEmpty()) {
            return null;
        }
        return RandomUtils.randomFromCollection(nextSet);
    }

    /**
     * 随机获取一个接龙词，排除某些成语
     */
    public Idiom getFollow(String startWord, Set<Idiom> exclude) {
        Set<Idiom> nextSet = getAllFollow(startWord);
        if (nextSet.isEmpty()) {
            return null;
        }
        nextSet.removeIf(exclude::contains);
        return RandomUtils.randomFromCollection(nextSet);
    }

    /**
     * 获取所有接龙词
     */
    public Set<Idiom> getAllFollow(String startWord) {
        Idiom idiomStart = idiomBook.get(startWord);
        if (idiomStart == null) {
            return null;
        }
        return idiomStart.getNext();
    }

    /**
     * 最短路径算法找出接龙路径
     */
    public List<Idiom> getFollowChain(String startWord, String endWord) {
        Idiom idiomStart = idiomBook.get(startWord);
        Idiom idiomEnd = idiomBook.get(endWord);
        if (idiomStart == null || idiomEnd == null) {
            return new ArrayList<>();
        }
        return BFS(idiomStart, idiomEnd, new HashMap<>());
    }

    /**
     * 广度优先遍历（按层次）
     */
    private List<Idiom> BFS(Idiom start, Idiom end, Map<Idiom, Idiom> visited) {
        Queue<Idiom> q = new LinkedList<>();
        q.add(start);
        visited.put(start, null);
        while (!q.isEmpty()) {
            Queue<Idiom> currentQ = new LinkedList<>(q);
            q.clear();
            while (!currentQ.isEmpty()) {
                Idiom top = currentQ.poll();
                if (top == end) {
                    List<Idiom> result = new ArrayList<>();
                    Idiom rev = end;
                    while (rev != null) {
                        result.add(0, rev);
                        rev = visited.get(rev);
                    }
                    return result;
                }
                for (Idiom next : top.getNext()) {
                    if (!visited.containsKey(next)) {
                        visited.put(next, top);
                        q.add(next);
                    }
                }
            }
        }
        return new ArrayList<>();
    }

}
