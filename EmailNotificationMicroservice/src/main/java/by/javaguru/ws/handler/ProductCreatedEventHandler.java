package by.javaguru.ws.handler;

import by.javaguru.ws.core.ProductCreatedEvent;
import by.javaguru.ws.exception.NonRetryableException;
import by.javaguru.ws.exception.RetryableException;
import by.javaguru.ws.persistence.entity.ProcessedEventEntity;
import by.javaguru.ws.persistence.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestTemplate restTemplate;  // для отправки сообщений в др. микосервис по url
    private final ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    // получаем сообщение
    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,    // @Payload - event читаем из тела
                    @Header("messageId") String messageId,  // id собщения читаем из заголовка
                    @Header(KafkaHeaders.RECEIVED_KEY)   String messageKey   // KafkaHeaders.RECEIVED_KEY - спринг. константа содержт ключ (key) сообщения
                    ) {

        // читаем сообщения из топика
        LOGGER.info("Received event: {}", productCreatedEvent.getTitle());

        ProcessedEventEntity processedEventEntity = processedEventRepository.findByMessageId(messageId);

        if(processedEventEntity != null) {
            LOGGER.info("Duplicate message id: {}", messageId);
            // id сообщения найден в бд связанной с консьюмером -> значит второй раз не будем создавать продукт ->
            // не будем отправлять rest-сообщение (бизнес-логика) в mock-сервис
            return;
        }

        // сообщения будем отправлять на mock-микросервис
        try {
            // здесь в коде пропbисывает REST сообщения на разные адреса mock-сервиса ("/200", "/500" или на время делаем недоступным mock-сервис)
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

        try {
            // сохраняем в бд messageId в качестве защиты от задваивания productCreatedEvent
            processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException e) {
            LOGGER.error(e.getMessage());
            throw new NonRetryableException(e);
        }

    }
}
