package com.KimStock.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 허용할 출처
        config.addAllowedOrigin("http://localhost:3000");
        
        // 허용할 HTTP 메서드
        config.addAllowedMethod("*");
        
        // 허용할 헤더
        config.addAllowedHeader("*");
        
        // 인증 정보 포함 여부
        config.setAllowCredentials(true);
        
        // 설정을 모든 경로에 적용
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
