package com.test.crawler;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ImportResource("classpath:spring-config.xml")
@EnableWebMvc
public class Configurations {
    @Bean
    @Order(1)
    public FilterRegistrationBean characterEncodingFilterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setName("CharacterEncodingFilter");
        CharacterEncodingFilter filter = new CharacterEncodingFilter("UTF-8", true);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(1);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
