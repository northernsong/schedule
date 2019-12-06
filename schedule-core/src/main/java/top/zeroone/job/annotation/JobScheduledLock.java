package top.zeroone.job.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobScheduledLock {


    String id() default "";

    /**
     * 当某个点执行了任务后,在 lockSecond 时间内,是不允许其他线程执行的
     * <p>
     * 如果配置了这个字段,则在任务开始执行后,不会执行unlock(),直到时间到后,锁自动失效
     *
     * @return 0
     */
    long lockSecond() default 0;

    // /**
    //  * 等待锁的时间
    //  *
    //  * @return
    //  */
    // long tryLockWaitTime() default 0;


    /**
     * 如果其他点执行了,就不再继续执行了
     * <p>
     * 默认就是不在执行
     *
     * @return true
     */
    boolean ifLockDoNot() default true;
}
