package com.crosspay.common.config;

import com.crosspay.module.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置
 *
 * 权限模型很简单（学习用）：
 * - /api/auth/**        → 公开（注册、登录）
 * - /api/callback/**    → 公开（第三方回调不能要求 JWT）
 * - /api/admin/**       → 需要 ADMIN 角色
 * - /api/merchant/**    → 需要 MERCHANT 角色
 * - /api/payment/**     → 需要 MERCHANT 角色
 * - /api/settlement/**  → 需要 MERCHANT 角色
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfig()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/callback/**").permitAll()
                // 运营端（管理后台是独立登录的）
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 商户端
                .requestMatchers("/api/merchant/**").hasRole("MERCHANT")
                .requestMatchers("/api/payment/**").hasRole("MERCHANT")
                .requestMatchers("/api/settlement/**").hasRole("MERCHANT")
                // AI 助手（运营使用）
                .requestMatchers("/api/ai/**").hasRole("ADMIN")
                // 其他全部需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
