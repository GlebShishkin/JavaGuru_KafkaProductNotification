package by.javaguru.ws.handler;

import by.javaguru.ws.core.ProductCreatedEvent;
import by.javaguru.ws.exception.NonRetryableException;
import by.javaguru.ws.exception.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {

    private final RestTemplate restTemplate;  // для отправки сообщений в др. микосервис по url

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public ProductCreatedEventHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // получаем сообщение
    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent) {

        // читаем сообщения из топика
        LOGGER.info("Received event: {}", productCreatedEvent.getTitle());

        // сообщения будем отправлять на mock-микросервис
        try {
            // здесь в коде проптсывает REST сообщения на разные адреса ("/200", "/500" или на время делаем недоступным mock-сервис)
            String url = "http://localhost:8090/response/200"; // успешно отрабатывается
            //String url = "http://localhost:8090/response/500"; // HttpServerErrorException -> вызывае неповторяющуюся ошибку
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                // ответ от mock-микросервиса пришел нормально
                LOGGER.info("Received response: {}", response.getBody());
            }
        } catch (ResourceAccessException e) {
            // ResourceAccessException: url-ресурс недоступен -> неплохо бы повторить к нему запрос -> вызываем исключение для повтора (RetryableException)
            LOGGER.error(e.getMessage());
            throw new RetryableException(e);
        }
        catch (HttpServerErrorException e) {
            // HttpServerErrorException: внутренняя ошибка сервера -> повтор бессмысленнен -> вызываем исключение без повтора (NonRetryableException)
            LOGGER.error(e.getMessage());
            throw new NonRetryableException(e);
        }
        catch (Exception e) {
            // общую ошибку нет смысла повторять
            LOGGER.error(e.getMessage());
            throw new NonRetryableException(e);
        }
    }
}
