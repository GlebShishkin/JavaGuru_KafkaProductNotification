package by.javaguru.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/// //////////////////////////////////
/// Конфиг REST:
/// при получении консьюмером сообщения из Kafka он будет посылать REST-сообщение по url в mock-микросервис
/// //////////////////////////////////
@Configuration
public class EmailNotificationConfig {


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
