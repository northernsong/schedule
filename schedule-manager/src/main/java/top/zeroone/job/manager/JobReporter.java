package top.zeroone.job.manager;

import top.zeroone.job.manager.model.TaskDescription;

import java.util.List;

public interface JobReporter {

    /**
     * 上报任务执行结果
     *
     * @param id              任务id
     * @param applicationName 实例名称
     * @param result          结果
     * @param msg             原因
     */
    void report(String id, String applicationName, Result result, String msg);

    /**
     * 上报所有任务信息
     * @param descriptions 任务描述
     */
    default void reportAllTask(List<TaskDescription> descriptions) {

    }


    enum Result {
        // 执行成功
        success,
        // 执行失败
        fail,
        // 未执行
        nonExecution;
    }
}