package by.javaguru.ws.productmicroservice.service;

import by.javaguru.ws.core.ProductCreatedEvent;
import by.javaguru.ws.productmicroservice.service.dto.CreateProductDto;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ProductServiceImpl implements ProductService {

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    // Создание в топики Record-а о новом продукте с event-ом в теле и header-ом (такой вариант соответствует 21 уроку)
    // В header-е прописываем id сообщения, кот будем записывать в бд для защиты от задвоения
    public String createProduct(CreateProductDto createProductDto)
            throws ExecutionException   // возникает, когда задача, выполняемая асинхронно, завершается с ошибкой
            , InterruptedException {
        //TODO save DB
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, createProductDto.getTitle(),
                createProductDto.getPrice(), createProductDto.getQuantity());

        // record для отправки в топик, к кот. далее добавим header, где разместим id сообщения
        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>(
                "product-created-events-topic", productId, productCreatedEvent
            );

        // в header record-а добавим id сообщения
        record.headers().add("messageId", UUID.randomUUID().toString().getBytes());

        // будем отправлять record вместо ProductCreatedEvent (т.е. ProductCreatedEvent + header)
        SendResult<String, ProductCreatedEvent> result = kafkaTemplate
                .send(record).get();

        LOGGER.info("Topic: {}", result.getRecordMetadata().topic());
        LOGGER.info("Partition: {}", result.getRecordMetadata().partition());
        LOGGER.info("Offset: {}", result.getRecordMetadata().offset());

        LOGGER.info("Return: {}", productId);

        return productId;
    }


    // асинхронный вариант без посылки заголовка (такой вариант соответствует 19 уроку и сохранился на github.com/AndreiBor/KafkaProductNotification)
    public String createProductAsinhron(CreateProductDto createProductDto)
            throws ExecutionException   // возникает, когда задача, выполняемая асинхронно, завершается с ошибкой
            , InterruptedException {
        //TODO save DB
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, createProductDto.getTitle(),
                createProductDto.getPrice(), createProductDto.getQuantity());

        // асинхронная передача сообщения в kafka
        CompletableFuture<SendResult<String, ProductCreatedEvent>> future = kafkaTemplate
                .send("product-created-events-topic", productId, productCreatedEvent);

        // whenComplete - отработает, когда придет ответ от брокера
        future.whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to send message: {}", exception.getMessage());
                    } else {
//                        LOGGER.info("Message send succefully: {}": result.getProducerRecord());
                    }
                }
        );

        LOGGER.info("Return {}", productId);

        return productId;
    }
}
