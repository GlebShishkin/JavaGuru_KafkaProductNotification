package by.javaguru.ws.config;

//import com.fasterxml.jackson.databind.JsonSerializer;
import by.javaguru.ws.exception.NonRetryableException;
import by.javaguru.ws.exception.RetryableException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;


//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
//import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;

// !!! использование StringDeserializer.class и JsonDeserializer.class из github.com/AndreiBor/KafkaProductNotification
// не работает, т.к. библиотеки устарели.

/// //////////////////////////////////
/// Конфиг Kafka-консьюмера
/// //////////////////////////////////
@Configuration
public class KafkaConfig {

    // Environment - спринговый бин, кот позволяем читать настройки из appication-properties
    @Autowired
    Environment environment;    // класс читает из application.properties

    // фабрика для консьюмера, куда будем складывать свойства взяты из "appication-properties" и др. мест
    @Bean
    ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();


        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("spring.kafka.consumer.bootstrap-servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, environment.getProperty("spring.kafka.consumer.key-deserializer"));

        // ErrorHandlingDeserializer - класс-обертка, кот. будет делать skip, в случае данных в некорректном формате
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // указание класса для десериализации корректных данных, кот будут фильтроваться ErrorHandlingDeserializer
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, environment.getProperty("spring.kafka.consumer.value-deserializer"));

        config.put("spring.json.trusted.packages",  // .TRUSTED_PACKAGES, - JsonDeserializer.TRUSTED_PACKAGES is deprecated as of Spring for Apache Kafka version 4.0
                environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"));

        config.put(ConsumerConfig.GROUP_ID_CONFIG
                , environment.getProperty("spring.kafka.consumer.group-id"));

        return new DefaultKafkaConsumerFactory<>(config);
    }

    // фабрика, кот далает Listener-ы. В ней делаются настройки, которые управляют жизненным циклом KafkaListener.
    // Часть настроек передается через "consumerFactory", а часть делается непосредственно в теле
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
            , KafkaTemplate<String, Object> kafkaTemplate
    ) {

        // задает поведение Listener-а при чтении ошибочных данных из топика:
        // делаем посылку через kafkaTemplate - передаем его в DeadLetterPublishingRecoverer
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),    // отправка сообщения в dlt-топик
                // 1000 - через 1 сек. повторять попытки обрабоки в методе с анотации с @KafkaHandler, 3 раза.
                // После этого отправлять сообщение в dlt-топик
                new FixedBackOff(1000, 3));

        // задаем типы неповторяющихся (кот. сразу идут в dlt-топик) и повторяющиеся ошибки
        errorHandler.addNotRetryableExceptions(NonRetryableException.class);    // неповторяющиеся исключения
        errorHandler.addRetryableExceptions(RetryableException.class);          // повторяющиеся исключения

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);    // настройки консьюмера
        factory.setCommonErrorHandler(errorHandler);    // настройки обработчика ошибок

        return factory;
    }

    // bean, чтобы спринг заинжектил KafkaTemplate в ConcurrentKafkaListenerContainerFactory:
    // KafkaTemplate будет использоваться для посылки ошибок в топик с ошибками (DLT-топик)
    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // В DLT-топик будем передавать значение со стринговым ключом и значением Object
    @Bean
    ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("spring.kafka.consumer.bootstrap-servers"));
        // JsonSerializer.class и StringSerializer.class из org.apache.kafka похоже устарели
        // -> скорее всего, надо переходить на com.fasterxml.jackson.databind.JsonSerializer
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonSerializer");
        return new DefaultKafkaProducerFactory<>(config);
    }


}
