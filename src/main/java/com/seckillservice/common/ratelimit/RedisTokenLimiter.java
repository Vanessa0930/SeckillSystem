package com.seckillservice.common.ratelimit;

import redis.clients.jedis.Jedis;

import com.seckillservice.utils.redis.RedisPool;

import java.io.IOException;
import java.util.Collections;

import static com.seckillservice.utils.redis.LuaScriptUtil.getRateLimiterScriptSource;
import static com.seckillservice.utils.redis.LuaScriptUtil.readScript;

/**
 * Rate limiter implemented using token bucket algorithm and Redis.
 * <p>
 * Inspired from https://github.com/gongfukangEE/Distributed-Learn and
 * https://github.com/daydreamdev/seconds-kill
 */
public class RedisTokenLimiter {
    private static final int FAIL_CODE = 0;
    // Set limit to 5 requests/sec
    private static Integer LIMIT_THRESHOLD = 5;

    public static boolean canGetAccess() throws IOException {
        Jedis jedis = null;
        Object result;
        try {
            jedis = RedisPool.getJedis();
            String script = readScript(getRateLimiterScriptSource());
            String keyCount = String.valueOf(System.currentTimeMillis() / 1000);
            result = jedis.eval(script, Collections.singletonList(keyCount),
                    Collections.singletonList(String.valueOf(LIMIT_THRESHOLD)));
            if (FAIL_CODE != (long)result) {
                // successful get access
                return true;
            }
            return false;
        } catch (Exception e) {
            throw e;
        } finally {
            RedisPool.closeJedis(jedis);
        }
    }
}
