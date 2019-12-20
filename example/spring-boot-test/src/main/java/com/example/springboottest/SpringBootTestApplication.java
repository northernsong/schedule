package com.example.springboottest;

import com.netflix.discovery.EurekaClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import top.zeroone.job.annotation.EnableJobScheduling;
import top.zeroone.job.annotation.JobScheduledLock;
import top.zeroone.job.manager.EnableJobManagerScheduling;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableJobManagerScheduling
public class SpringBootTestApplication {

    @Autowired
    private EurekaClientConfig eurekaClientConfig;

    public static void main(final String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }


    @Scheduled(cron = "0/5 * * * * ?")
    @JobScheduledLock(lockSecond = 2)
    public void cron(final String i) throws InterruptedException {
        System.out.println();
        System.out.println("************");
        System.out.println("Schedule Test cron start" + i);
        System.out.println(LocalDateTime.now());
        Thread.sleep(2000);
        System.out.println("Schedule Test cron end");
        System.out.println("************");
        System.out.println();
    }


    @Scheduled(fixedRate = 10000)
    @JobScheduledLock(lockSecond = 2, id = "fixedTest")
    public void fixedTest() throws InterruptedException {
        System.out.println();
        System.out.println("************");
        System.out.println("Schedule Test fixedTest start");
        System.out.println(LocalDateTime.now());
        System.out.println("Schedule Test fixedTest end");
        System.out.println("************");
        System.out.println();
    }

    @Scheduled(fixedDelay = 13000)
    @JobScheduledLock(lockSecond = 2, id = "fixedDelay")
    public void fixedDelay() throws InterruptedException {
        System.out.println();
        System.out.println("************");
        System.out.println("Schedule Test fixedDelay start");
        System.out.println(LocalDateTime.now());
        System.out.println("Schedule Test fixedDelay end");
        System.out.println("************");
        System.out.println();
    }
}