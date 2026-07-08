package com.example.demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;


@Testcontainers
public class TestKafka0 {

    @Container
    static final KafkaContainer kafka= new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );


    @Test
    //не более одного раза
    void produceConsume() throws Exception {
        // Получаем bootstrap-адрес запущенного брокера
        String bootstrap = kafka.getBootstrapServers();

        // Конфигурация Kafka-продюсера
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Создаём продюсер и отправляем одно сообщение в demo-topic
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        producer.send(new ProducerRecord<>("demo-topic", "my-key", "Hello Kafka!")).get();
        producer.close();

        // Конфигурация Kafka-консьюмера
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

}
