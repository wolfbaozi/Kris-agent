package com.kris.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置
 *
 * 【前端类比】相当于 Vite 的 public 目录或 Express 的 express.static('uploads')
 * 把磁盘上的 uploads/ 目录映射为 HTTP 可访问的 /uploads/ 路径
 * 这样前端就可以通过 URL 直接访问上传的文件
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath + "/");
    }
}
