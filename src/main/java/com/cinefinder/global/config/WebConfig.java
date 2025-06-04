package com.cinefinder.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // CORS를 적용할 경로
                .allowedOrigins("*")  // 허용할 origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")  // 허용할 HTTP 메서드
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With")  // 허용할 헤더
                .maxAge(360000);  // preflight 요청 결과를 캐시할 시간(초)
    }
}
