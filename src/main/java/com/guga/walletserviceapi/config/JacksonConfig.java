package com.guga.walletserviceapi.config;

import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.guga.walletserviceapi.helpers.GlobalHelper;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .simpleDateFormat(GlobalHelper.PATTERN_FORMAT_DATE_TIME)
                .serializers(new LocalDateTimeSerializer(
                        DateTimeFormatter.ofPattern(GlobalHelper.PATTERN_FORMAT_DATE_TIME)))
                .deserializers(new LocalDateTimeDeserializer(
                        DateTimeFormatter.ofPattern(GlobalHelper.PATTERN_FORMAT_DATE_TIME)));
    }
    
    @Bean
    public Hibernate6Module hibernateModule() {
        Hibernate6Module module = new Hibernate6Module();

        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);

        return module;
    }


}
