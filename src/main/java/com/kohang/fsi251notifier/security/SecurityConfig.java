package com.kohang.fsi251notifier.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final String username;
	private final String password;
	
	public SecurityConfig(@Value("${web_user}")String username, @Value("${web_password}")String password) {
		super();
		this.username = username;
		this.password = password;
	}
	
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin().defaultSuccessUrl("/manual")
                .loginPage("/login").permitAll()
                .and()
                .logout().permitAll();
        		
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
