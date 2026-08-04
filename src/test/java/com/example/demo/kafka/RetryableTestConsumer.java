package com.example.demo.kafka;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Component
class RetryableTestConsumer {

    @Getter
    @Setter
    private CountDownLatch latch = new CountDownLatch(1);

    @Getter
    @Setter
    private String payload;

    @Getter
    private final AtomicInteger processedCount = new AtomicInteger(0);

    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "demo-topic", groupId = "retry-group")
    public void listen(ConsumerRecord<String, String> record) {
        String messageId = record.key();

        if (messageId != null && !processedIds.add(messageId)) {
            System.out.println("Дубль пропущен: " + messageId);
            return;
        }

        payload = record.value();
        latch.countDown();
        processedCount.incrementAndGet();
        System.out.println("Обработано: " + record);
    }
}