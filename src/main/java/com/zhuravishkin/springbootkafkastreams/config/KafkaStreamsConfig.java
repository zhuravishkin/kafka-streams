package com.zhuravishkin.springbootkafkastreams.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuravishkin.springbootkafkastreams.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@EnableKafkaStreams
@Configuration
public class KafkaStreamsConfig {
    private final ObjectMapper objectMapper;

    public KafkaStreamsConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "streamsApplicationId");
        properties.put(StreamsConfig.CLIENT_ID_CONFIG, "streamsClientId");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "streamsGroupId");

        properties.put(StreamsConfig.producerPrefix(StreamsConfig.RETRIES_CONFIG), Integer.MAX_VALUE);
        properties.put(StreamsConfig.consumerPrefix(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG), Integer.MAX_VALUE);

        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());
        properties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());

        properties.put(StreamsConfig.TOPOLOGY_OPTIMIZATION, StreamsConfig.OPTIMIZE);
        properties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "1");

        return new KafkaStreamsConfiguration(properties);
    }

    @Bean
    public Serde<User> userSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(User.class));
    }

    @Bean
    public KStream<String, String> kStream(StreamsBuilder kStreamBuilder) {
        KStream<String, String> stream = kStreamBuilder
                .stream("src-topic", Consumed.with(Serdes.String(), Serdes.String()));
        KStream<String, User> userStream = stream.mapValues(this::getUserFromString);
        userStream.to("out-topic", Produced.with(Serdes.String(), userSerde()));

        return stream;
    }

    User getUserFromString(String userString) {
        User user = null;
        try {
            user = objectMapper.readValue(userString, User.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }

        return user;
    }
}
