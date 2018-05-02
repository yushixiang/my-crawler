package com.test.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrawlerResult implements Serializable {
    private static final long serialVersionUID = 2424116524131940971L;
    @JsonProperty(value = "user_id")
    private int userId;
}
