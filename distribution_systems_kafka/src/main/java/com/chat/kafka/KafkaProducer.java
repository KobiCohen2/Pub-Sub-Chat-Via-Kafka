package com.chat.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;
import java.util.UUID;

/**
 * A class represent a kafka producer
 */
public class KafkaProducer {

    /**
     * A method the creates a kafka producer that sends messages on different topics
     * @param ip - the ip of the kafka broker
     * @param port - the port of the kafka broker
     * @return - Producer<Long, String> that represent the kafka producer
     */
    public static Producer<Long, String> createProducer(String ip, int port) {
        final String BOOTSTRAP_SERVERS = ip+":"+port;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer_" + UUID.randomUUID());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);//maybe in comment
        return new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }

}
