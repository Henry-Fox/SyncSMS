package com.syncsms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.syncsms.mapper")
public class SyncSmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncSmsApplication.class, args);
    }
}
