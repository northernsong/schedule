package top.zeroone.job.manager;

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


    enum Result {
        // 执行成功
        success,
        // 执行失败
        fail,
        // 未执行
        nonExecution;
    }
}