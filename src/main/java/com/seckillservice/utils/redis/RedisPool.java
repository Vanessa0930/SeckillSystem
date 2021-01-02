package main.java.com.seckillservice.utils.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {

    private static JedisPool instance;

    private static int MAX_TOTAL = 100;
    private static int MAX_IDLE = 100;
    private static int MAX_WAIT = 10000; // 10s
    private static String REDIS_HOST_ADDRESS = "127.0.0.1"; // default localhost
    private static int REDIS_PORT = 6379;
    private static int DEFAULT_TIMEOUT = 2000; // 2s

    private static void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(MAX_TOTAL);
        config.setMaxIdle(MAX_IDLE);
        config.setMaxWaitMillis(MAX_WAIT);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        instance = new JedisPool(config, REDIS_HOST_ADDRESS, REDIS_PORT, DEFAULT_TIMEOUT);
    }

    static {
        init();
    }

    public static Jedis getJedis() {
        return instance.getResource();
    }

    public static void closeJedis(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}
