package com.dream.dream.config;


import com.dream.dream.jwt.JwtAccessDeniedHandler;
import com.dream.dream.jwt.JwtAuthenticationEntryPoint;
import com.dream.dream.jwt.JwtSecurityConfig;
import com.dream.dream.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private final CorsConfig corsConfig;

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);
        http.httpBasic().disable()
                .csrf().disable()
                .headers().frameOptions().disable()// crsf 보안 X (rest api)
                .and()
                .apply(new MyCustomDsl())
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // jwt token으로 인증 > 세션 필요없음

        http.authorizeHttpRequests((req) -> req
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/members/register")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/members/login")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/members/email")).permitAll().requestMatchers(mvcMatcherBuilder.pattern("/api/members/refresh")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/members/nickname")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/h2-console/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/favicon.ico")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/error")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/swagger-ui/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/swagger-resources/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/v3/api-docs/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/image/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern("/api/diary")).permitAll()
                        .anyRequest().authenticated()
//                        .anyRequest().permitAll()
                )   // 다음 리퀘스트에 대한 사용권한 체크
//                .requestMatchers( "/api/user/login","/api/user/signup").permitAll() // 허용된 주소
//                .requestMatchers(
//                        "/h2-console/**",
//                        "/favicon.ico",
//                        "/error",
//                        "/swagger-ui/**",
//                        "/swagger-resources/**",
//                        "/v3/api-docs/**").permitAll()
//                .anyRequest().authenticated() // Authentication 필요한 주소
//                .and()                  // exception handling for jwt
                .exceptionHandling()
                .accessDeniedHandler(jwtAccessDeniedHandler)
                .authenticationEntryPoint(jwtAuthenticationEntryPoint);

        // jwt 적용
        http.apply(new JwtSecurityConfig(jwtTokenProvider));
        return http.build();
    }

    // 커스텀 필터 추가를 여기서 처리하기
    public class MyCustomDsl extends AbstractHttpConfigurer<MyCustomDsl, HttpSecurity> {
        @Override
        public void configure(HttpSecurity http) throws Exception {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
            http
                    // cors 오류를 해결하기 위해 Controller 에 @CrossOrigin 을 붙여주는 방법도 있지만
                    // 이 방식은 필터 추가와 다르게 인증이 필요 없는 url 만 처리해줌
                    .addFilter(corsConfig.corsFilter()); // cors 에 대해 허락하는 필터
            // .addFilter(new JwtAuthorizationFilter(authenticationManager));
        }
    }
}
