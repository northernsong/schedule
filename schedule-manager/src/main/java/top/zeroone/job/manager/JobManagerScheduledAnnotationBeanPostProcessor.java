package top.zeroone.job.manager;

import com.alibaba.fastjson.JSONObject;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import top.zeroone.job.annotation.JobScheduledAnnotationBeanPostProcessor;

import java.util.Set;

public class JobManagerScheduledAnnotationBeanPostProcessor extends JobScheduledAnnotationBeanPostProcessor implements SchedulingConfigurer {

    private ScheduledTaskRegistrar registrar;

    public JobManagerScheduledAnnotationBeanPostProcessor(final RedisConnectionFactory factory, final Integer corePoolSize) {
        super(factory, corePoolSize);
    }


    @Override
    public void afterSingletonsInstantiated() {
        super.afterSingletonsInstantiated();
        //getAllTask();
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        super.onApplicationEvent(event);
        getAllTask();
    }


    public void getAllTask() {
        Set<ScheduledTask> scheduledTasks = registrar.getScheduledTasks();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(JSONObject.toJSONString(scheduledTasks));
    }


    @Override
    public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
        this.registrar = taskRegistrar;
    }
}
