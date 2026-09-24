package com.linweiyun.minegenshin.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** MineGenshin 文档站入口。与 Mod 本体完全隔离：独立构建、独立依赖、独立进程。 */
@SpringBootApplication
public class SiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiteApplication.class, args);
    }
}
