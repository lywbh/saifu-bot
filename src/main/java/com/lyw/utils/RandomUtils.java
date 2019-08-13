package com.lyw.utils;

import java.util.Collection;
import java.util.Random;

public class RandomUtils {

    private static Random random = new Random();

    private static Random getRandom() {
        return random;
    }

    /**
     * 获得一个[0,max)之间的整数。
     */
    public static int getRandomInt(int max) {
        return getRandomInt(0, max);
    }

    /**
     * 获得一个[min,max)之间的整数。
     */
    public static int getRandomInt(int min, int max) {
        return Math.abs(getRandom().nextInt()) % max + min;
    }

    /**
     * 从集合中随机取一个元素
     */
    public static <E> E randomFromCollection(Collection<E> set) {
        int rn = getRandomInt(set.size());
        int i = 0;
        for (E e : set) {
            if (i == rn) {
                return e;
            }
            i++;
        }
        return null;
    }

}
