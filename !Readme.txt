 Пример JavaGuru работы приложения с Kafka расположенного в Docker
 см. исходный код https://github.com/AndreiBor/KafkaProductNotification

В проекте 4 модуля:
1) core - общая библиотека (не запускается как сервис)
2) ProductMicroservice - продьюсер: принимает json запросы от Postman (http://localhost:8083/product) и кладет их в Kafka-топик
3) EmailNotificationMicroservice - консьюмер: читает сообщения из Kafka-топика и послылает REST-сообщения в mock-сервис
4) mockservice  - mock-сервис принимает REST-сообщения от консьюмера и отвечает ему статусом

Kafka и Kafka-UI (http://localhost:8086) разворачиваются в Docker через docker-compose.yml