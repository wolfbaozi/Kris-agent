package com.kris.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口
 *
 * 【前端类比】相当于前端的 main.ts —— createApp().mount('#app')
 * @SpringBootApplication 是一个组合注解，等价于：
 *   - @Configuration     -> 可以声明 Bean（类似 Vue 里 app.use()）
 *   - @EnableAutoConfig  -> Spring Boot 自动根据依赖配置（类似 Vite 自动加载插件）
 *   - @ComponentScan     -> 自动扫描当前包及子包下的所有组件
 *
 * @MapperScan 告诉 MyBatis-Plus 去哪里找 Mapper 接口（类似告诉 ORM 去哪里找 Model 定义）
 */
@SpringBootApplication
@MapperScan("com.kris.agent.mapper")
public class KrisAgentApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 做三件事：
        // 1. 创建 Spring 容器（IoC 容器，类似前端的 provide/inject 全局状态树）
        // 2. 自动配置所有组件（Controller、Service、Config 等）
        // 3. 启动内嵌 Tomcat 服务器监听端口
        SpringApplication.run(KrisAgentApplication.class, args);
    }
}
