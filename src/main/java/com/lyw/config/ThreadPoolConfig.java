package com.lyw.config;

import java.util.concurrent.*;

public class ThreadPoolConfig {

    public static ExecutorService gamePool = Executors.newCachedThreadPool();

}
