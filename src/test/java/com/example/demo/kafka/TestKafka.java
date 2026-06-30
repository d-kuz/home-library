package com.example.demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


@SpringBootTest
@Import(RetryableTestConsumer.class)
@Testcontainers
@ContextConfiguration(classes = {
        TestKafka.TestConfig.class
})
public class TestKafka {

    @Autowired
    RetryableTestConsumer consumer;

    // Объявляем KafkaContainer как @Container — Testcontainers сам поднимет и убьёт его
    @Container
    static final KafkaContainer kafka= new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    private BlockingQueue<String> receivedMessages;
    private KafkaMessageListenerContainer<String, String> container;
    private volatile boolean failFirst = true; // имитация ошибки при первом вызове
    Map<String, Object> producerProps;
    Map<String, Object> consumerProps;
    Set<String> processedMessage = new HashSet<>();

    @Configuration
    @EnableKafka
    static class TestConfig {

        @Bean
        public ProducerFactory<String, String> producerFactory() {
            Map<String, Object> config = Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            );
            return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        public KafkaTemplate<String, String> kafkaTemplate() {
            return new KafkaTemplate<>(producerFactory());
        }

        @Bean
        public ConsumerFactory<String, String> consumerFactory() {
            Map<String, Object> config = Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                    ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
            );
            return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory());

            // Настройка retry + DLQ
            ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(2);
            backOff.setInitialInterval(1000);
            backOff.setMultiplier(2.0);
            backOff.setMaxInterval(2000);

            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate());

            DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
            factory.setCommonErrorHandler(errorHandler);

            return factory;
        }
    }

    @Test
    //не более одного раза
    void produceConsume() throws Exception {
        // Получаем bootstrap-адрес запущенного брокера
        String bootstrap = kafka.getBootstrapServers();

        // Конфигурируем Kafka-продюсер
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Создаём продюсер и отправляем одно сообщение в demo-topic
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        producer.send(new ProducerRecord<>("demo-topic", "my-key", "Hello Kafka!")).get();
        producer.close();

        // Конфигурируем Kafka-консьюмер
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Создаём консьюмера и подписываемся на тот же топик
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("demo-topic"));

        // Ждём сообщения
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

        // Проверяем, что пришло именно то сообщение
        Assertions.assertEquals(1, records.count());
        Assertions.assertEquals("Hello Kafka!", records.iterator().next().value());
    }

    @Test
    void consume2read() throws Exception {
        //настройка отказа при первом обращении
        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        container = new KafkaMessageListenerContainer<>(consumerFactory, new ContainerProperties("demo-topic"));
        container.setupMessageListener((MessageListener<String, String>) record -> {
            if (failFirst) {
                failFirst = false;
                throw new RuntimeException("Имитация сбоя обработки");

            }
            receivedMessages.offer(record.value());
        });

        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);

        // Отправляем сообщение
        kafkaTemplate.send("demo-topic", "test-key", "test-value");
        kafkaTemplate.flush();

        // Ждём: первая попытка — ошибка, сообщение не коммитится
        Thread.sleep(2000);

        // Перезапускаем контейнер (имитация перезапуска потребителя)
        container.stop();
        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);

        // Вторая попытка — успешна
        String result = receivedMessages.poll(10, TimeUnit.SECONDS);
        Assertions.assertEquals("test-value", result);

        // Проверяем, что сообщение пришло только один раз (после восстановления)
        Assertions.assertTrue(receivedMessages.isEmpty(), "Сообщение не должно быть обработано дважды после успеха");
        // отправить подтверждение, что пришло

        container.stop();
    }

    @Test
    void consume3read() throws Exception {

//        Сценария интеграционного теста
//        Отправка сообщения
//        Обработка с возможной ошибкой
//        Повторная доставку при сбое
//        Успешная обработка при повторной попытке
//        Ручное подтверждение (acknowledgment)
//                Задача - проверить, что:
//        Потребитель получает сообщение.
//                При ошибке — сообщение не коммитится и будет перечитано.
//        При успешной обработке — сообщение подтверждается и не дублируется.

        Map<String,  String> cashe = new HashMap<>();
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProps));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Создаем контейнер через фабрику
        ConcurrentMessageListenerContainer<String, String> container =
                factory.createContainer("demo-topic");

        // Устанавливаем слушатель
        container.setupMessageListener((AcknowledgingMessageListener<String, String>)
                (record, acknowledgment) -> {
                    if (failFirst) {
                        failFirst = false;
                        throw new RuntimeException("Имитация сбоя обработки");
                    }
                    cashe.put(record.key(), record.value());
                    acknowledgment.acknowledge(-1);
                });

        // Запускаем контейнер
        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);
        // Отправляем сообщение
        try {
            // Отправляем сообщение
            kafkaTemplate.send("demo-topic", "test-key", "test-value");
            kafkaTemplate.send("demo-topic", "test-key", "test-value");
            kafkaTemplate.flush();

            // Ждем результат
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> cashe.size() <= 1);
            String result = cashe.get("test-key");
            Assertions.assertEquals("test-value", result);

            // Проверяем отсутствие дублей
            Thread.sleep(200);
            Assertions.assertTrue(cashe.size() <= 1,
                    "Сообщение не должно быть обработано дважды");
        } finally {
            container.stop();
        }


    }

    

    @Test
    void consume4read() throws Exception {

            // Отправляем сообщение
            kafkaTemplate.send("demo-topic", "test-key", "test-value");
            kafkaTemplate.send("demo-topic", "test-key", "test-value");
            kafkaTemplate.flush();

            // Ждем результат
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> consumer.getLatch().getCount() <= 1);
            String result = consumer.getPayload();
            Assertions.assertEquals("test-value", result);

            // Проверяем отсутствие дублей
            Thread.sleep(200);
            Assertions.assertTrue(consumer.getLatch().getCount() <= 1,
                    "Сообщение не должно быть обработано дважды");

    }
    // Реализация идемпотентного фильтра для Spring Kafka, который гарантирует,
    // что сообщение будет обработано только один раз, даже при повторной доставке (например, при at-least-once).
    //Хранит обработанные messageId во внутреннем кэше
    //Проверяет, не было ли уже обработано сообщение.
    //Пропускает дубли.


}
