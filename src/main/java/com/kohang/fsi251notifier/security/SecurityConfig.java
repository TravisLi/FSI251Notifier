package com.kohang.fsi251notifier.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String username;
    private final String password;

    public SecurityConfig(@Value("${web.user}") String username,
                          @Value("${web.password}") String password) {
        this.username = username.strip();
        this.password = password.strip();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .defaultSuccessUrl("/manual")
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout.permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService users() {

        UserDetails user = User.builder()
                .username(username)
                .password(password)
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

}
