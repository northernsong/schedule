package top.zeroone.job.annotation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
public class JobScheduledAnnotationBeanPostProcessor extends ScheduledAnnotationBeanPostProcessor {

    private final RedisConnectionFactory factory;
    private final Integer corePoolSize;


    public JobScheduledAnnotationBeanPostProcessor(final RedisConnectionFactory factory, final Integer corePoolSize) {
        super();
        Objects.requireNonNull(factory, "redis factory is null");
        this.factory = factory;
        this.corePoolSize = corePoolSize;
        initExecutor();
    }

    private void initExecutor() {
        final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(this.corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build());
        setScheduler(scheduledExecutorService);
    }

    @Override
    protected Runnable createRunnable(final Object target, final Method method) {
        final JobScheduledLock scheduledLock = method.getAnnotation(JobScheduledLock.class);
        final Method invocableMethod = AopUtils.selectInvocableMethod(method, target.getClass());
        final ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(target, invocableMethod);
        if (scheduledLock == null) {
            return runnable;
        }
        return new JobScheduledMethodRunnable(runnable, this.factory, scheduledLock);
    }
}
