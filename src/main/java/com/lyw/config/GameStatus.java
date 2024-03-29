package com.lyw.config;

import com.lyw.core.IdiomGame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameStatus {

    private static Map<Long, IdiomGame> GAME_MAP = new ConcurrentHashMap<>();

    public static boolean isRunning(long groupId) {
        return GAME_MAP.containsKey(groupId);
    }

    public static IdiomGame getGame(long groupId) {
        return GAME_MAP.get(groupId);
    }

    public static void startGame(long groupId, IdiomGame game) {
        GAME_MAP.put(groupId, game);
    }

    public static void endGame(long groupId) {
        GAME_MAP.remove(groupId);
    }

}
