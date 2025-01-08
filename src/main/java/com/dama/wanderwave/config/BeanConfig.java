package com.dama.wanderwave.config;


import com.dama.wanderwave.comment.Comment;
import com.dama.wanderwave.place.Place;
import com.dama.wanderwave.post.response.AccountInfoResponse;
import com.dama.wanderwave.post.response.CommentResponse;
import com.dama.wanderwave.post.response.PlaceResponse;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Properties;


@Configuration
@RequiredArgsConstructor
public class BeanConfig {

    private final UserDetailsService userDetailsService;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<Place, PlaceResponse>() {
            @Override
            protected void configure() {
                map().setDescription(source.getDescription());
                map().setRating(source.getRating());
                map().getCoords().setLatitude(source.getLatitude());
                map().getCoords().setLongitude(source.getLongitude());
            }
        });

        modelMapper.addMappings(new PropertyMap<User, UserResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setNickname(source.getNickname());
                map().setEmail(source.getEmail());
                map().setDescription(source.getDescription());
                map().setSubscriberCount(source.getSubscriberCount());
                map().setSubscriptionsCount(source.getSubscriptionsCount());
                map().setAvatarUrl(source.getImageUrl());
            }
        });

        modelMapper.addMappings(new PropertyMap<Comment, CommentResponse>() {
            @Override
            protected void configure() {
                map().setText(source.getContent());
                map().setCreationDate(source.getCreatedAt());
                map(source.getUser(), destination.getAccountInfo());
            }
        });

        modelMapper.addMappings(new PropertyMap<User, AccountInfoResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setNickname(source.getNickname());
                map().setImageUrl(source.getImageUrl());
            }
        });

        return modelMapper;
    }
}

