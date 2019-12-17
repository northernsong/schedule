package top.zeroone.job.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import top.zeroone.job.annotation.JobScheduledAnnotationBeanPostProcessor;

public class JobManager extends JobScheduledAnnotationBeanPostProcessor implements SchedulingConfigurer {

    private ScheduledTaskRegistrar registrar;

    private JobReporter jobReporter;
    //private final Collection<ScheduledTaskHolder> scheduledTaskHolders;

    @Value("${spring.application.name:}")
    private String applicationName;

    public JobManager(final RedisConnectionFactory factory, final Integer corePoolSize) {
        super(factory, corePoolSize);
    }

    // public JobManager(final Collection<ScheduledTaskHolder> scheduledTaskHolders) {
    //     this.scheduledTaskHolders = scheduledTaskHolders;
    // }

    @Override
    public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
        this.registrar = taskRegistrar;
    }
}
