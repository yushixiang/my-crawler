package com.test.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.system.ApplicationPidFileWriter;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        JooqAutoConfiguration.class})
@Slf4j
public class CrawlerApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(CrawlerApplication.class);
        springApplication.addListeners(new ApplicationPidFileWriter("crawler.pid"));
        try {
            springApplication.run(args);
        } catch (Throwable t) {
            log.error("startup exception", t);
        }
    }
}
