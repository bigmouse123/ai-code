package com.jiankun.aicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jiankun.aicode.mapper")
public class KunAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(KunAiCodeMotherApplication.class, args);
    }

}
