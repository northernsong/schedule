package top.zeroone.job.manager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.*;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import top.zeroone.job.annotation.JobScheduledLock;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author songyang
 */
@Configuration
@Endpoint(id = "schedule-manager")
public class JobManagerSchedulingConfigurer implements SchedulingConfigurer {

    private ScheduledTaskRegistrar registrar;
    private final Collection<ScheduledTaskHolder> scheduledTaskHolders;

    private final Map<String, RunnableDescription> taskMap = new HashMap<>(64);

    public JobManagerSchedulingConfigurer(final Collection<ScheduledTaskHolder> scheduledTaskHolders) {
        this.scheduledTaskHolders = scheduledTaskHolders;
    }

    @Override
    public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
        this.registrar = taskRegistrar;
    }

    @ReadOperation
    public ScheduledTasksReport readTasks() {
        final Map<TaskType, List<TaskDescription>> descriptionsByType = this.scheduledTaskHolders.stream()
                .flatMap((holder) -> holder.getScheduledTasks().stream()).map(ScheduledTask::getTask)
                .map(TaskDescription::of).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(TaskDescription::getType));
        return new ScheduledTasksReport(descriptionsByType);
    }

    @ReadOperation
    public ScheduledTask readTask(@Selector final String id) {
        return this.scheduledTaskHolders.stream()
                .flatMap((holder) -> holder.getScheduledTasks().stream())
                .filter(scheduledTask -> {
                    RunnableDescription description = new RunnableDescription(scheduledTask.getTask().getRunnable());
                    return description.getId().equalsIgnoreCase(id);
                })
                .findFirst().orElse(null);
    }

    @WriteOperation
    public ScheduledTasksReport stopTask(final String id) {
        final Map<TaskType, List<TaskDescription>> descriptionsByType = this.scheduledTaskHolders.stream()
                .flatMap((holder) -> holder.getScheduledTasks().stream())
                .filter(scheduledTask -> {
                    RunnableDescription description = new RunnableDescription(scheduledTask.getTask().getRunnable());
                    return description.getId().equalsIgnoreCase(id);
                })
                .filter(scheduledTask -> {
                    scheduledTask.cancel();
                    return true;
                })
                .map(ScheduledTask::getTask)
                .map(TaskDescription::of).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(TaskDescription::getType));

        return new ScheduledTasksReport(descriptionsByType);
    }


    /**
     * A report of an application's scheduled {@link Task Tasks}, primarily intended for
     * serialization to JSON.
     */
    public static final class ScheduledTasksReport {

        private final List<TaskDescription> cron;

        private final List<TaskDescription> fixedDelay;

        private final List<TaskDescription> fixedRate;

        private final List<TaskDescription> custom;

        private ScheduledTasksReport(final Map<TaskType, List<TaskDescription>> descriptionsByType) {
            this.cron = descriptionsByType.getOrDefault(TaskType.CRON, Collections.emptyList());
            this.fixedDelay = descriptionsByType.getOrDefault(TaskType.FIXED_DELAY, Collections.emptyList());
            this.fixedRate = descriptionsByType.getOrDefault(TaskType.FIXED_RATE, Collections.emptyList());
            this.custom = descriptionsByType.getOrDefault(TaskType.CUSTOM_TRIGGER, Collections.emptyList());
        }

        public List<TaskDescription> getCron() {
            return this.cron;
        }

        public List<TaskDescription> getFixedDelay() {
            return this.fixedDelay;
        }

        public List<TaskDescription> getFixedRate() {
            return this.fixedRate;
        }

        public List<TaskDescription> getCustom() {
            return this.custom;
        }

    }

    /**
     * Base class for descriptions of a {@link Task}.
     */
    public abstract static class TaskDescription {

        private static final Map<Class<? extends Task>, Function<Task, TaskDescription>> DESCRIBERS = new LinkedHashMap<>();

        static {
            DESCRIBERS.put(FixedRateTask.class, (task) -> new FixedRateTaskDescription((FixedRateTask) task));
            DESCRIBERS.put(FixedDelayTask.class, (task) -> new FixedDelayTaskDescription((FixedDelayTask) task));
            DESCRIBERS.put(CronTask.class, (task) -> new CronTaskDescription((CronTask) task));
            DESCRIBERS.put(TriggerTask.class, (task) -> describeTriggerTask((TriggerTask) task));
        }

        private static TaskDescription of(final Task task) {
            return DESCRIBERS.entrySet().stream().filter((entry) -> entry.getKey().isInstance(task))
                    .map((entry) -> entry.getValue().apply(task)).findFirst().orElse(null);
        }

        private static TaskDescription describeTriggerTask(final TriggerTask triggerTask) {
            final Trigger trigger = triggerTask.getTrigger();
            if (trigger instanceof CronTrigger) {
                return new CronTaskDescription(triggerTask, (CronTrigger) trigger);
            }
            if (trigger instanceof PeriodicTrigger) {
                final PeriodicTrigger periodicTrigger = (PeriodicTrigger) trigger;
                if (periodicTrigger.isFixedRate()) {
                    return new FixedRateTaskDescription(triggerTask, periodicTrigger);
                }
                return new FixedDelayTaskDescription(triggerTask, periodicTrigger);
            }
            return new CustomTriggerTaskDescription(triggerTask);
        }

        private final TaskType type;

        private final RunnableDescription runnable;

        protected TaskDescription(final TaskType type, final Runnable runnable) {
            this.type = type;
            this.runnable = new RunnableDescription(runnable);
        }

        private TaskType getType() {
            return this.type;
        }

        public final RunnableDescription getRunnable() {
            return this.runnable;
        }

    }

    /**
     * A description of an {@link IntervalTask}.
     */
    public static class IntervalTaskDescription extends TaskDescription {

        private final long initialDelay;

        private final long interval;

        protected IntervalTaskDescription(final TaskType type, final IntervalTask task) {
            super(type, task.getRunnable());
            this.initialDelay = task.getInitialDelay();
            this.interval = task.getInterval();
        }

        protected IntervalTaskDescription(final TaskType type, final TriggerTask task, final PeriodicTrigger trigger) {
            super(type, task.getRunnable());
            this.initialDelay = trigger.getInitialDelay();
            this.interval = trigger.getPeriod();
        }

        public long getInitialDelay() {
            return this.initialDelay;
        }

        public long getInterval() {
            return this.interval;
        }

    }

    /**
     * A description of a {@link FixedDelayTask} or a {@link TriggerTask} with a
     * fixed-delay {@link PeriodicTrigger}.
     */
    public static final class FixedDelayTaskDescription extends IntervalTaskDescription {

        private FixedDelayTaskDescription(final FixedDelayTask task) {
            super(TaskType.FIXED_DELAY, task);
        }

        private FixedDelayTaskDescription(final TriggerTask task, final PeriodicTrigger trigger) {
            super(TaskType.FIXED_DELAY, task, trigger);
        }

    }

    /**
     * A description of a {@link FixedRateTask} or a {@link TriggerTask} with a fixed-rate
     * {@link PeriodicTrigger}.
     */
    public static final class FixedRateTaskDescription extends IntervalTaskDescription {

        private FixedRateTaskDescription(final FixedRateTask task) {
            super(TaskType.FIXED_RATE, task);
        }

        private FixedRateTaskDescription(final TriggerTask task, final PeriodicTrigger trigger) {
            super(TaskType.FIXED_RATE, task, trigger);
        }

    }

    /**
     * A description of a {@link CronTask} or a {@link TriggerTask} with a
     * {@link CronTrigger}.
     */
    public static final class CronTaskDescription extends TaskDescription {

        private final String expression;

        private CronTaskDescription(final CronTask task) {
            super(TaskType.CRON, task.getRunnable());
            this.expression = task.getExpression();
        }

        private CronTaskDescription(final TriggerTask task, final CronTrigger trigger) {
            super(TaskType.CRON, task.getRunnable());
            this.expression = trigger.getExpression();
        }

        public String getExpression() {
            return this.expression;
        }

    }

    /**
     * A description of a {@link TriggerTask} with a custom {@link Trigger}.
     *
     * @since 2.1.3
     */
    public static final class CustomTriggerTaskDescription extends TaskDescription {

        private final String trigger;

        private CustomTriggerTaskDescription(final TriggerTask task) {
            super(TaskType.CUSTOM_TRIGGER, task.getRunnable());
            this.trigger = task.getTrigger().toString();
        }

        public String getTrigger() {
            return this.trigger;
        }

    }


    public static final class ScheduleTaskDescription {
        private RunnableDescription runnableDescription;
        private Task task;

        public ScheduleTaskDescription(RunnableDescription runnableDescription, ScheduledFuture future) {
            this.runnableDescription = runnableDescription;
            this.task = task;
        }

        public RunnableDescription getRunnableDescription() {
            return runnableDescription;
        }

        public Task getTask() {
            return task;
        }
    }

    /**
     * A description of a {@link Task Task's} {@link Runnable}.
     *
     * @author Andy Wilkinson
     */
    public static final class RunnableDescription {

        private final String target;
        private String jobDesc = "";
        private String id;

        private RunnableDescription(final Runnable runnable) {
            if (runnable instanceof ScheduledMethodRunnable) {
                final Method method = ((ScheduledMethodRunnable) runnable).getMethod();
                this.target = method.getDeclaringClass().getName() + "." + method.getName();
                this.id = this.target;
                final JobScheduledLock lock = method.getAnnotation(JobScheduledLock.class);
                if (lock != null) {
                    this.jobDesc = String.format("id: %s, lockSecond: %d, ifLockDoNot: %s", lock.id(), lock.lockSecond(), lock.ifLockDoNot());
                    if (StringUtils.isNotBlank(lock.id())) {
                        this.id = lock.id();
                    }
                }
            } else {
                this.target = runnable.getClass().getName();
            }
        }

        public String getTarget() {
            return this.target;
        }

        public String getJobDesc() {
            return this.jobDesc;
        }

        public String getId() {
            return this.id;
        }
    }

    private enum TaskType {
        CRON, CUSTOM_TRIGGER, FIXED_DELAY, FIXED_RATE
    }
}
