package top.zeroone.job.annotation;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisTimeLock {

    private final StringRedisTemplate stringRedisTemplate;
    private final String key;
    private final Long second;

    public RedisTimeLock(final RedisConnectionFactory factory, final String key, final Long second) {
        this.stringRedisTemplate = new StringRedisTemplate(factory);
        this.key = key + ":time";
        this.second = second;
    }

    public boolean tryLock() {
        final boolean flag = this.stringRedisTemplate.opsForValue().setIfAbsent(this.key, "1");
        if (flag) {
            this.stringRedisTemplate.opsForValue().set(this.key, "1", this.second, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }
}