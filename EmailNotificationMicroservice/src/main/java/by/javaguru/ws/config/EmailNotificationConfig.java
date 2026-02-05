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


    // без явного разрешения отсутствует доступ к адресу бд через браузер: "http://localhost:8084"
    /*
    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .formLogin(withDefaults());

        return http.build();
    }
    */


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
