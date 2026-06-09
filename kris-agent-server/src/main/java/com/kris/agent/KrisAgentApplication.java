package com.kris.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kris.agent.mapper")
public class KrisAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(KrisAgentApplication.class, args);
    }
}
