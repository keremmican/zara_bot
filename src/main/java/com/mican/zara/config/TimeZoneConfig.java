package com.mican.zara.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            // Uygulama genelinde UTC +3 zaman dilimini ayarla
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
        };
    }
}