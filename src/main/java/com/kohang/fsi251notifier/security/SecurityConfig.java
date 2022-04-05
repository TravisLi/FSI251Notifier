package com.kohang.fsi251notifier.security;

import com.kohang.fsi251notifier.mongo.MongoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	private final String username;
	private final String password;
	
	public SecurityConfig(@Value("#{systemProperties['web.user']!=null && systemProperties['web.user']!='' ? systemProperties['web.user'] : systemEnvironment['web_user']}"
	)String username,
						  @Value("#{systemProperties['web.password']!=null && systemProperties['web.password']!='' ? systemProperties['web.password'] : systemEnvironment['web_password']}"
						  )String password) {
		super();

		this.username = username.strip();
		this.password = password.strip();
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
