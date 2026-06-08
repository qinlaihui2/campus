package com.campus.file.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String DOCUMENT_PROCESS_QUEUE = "document.process.queue";
    public static final String DOCUMENT_PROCESS_DLQ = "document.process.dlq";

    @Bean
    public Queue documentProcessQueue() {
        return new Queue(DOCUMENT_PROCESS_QUEUE, true);
    }

    @Bean
    public Queue documentProcessDlq() {
        return new Queue(DOCUMENT_PROCESS_DLQ, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
