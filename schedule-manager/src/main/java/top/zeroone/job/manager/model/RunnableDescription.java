package top.zeroone.job.manager.model;

import lombok.Data;
import top.zeroone.job.annotation.JobScheduledLock;
import top.zeroone.job.manager.JobReportScheduledMethodRunnable;

@Data
public class RunnableDescription {

    private String target;
    private String jobDesc = "";
    private String id;

    public RunnableDescription(final Runnable runnable) {
        if (runnable instanceof JobReportScheduledMethodRunnable) {
            final JobReportScheduledMethodRunnable job = (JobReportScheduledMethodRunnable) runnable;
            this.target = job.getMethod().getDeclaringClass().getName() + "." + job.getMethod().getName();
            this.id = job.getName();
            final JobScheduledLock lock = job.getMethod().getAnnotation(JobScheduledLock.class);
            if (lock != null) {
                this.jobDesc = String.format("id: %s, lockSecond: %d, ifLockDoNot: %s", lock.id(), lock.lockSecond(), lock.ifLockDoNot());
            }
        } else {
            this.target = runnable.getClass().getName();
        }
    }
}
