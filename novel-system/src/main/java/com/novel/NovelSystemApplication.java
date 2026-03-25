package com.novel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 小说系统应用主类
 * 继承SpringBootServletInitializer以支持WAR部署
 */
@SpringBootApplication
public class NovelSystemApplication extends SpringBootServletInitializer {

    /**
     * 应用程序主入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NovelSystemApplication.class, args);
    }
    
    /**
     * 配置Spring应用构建器
     * 用于WAR部署时初始化应用
     * @param application Spring应用构建器
     * @return 配置后的应用构建器
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(NovelSystemApplication.class);
    }
}
