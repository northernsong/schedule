package top.zeroone.job.manager;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import top.zeroone.job.annotation.JobScheduledLock;
import top.zeroone.job.manager.model.TaskDescription;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

public class JobManager extends ScheduledAnnotationBeanPostProcessor {


    private final RedisConnectionFactory factory;
    private final Integer corePoolSize;
    private final JobReporter jobReporter;

    @Value("${spring.application.name:}")
    private String applicationName;

    public JobManager(final RedisConnectionFactory factory, final Integer corePoolSize, final JobReporter jobReporter) {
        super();
        this.jobReporter = jobReporter;
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
        return new JobReportScheduledMethodRunnable(runnable, this.factory, scheduledLock, this.jobReporter);
    }


    @Override
    public void afterSingletonsInstantiated() {
        super.afterSingletonsInstantiated();

        final List<TaskDescription> list = getScheduledTasks().stream().map(ScheduledTask::getTask).map(TaskDescription::of).collect(Collectors.toList());
        this.jobReporter.reportAllTask(list);
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        super.onApplicationEvent(event);

        final List<TaskDescription> list = getScheduledTasks().stream().map(ScheduledTask::getTask).map(TaskDescription::of).collect(Collectors.toList());
        this.jobReporter.reportAllTask(list);
    }
}
