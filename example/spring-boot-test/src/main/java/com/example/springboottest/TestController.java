package com.example.springboottest;

import com.alibaba.fastjson.JSONObject;
import com.netflix.discovery.EurekaClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {


    @Autowired
    private EurekaClientConfig eurekaClientConfig;


    @Autowired
    private EurekaInstanceConfigBean eurekaInstanceConfigBean;

    @GetMapping("eurekaConfig")
    public String config() {
        return JSONObject.toJSONString(this.eurekaClientConfig);
    }


    @GetMapping("eurekaInstance")
    public String eurekaInstance() {
        return JSONObject.toJSONString(this.eurekaInstanceConfigBean);
    }

}
