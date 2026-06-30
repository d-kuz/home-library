package com.example.demo.kafka;

import com.example.demo.kafka.RetryableTestConsumer;
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
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


@SpringBootTest
@Import(RetryableTestConsumer.class)
@Testcontainers
@ContextConfiguration(classes = {
        TestKafka2.TestConfig.class
})
public class TestKafka2 {

    @Autowired
    RetryableTestConsumer consumer;

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
