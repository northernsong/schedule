package top.zeroone.job.annotation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    public JobScheduledMethodRunnable(final ScheduledMethodRunnable runnable, final RedisConnectionFactory factory, final JobScheduledLock scheduledLock) {
        super(runnable.getTarget(), runnable.getMethod());
        this.scheduledLock = scheduledLock;
        this.name = StringUtils.isBlank(scheduledLock.id()) ? name(runnable.getTarget(), runnable.getMethod()) : scheduledLock.id();

        final RedisLockRegistry redisLockRegistry;
        if (scheduledLock.lockSecond() > 0) {
            redisLockRegistry = new RedisLockRegistry(factory, this.name, scheduledLock.lockSecond());
        } else {
            redisLockRegistry = new RedisLockRegistry(factory, this.name);
        }
        this.lock = redisLockRegistry.obtain("l");
    }

    public String name(final Object object, final Method method) {
        return object.getClass().getName() + method.getName();
    }


    @Override
    public void run() {
        if (this.scheduledLock.ifLockDoNot()) {
            if (this.lock.tryLock()) {
                try {
                    invokeMethod();
                } finally {
                    if (this.scheduledLock.lockSecond() == 0) {
                        this.lock.unlock();
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("任务{}, tryLock return false, 停止执行", this.name);
                }
            }
        }
    }

    private void invokeMethod() {
        try {
            ReflectionUtils.makeAccessible(getMethod());
            this.getMethod().invoke(getTarget());
        } catch (final InvocationTargetException ex) {
            ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
        } catch (final IllegalAccessException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
}
