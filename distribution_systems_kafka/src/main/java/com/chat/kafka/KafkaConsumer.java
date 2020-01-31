package com.chat.kafka;

import com.chat.client.Client;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static com.chat.client.Client.getCurrentDateTimeStamp;

/**
 * A class represent a kafka consumer
 */
public class KafkaConsumer extends Thread {

    private final String TOPIC;
    private final String BOOTSTRAP_SERVERS;
    private boolean stop = false;
    private File messagesOutput;
    /**
     * Constructor
     * @param ip - the ip of the kafka broker
     * @param port - the port of the kafka broker
     * @param topic - the topic to register on
     */
    public KafkaConsumer(String ip, int port, String topic)
    {
        BOOTSTRAP_SERVERS = ip+":"+port;
        TOPIC = topic;
        messagesOutput = new File(topic + ".txt");
    }

    /**
     * A method the creates a kafka consumer that listen to specific topic
     * @return - Consumer<Long, String> that represent the kafka consumer
     */
    private Consumer<Long, String> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ID_" + UUID.randomUUID());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, Client.getUsername() + "_" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // Create the consumer using props.
        final Consumer<Long, String> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);

        System.out.println("[" + getCurrentDateTimeStamp() + "] new consumer created on topic " + TOPIC);
        // Subscribe to the topic..
        consumer.subscribe(Collections.singletonList(TOPIC));

        notifyClientUiTread();
        return consumer;
    }

    /**
     * A method that listen to incoming messages, in a new thread
     */
    @Override
    public void run() {
        final Consumer<Long, String> consumer = createConsumer();
        while (!stop) {
            final ConsumerRecords<Long, String> consumerRecords = consumer.poll(Duration.ofSeconds(1));
            if (consumerRecords.count() == 0) {
                continue;
            }
            consumerRecords.forEach(record -> {

                System.out.println(record.value());
                writeMessageToFile(record.value());
            });
        }
        consumer.close();
    }

    /**
     * A getter method for stop field
     * @param stop - indication field tells the consumer to stop listen
     */
    public void setStop(boolean stop) {
        this.stop = stop;
    }

    /**
     * A method that notify the UI thread of the client,
     * in order to receive reply from the server, before the UI main thread take control
     */
    private void notifyClientUiTread()
    {
        synchronized (Client.class) {
            Client.class.notify();
        }
    }

    /**
     * A method that write a message to topic file
     * @param message - message to write to topic file
     */
    private void writeMessageToFile(String message) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(messagesOutput, true))) {
            bw.write(message);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error while writing to message to topic fle");
        }
    }
}
