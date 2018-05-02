package com.test.crawler.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "")
@Slf4j
public class PingController {
    /**
     * 测试服务是否ok的
     *
     * @return
     */
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public Object ping() throws Throwable {
        log.info("pong");
        return "OK";
    }
}
