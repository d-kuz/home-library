package com.example.demo.kafka;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
class RetryableTestConsumer {

    @Getter
    @Setter
    private CountDownLatch latch = new CountDownLatch(1);
    @Getter
    @Setter
    private String payload;

    @KafkaListener(topics = "retry-topic", groupId = "retry-group")
    public void listen(ConsumerRecord<String, String> record) {
        payload = record.value().toString();
        latch.countDown();
        System.out.println("Попытка " + latch.getCount() + ": обработка сообщения = " + record);

    }
}
