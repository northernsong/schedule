package top.zeroone.job.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(JobSchedulingConfiguration.class)
@Documented
public @interface EnableJobScheduling {
}
