package com.startingblue.fourtooncookie.config;

import com.startingblue.fourtooncookie.config.authentication.AuthenticationInterceptor;
import com.startingblue.fourtooncookie.member.authorization.MemberAuthorizationInterceptor;
import com.startingblue.fourtooncookie.member.dto.MemberArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class AuthConfig implements WebMvcConfigurer {

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;
    @Autowired
    private MemberAuthorizationInterceptor memberAuthorizationInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new MemberArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(memberAuthorizationInterceptor)
                .addPathPatterns("/member/**");
    }
}
