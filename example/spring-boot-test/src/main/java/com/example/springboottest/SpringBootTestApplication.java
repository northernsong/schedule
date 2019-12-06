package com.example.springboottest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Scheduled;
import top.zeroone.job.annotation.EnableJobScheduling;
import top.zeroone.job.annotation.JobScheduledLock;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableJobScheduling
public class SpringBootTestApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }

    @Scheduled(cron = "0/5 * * * * ?")
    @JobScheduledLock(lockSecond = 2)
    public void cron(String i) throws InterruptedException {
        System.out.println();
        System.out.println("************");
        System.out.println("Schedule Test cron start" + i);
        System.out.println(LocalDateTime.now());
        Thread.sleep(2000);
        System.out.println("Schedule Test cron end");
        System.out.println("************");
        System.out.println();
    }
}