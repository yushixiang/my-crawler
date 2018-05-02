package com.test.crawler.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPoolUtils {
    public static void stop(ExecutorService service){
        if(null != service) {
            int tryTime = 3;
            service.shutdown();
            while (tryTime-- > 0) {
                try {
                    service.awaitTermination(1, TimeUnit.SECONDS);
                    if (service.isTerminated()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    log.error("terminat error.", e);
                }
            }
        }
    }
}
