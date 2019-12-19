package top.zeroone.job.annotation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.locks.Lock;

@Slf4j
public class JobScheduledMethodRunnable extends ScheduledMethodRunnable {

    private final Lock lock;
    private final JobScheduledLock scheduledLock;
    private final String name;
    private final RedisTimeLock timeLock;

    public JobScheduledMethodRunnable(final ScheduledMethodRunnable runnable, final RedisConnectionFactory factory, final JobScheduledLock scheduledLock) {
        super(runnable.getTarget(), runnable.getMethod());
        this.scheduledLock = scheduledLock;
        this.name = name(runnable.getTarget(), runnable.getMethod(), scheduledLock.id());
        final RedisLockRegistry redisLockRegistry;
        redisLockRegistry = new RedisLockRegistry(factory, this.name);
        this.lock = redisLockRegistry.obtain(":lock");

        if (scheduledLock.lockSecond() > 0) {
            this.timeLock = new RedisTimeLock(factory, this.name, scheduledLock.lockSecond());
        } else {
            this.timeLock = null;
        }
    }

    public String name(final Object object, final Method method, final String id) {
        final String key = StringUtils.isBlank(id) ? (object.getClass().getName() + method.getName()) : id;
        return "redis:lock:" + key;
    }


    @Override
    public void run() {
        if (this.scheduledLock.ifLockDoNot()) {
            if (this.timeLock != null) {
                // 有些任务,短时间内不必重复执行.因没有等待执行,所以没有显式的解锁
                if (!this.timeLock.tryLock()) {
                    log.debug("任务{}, time tryLock return false, 停止执行", this.name);
                    return;
                }
            }

            if (this.lock.tryLock()) {
                try {
                    invokeMethod();
                } finally {
                    this.lock.unlock();
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("任务{}, tryLock return false, 停止执行", this.name);
                }
            }
        } else {
            invokeMethod();
        }
    }

    protected void invokeMethod() {
        try {
            ReflectionUtils.makeAccessible(getMethod());
            final Object[] objects = new Object[this.getMethod().getParameterCount()];
            for (int i = 0; i < objects.length; i++) {
                objects[i] = null;
            }

            this.getMethod().invoke(getTarget(), objects);
        } catch (final InvocationTargetException ex) {
            ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
        } catch (final IllegalAccessException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

    public Lock getLock() {
        return lock;
    }

    public JobScheduledLock getScheduledLock() {
        return scheduledLock;
    }

    public String getName() {
        return name;
    }

    public RedisTimeLock getTimeLock() {
        return timeLock;
    }
}