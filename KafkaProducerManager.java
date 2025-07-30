package com.example.streambridge.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import com.example.streambridge.config.KafkaProperties;
import com.example.streambridge.dto.KafkaPublishRequest;
import com.example.streambridge.interceptor.KafkaInterceptor;
import com.example.streambridge.interceptor.KafkaTransformedRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaProducerManager {

    private final Map<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();
    private final String krb5ConfPath = "classpath:/krb5.conf"; // customize if needed

    public KafkaProducer<String, String> getOrCreateProducer(String jaasConfig, KafkaProperties.Truststore truststore, String bootstrapServers) {
        String producerKey = Integer.toHexString(Objects.hash(jaasConfig, truststore.getLocation()));

        return producers.computeIfAbsent(producerKey, k -> {
            try {
                // Set krb5.conf programmatically
                if (System.getProperty("java.security.krb5.conf") == null) {
                    File krbFile = new File(getClass().getResource("/krb5.conf").toURI());
                    System.setProperty("java.security.krb5.conf", krbFile.getAbsolutePath());
                }else
                {
                    System.setProperty("java.security.krb5.conf", krb5ConfPath);
                }
            } catch (Exception e) {
                log.warn("Failed to load krb5.conf dynamically", e);
            }

            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "GSSAPI");
            props.put("sasl.jaas.config", jaasConfig);
            props.put("ssl.truststore.location", truststore.getLocation());
            props.put("ssl.truststore.password", truststore.getPassword());
            props.put("key.serializer", StringSerializer.class.getName());
            props.put("value.serializer", StringSerializer.class.getName());

            return new KafkaProducer<>(props);
        });
    }

    public CompletableFuture<RecordMetadata> sendMessage(KafkaPublishRequest request,
                                                         KafkaProperties.TopicSecurity security,
                                                         KafkaProperties.Truststore truststore,
                                                         String bootstrapServers,
                                                         KafkaInterceptor interceptor) {
        KafkaProducer<String, String> producer = getOrCreateProducer(security.getJaasConfig(), truststore, bootstrapServers);
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();

        try {
        	KafkaTransformedRecord kafkaTransformedRecord = interceptor.prePublish(request.getTopic(), request);

            ProducerRecord<String, String> record =
            	    new ProducerRecord<>(request.getTopic(), request.getPartition(), kafkaTransformedRecord.getKey(), kafkaTransformedRecord.getValue());

            	if (request.getHeaders() != null) {
            	    request.getHeaders().forEach((k, v) -> {
            	        if (v != null) {
            	            record.headers().add(k, v.getBytes(StandardCharsets.UTF_8));
            	        }
            	    });
            	}

            	producer.send(record, (metadata, exception) -> {
            	    if (exception != null) {
            	        log.error("Kafka send FAILED to topic {}: {}", request.getTopic(), exception.getMessage(), exception);
            	        interceptor.onError(request.getTopic(), kafkaTransformedRecord, exception);
            	        future.completeExceptionally(exception);
            	    } else {
            	        log.info("Kafka message SENT successfully: topic={}, partition={}, offset={}", 
            	                 metadata.topic(), metadata.partition(), metadata.offset());
            	        interceptor.postPublish(request.getTopic(), kafkaTransformedRecord, metadata);
            	        future.complete(metadata);
            	    }
            	});


        } catch (Exception e) {
            log.error("Error in sendMessage()", e);
            future.completeExceptionally(e);
        }

        return future;
    }
}
