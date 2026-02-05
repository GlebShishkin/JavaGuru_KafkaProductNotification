 Пример JavaGuru работы приложения с Kafka расположенного в Docker
 см. исходный код https://github.com/AndreiBor/KafkaProductNotification

В проекте 4 модуля:
1) core - общая библиотека (не запускается как сервис)
2) ProductMicroservice - продьюсер: принимает json запросы от Postman (http://localhost:8083/product) и кладет их в Kafka-топик
3) EmailNotificationMicroservice - консьюмер: читает сообщения из Kafka-топика и послылает REST-сообщения в mock-сервис
4) mockservice  - mock-сервис принимает REST-сообщения от консьюмера и отвечает ему статусом
+ БД h2 в памяти для хранения id сообшений из ProductMicroservice

Kafka и Kafka-UI (http://localhost:8086) разворачиваются в Docker через docker-compose.yml

БД h2 используется для хранения id сообщения из ProductMicroservice для защиты от задваивания в Консьюмере её обработки (урок 22).
БД h2 размещается в памяти:
в браузере по адресу: http://localhost:8084/h2-console
в окне "JDBC_URL" забить адрес из свойства в application.properties "spring.datasource.url": jdbc:h2:mem:testdb