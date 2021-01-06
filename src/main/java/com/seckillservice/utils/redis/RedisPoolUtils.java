package com.seckillservice.utils.redis;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class RedisPoolUtils {
    private static final Logger log = LogManager.getLogger(RedisPoolUtils.class);

    public static String set(String key, String val) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.set(key, val);
        } catch (Exception e) {
            log.error("Failed to set key {} with value {}", key, val, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }

    public static String get(String key) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("Failed to get key {}", key, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }

    public static Long del(String key) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.del(key);
        } catch (Exception e) {
            log.error("Failed to delete key {}", key, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }

    public static Long incr(String key) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.incr(key);
        } catch (Exception e) {
            log.error("Failed to increment key {}", key, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }

    public static Long decr(String key) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.decr(key);
        } catch (Exception e) {
            log.error("Failed to decrement key %{}", key, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }

    public static List<String> listAllElements(String key) {
        Jedis jedis = null;
        List<String> result = new ArrayList<>();
        try {
            jedis = RedisPool.getJedis();
            result = jedis.lrange(key, 0, -1);
        } catch (Exception e) {
            log.error("Failed to list all elements for key {}", key, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }

    public static Long addToListHead(String key, String name, String count, String sales, String version) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.lpush(key, name, count, sales, version);
        } catch (Exception e) {
            log.error("Failed to add key {}", key, e);
        } finally {
            RedisPool.closeJedis(jedis);
        }
        return result;
    }
}
