package top.zeroone.job.manager;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author songyang
 */
@Configuration
@Endpoint(id = "schedule-manager")
public class TaskManager implements SchedulingConfigurer {

    private ScheduledTaskRegistrar registrar;
    private final Collection<ScheduledTaskHolder> scheduledTaskHolders;

    private final Map<String, ScheduledTask> taskMap = new HashMap<>(64);

    public TaskManager(final Collection<ScheduledTaskHolder> scheduledTaskHolders) {
        this.scheduledTaskHolders = scheduledTaskHolders;
    }

    private static final char[] RANDOM_CHARS = "1234567890zxcvbnmasdfghjkl".toCharArray();

    public void initTaskMap() {
        this.scheduledTaskHolders.stream().flatMap((hold) -> hold.getScheduledTasks().stream())
                .forEach(task -> {
                    final boolean isJob = task.getTask().getRunnable() instanceof JobReportScheduledMethodRunnable;
                    if (!isJob) {
                        return;
                    }
                    final JobReportScheduledMethodRunnable job = (JobReportScheduledMethodRunnable) task.getTask().getRunnable();

                    if (this.taskMap.containsKey(job.getName())) {
                        final ScheduledTask old = this.taskMap.get(job.getName());
                        if (!old.equals(task)) {
                            this.taskMap.put(job.getName() + RandomStringUtils.random(4, RANDOM_CHARS), task);
                        }
                    } else {
                        this.taskMap.put(job.getName(), task);
                    }
                });
    }


    @ReadOperation
    public String readTasks() {

        if (taskMap.isEmpty()){
            initTaskMap();
        }

        final SimplePropertyPreFilter simplePropertyPreFilter = new SimplePropertyPreFilter(Task.class, "runnable");
        return JSONObject.toJSONString(this.taskMap, simplePropertyPreFilter);
    }


    @Override
    public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
        this.registrar = taskRegistrar;
    }
}