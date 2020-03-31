package top.zeroone.job.admin.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import top.zeroone.job.manager.model.TaskDescription;

import java.util.List;

/**
 * @author songyang
 */
public class JobController {



    @PostMapping("reportTask/{id}")
    public void reportTask(@RequestBody List<TaskDescription> taskDescriptions, @PathVariable String id) {


    }
}

// 上报,执行日志