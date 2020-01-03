package top.zeroone.job.manager.model;

import lombok.Data;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.Task;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Data
public class TaskDescription {

    private TaskType taskType;
    private RunnableDescription runnable;

    private long initialDelay;
    private long interval;

    private String expression;
    String trigger;

    private static final Map<Class<? extends Task>, Function<Task, TaskDescription>> DESCRIBERS = new LinkedHashMap<>();

    static {
        DESCRIBERS.put(FixedRateTask.class, (task) -> fixedRate((FixedRateTask) task));
        DESCRIBERS.put(FixedDelayTask.class, (task) -> fixedDelay((FixedDelayTask) task));
        DESCRIBERS.put(CronTask.class, (task) -> cron((CronTask) task));
        //    DESCRIBERS.put(TriggerTask.class, (task) -> triggerTask((TriggerTask) task));
    }

    public static TaskDescription of(final Task task) {
        return DESCRIBERS.entrySet().stream().filter((entry) -> entry.getKey().isInstance(task))
                .map((entry) -> entry.getValue().apply(task)).findFirst().orElse(null);
    }

    private static TaskDescription fixedRate(final FixedRateTask task) {
        final TaskDescription description = new TaskDescription();
        description.setTaskType(TaskType.FIXED_RATE);
        description.setInitialDelay(task.getInitialDelay());
        description.setInterval(task.getInterval());
        description.setRunnable(new RunnableDescription(task.getRunnable()));
        return description;
    }

    private static TaskDescription fixedDelay(final FixedDelayTask task) {
        final TaskDescription description = new TaskDescription();
        description.setTaskType(TaskType.FIXED_DELAY);
        description.setInitialDelay(task.getInitialDelay());
        description.setInterval(task.getInterval());
        description.setRunnable(new RunnableDescription(task.getRunnable()));
        return description;
    }

    private static TaskDescription cron(final CronTask task) {
        final TaskDescription description = new TaskDescription();
        description.setTaskType(TaskType.CRON);
        description.setExpression(task.getExpression());
        description.setRunnable(new RunnableDescription(task.getRunnable()));
        return description;
    }

    public enum TaskType {
        CRON, CUSTOM_TRIGGER, FIXED_DELAY, FIXED_RATE
    }
}
