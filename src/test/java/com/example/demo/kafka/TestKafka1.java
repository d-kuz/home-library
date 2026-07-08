package com.example.demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = {
        TestKafka1.TestConfig.class
})
public class TestKafka1 {

    @Container
    static final KafkaContainer kafka= new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ProducerFactory<String, String> producerFactory;

    @Autowired
    ConsumerFactory<String, String> consumerFactory;


    private BlockingQueue<String> receivedMessages;
    private KafkaMessageListenerContainer<String, String> container;
    private volatile boolean failFirst = true; // имитация ошибки при первом вызове


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
    void consume2read() throws Exception {
        failFirst = true;
        receivedMessages = new LinkedBlockingQueue<>();

        //настройка отказа при первом обращении
        container = new KafkaMessageListenerContainer<>(consumerFactory, new ContainerProperties("demo-topic"));
        container.setupMessageListener((MessageListener<String, String>) record -> {
            if (failFirst) {
                failFirst = false;
                throw new RuntimeException("Имитация сбоя обработки");

            }
            receivedMessages.offer(record.value());
        });

        kafkaTemplate = new KafkaTemplate<>(producerFactory);
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
        factory.setConsumerFactory(consumerFactory);
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

}
