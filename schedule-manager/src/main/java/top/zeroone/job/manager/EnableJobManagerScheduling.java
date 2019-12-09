package top.zeroone.job.manager;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(JobManagerSchedulingConfiguration.class)
@Documented
public @interface EnableJobManagerScheduling {
}
