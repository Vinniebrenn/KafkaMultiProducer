package com.example.streambridge.service.impl;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.example.streambridge.config.KafkaProperties;
import com.example.streambridge.dto.KafkaPublishRequest;
import com.example.streambridge.dto.MessageMetadataFactory;
import com.example.streambridge.util.KafkaInterceptorRegistry;
import com.example.streambridge.util.KafkaProducerManager;
import com.example.streambridge.util.MessageStatusStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublishService {

    private final KafkaProducerManager producerManager;
    private final KafkaProperties kafkaProperties;
    private final KafkaInterceptorRegistry interceptorRegistry;
    private final MessageStatusStore messageStatusStore;

    @Retryable(
        retryFor = { Exception.class },  
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public boolean sendWithRetry(KafkaPublishRequest request) throws Exception {
        KafkaProperties.TopicSecurity topicSecurity = kafkaProperties.getTopics().stream()
            .filter(t -> t.getName().equals(request.getTopic()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No config for topic: " + request.getTopic()));

        CompletableFuture<RecordMetadata> future = producerManager.sendMessage(
            request,
            topicSecurity,
            kafkaProperties.getTruststore(),
            kafkaProperties.getBootstrapServers(),
            interceptorRegistry.get(request.getTopic())
        );

        try {
        	future.get(10, TimeUnit.SECONDS); // Will throw if Kafka failed
            return true;  // Only success if no exception thrown
        } catch (Exception e) {
            log.error("Error sending Kafka message (will retry if applicable): {}", e.getMessage(), e);
            throw e;
        }
    }


    public ResponseEntity<String> publish(KafkaPublishRequest request) {
        if (request.getHeaders() == null) {
            request.setHeaders(new HashMap<>());
        }

        String messageId = request.getHeaders().getOrDefault("messageId", UUID.randomUUID().toString());
        request.getHeaders().put("messageId", messageId);

        messageStatusStore.put(messageId, MessageMetadataFactory.pending(messageId));

        try {
            boolean published = sendWithRetry(request);
            if (published) {
                messageStatusStore.put(messageId, MessageMetadataFactory.success(messageId));
            } else {
                messageStatusStore.put(messageId, MessageMetadataFactory.failed(messageId, "Unknown failure", true));
            }
        } catch (Exception e) {
            messageStatusStore.put(messageId, MessageMetadataFactory.failed(messageId, e.getMessage(), true));
        }

        return ResponseEntity.accepted().body("Message accepted, id: " + messageId);
    }

}
