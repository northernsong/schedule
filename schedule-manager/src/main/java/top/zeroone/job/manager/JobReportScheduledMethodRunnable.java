package top.zeroone.job.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import top.zeroone.job.annotation.JobScheduledLock;
import top.zeroone.job.annotation.JobScheduledMethodRunnable;

@Slf4j
public class JobReportScheduledMethodRunnable extends JobScheduledMethodRunnable {

    private final JobReporter jobReporter;

    public JobReportScheduledMethodRunnable(final ScheduledMethodRunnable runnable, final RedisConnectionFactory factory, final JobScheduledLock scheduledLock, final JobReporter jobReporter) {
        super(runnable, factory, scheduledLock);
        this.jobReporter = jobReporter == null ? (id, applicationName, result, msg) -> {
            log.warn("没有实现上报的方法");
        } : jobReporter;
    }

    @Override
    public void run() {
        if (getScheduledLock().ifLockDoNot()) {
            if (getTimeLock() != null) {
                // 有些任务,短时间内不必重复执行.因没有等待执行,所以没有显式的解锁
                if (!getTimeLock().tryLock()) {
                    if (this.jobReporter != null) {
                        this.jobReporter.report(getName(), "", JobReporter.Result.nonExecution, "redis时间锁未过期,无法执行");
                    }
                    log.debug("任务{}, time tryLock return false, 未执行", getName());
                    return;
                }
            }

            if (getLock().tryLock()) {
                try {
                    invokeMethod();
                    this.jobReporter.report(getName(), "", JobReporter.Result.success, null);
                } catch (final Exception e) {
                    this.jobReporter.report(getName(), "", JobReporter.Result.fail, e.getMessage());
                    throw e;
                } finally {
                    getLock().unlock();
                }
            } else {
                this.jobReporter.report(getName(), "", JobReporter.Result.nonExecution, "trylock 未获取到锁, 无法执行");
                log.debug("任务{}, tryLock return false, 停止执行", getName());
            }
        } else {

            try {
                invokeMethod();
                this.jobReporter.report(getName(), "", JobReporter.Result.success, null);
            } catch (final Exception e) {
                this.jobReporter.report(getName(), "", JobReporter.Result.fail, e.getMessage());
                throw e;
            }
        }
    }
}
