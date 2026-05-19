package com.edu.live;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.edu.live.mapper")
public class LivePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(LivePlatformApplication.class, args);
    }
}
