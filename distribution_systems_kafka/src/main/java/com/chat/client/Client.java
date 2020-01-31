package com.chat.client;

import com.chat.kafka.KafkaConsumer;
import com.chat.kafka.KafkaProducer;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.PropertyConfigurator;
import java.io.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * A class represent client
 */
public class Client {
    private static final int PARTITIONS = 1;
    private static final short REPLICATION_FACTORY = 2;
    private static boolean connected = false;
    private static AdminClient adminClient;
    private static Hashtable<String, KafkaConsumer> consumers = new Hashtable<>();
    private static Producer<Long, String> producer;
    private static String username = "";
    private static String ip;
    private static int port;
    private static long index = 0;
    /**
     * The main method that run the main ui thread
     */
    public static void main(String[] args) {
        PropertyConfigurator.configure(Client.class.getClassLoader().getResource("log4j.properties"));
        Scanner sc = new Scanner(System.in);

        String command = "";
        Properties config = new Properties();//object to load the properties file

        //print main menu
        printMenu();

        while (!command.trim().toUpperCase().equals("QUIT")) {
            System.out.println("\n(1)Login (2)Register (3)Leave (4)Send (5)Refresh (6)Disconnect (7)Quit\nChoose an option:");
            try
            {
                command = "";
                command = sc.nextLine();
            }
            catch (Exception e){ System.out.println("The program was interrupted, Good Bye!"); System.exit(1);}
            switch (command.toUpperCase().trim()) {
                case "1":
                case "IN":
                case "LOGIN":
                    connect(sc, config);
                    break;
                case "6":
                case "D":
                case "DISCONNECT":
                    if (connected) {
                        username = "";
                        stopConnection();
                    } else {
                        System.out.println("You need to connect first");
                    }
                    break;
                case "7":
                case "Q":
                case "QUIT":
                    if (connected)//close connection first
                    {
                        stopConnection();
                        //Close resources
                        try {
                            sc.close();
                        } catch (Exception e) {
                            System.out.println("An error occurred while trying to close one of the resources");
                        }
                    }
                    System.out.println("Closing the app --- GoodBye!");
                    System.exit(0);
                    break;
                case "2":
                case "R":
                case "REGISTER":
                    if (connected) {
                        //show registered topics
                        System.out.println("registered topics: " + consumers.keySet());
                        System.out.println("Please insert a topic");
                        System.out.println("topic : ");
                        String topic = "";
                        topic = isEmptyLoop(sc, topic, "topic cannot be empty").trim().toUpperCase();
                        registerForTopic(topic, false);
                    }
                    else {
                        System.out.println("You need to connect first");
                    }
                    break;
                case "3":
                case "L":
                case "LEAVE":
                    if (connected) {
                        //show registered topics
                        System.out.println("registered topics: " + consumers.keySet());

                        System.out.println("Please insert a topic");
                        System.out.println("topic : ");
                        String topic = "";
                        topic = isEmptyLoop(sc, topic, "topic cannot be empty").trim().toUpperCase();
                        if (consumers.containsKey(topic)) {
                            consumers.get(topic).setStop(true);
                            consumers.remove(topic);
                            System.out.println("OK - topic unregistered successfully");
                        } else {
                            System.out.println("Error - you tried to leave unregistered topic");
                        }
                    } else {
                        System.out.println("You need to connect first");
                    }
                    break;
                case "4":
                case "S":
                case "SEND":
                    if (connected) {
                        System.out.println("Please insert a topic");
                        System.out.println("topic : ");
                        String topic = "";
                        topic = isEmptyLoop(sc, topic, "topic cannot be empty").trim().toUpperCase();
                        System.out.println("Please insert a sentence");
                        System.out.println("sentence : ");
                        String sentence = "";
                        sentence = isEmptyLoop(sc, sentence, "message cannot be empty");
                        if(!checkIfTopicExistsInCluster(topic))
                        {
                            addTopicToCluster(topic);
                        }
                        ProducerRecord<Long, String> record = new ProducerRecord<>(topic, index++, String.format("( %s ) %s %s - %s", topic, username,
                                            getCurrentTimeStamp(), sentence));
                        new Thread(() -> {
                            producer.send(record);
                            producer.flush(); }).start();
                    } else {
                        System.out.println("You need to connect first");
                    }
                    break;
                case "5":
                case "F":
                case "REFRESH":
                    refreshConsumers();
                    break;
                default:
                    System.out.println("Unsupported Command");
                    break;
            }
        }
    }

    /**
     * A method that reads the server ip and port from the configuration file
     * The method tries to connect to server
     * @param config - properties file object
     */
    private static void connect(Scanner sc, Properties config) {
        if (!connected) {
            //Load the properties file
            try {
                config.load(new FileInputStream(System.getProperty("user.dir") + "/kafka-broker-config.properties"));
            } catch (IOException e) {
                System.out.println("\nThe configuration file 'client-config.properties' was not found.");
                System.out.println("You need to configure this file, and locate it in the same location of the Client tool.");
                System.out.println("The requested location is: " + System.getProperty("user.dir"));
                return;
            }

            System.out.println("Please insert user name");
            System.out.println("user name : ");
            username = isEmptyLoop(sc, username, "user name cannot be empty");

            //Read Server information configuration from properties file
            ip = config.getProperty("broker-ip");

            try {
                port = Integer.parseInt(config.getProperty("broker-port"));
            } catch (NumberFormatException e) {
                System.out.println("\nPort must be a number!");
                System.out.println("Please modify the port in the configuration file, and try to connect again");
                return;
            }
            Properties adminConf = new Properties();
            adminConf.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ip+":"+port);
            adminConf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
            adminClient = KafkaAdminClient.create(adminConf);
            producer = KafkaProducer.createProducer(ip, port);
            connected = true;
            System.out.println("Connecting successfully to kafka broker - " + ip + " on port " + port);
        } else {
            System.out.println("You are already connected");
        }
    }

    /**
     * A method that print the client menu
     */
    private static void printMenu() {
        System.out.println("Welcome to client app\n" +
                "--- Client app menu ---\n" +
                "1. Login (in) - Login to kafka broker with user name\n" +
                "2. Register (r) - Register for a topic\n" +
                "3. Leave (l) - Leave a topic\n" +
                "4. Send (s) - Send a message\n" +
                "5. Refresh (f) - Refresh kafka consumers\n" +
                "6. Disconnect(d) - Disconnect from the kafka broker\n" +
                "7. Quit (q) - Close client program");
    }

    /**
     * A method that registers for new topic, and creates a new consumer
     * @param topic  - the topic to be registered on
     * @param refresh - indication if it's a new topic, or refresh of an existing one
     */
    private static void registerForTopic(String topic, boolean refresh)
    {
        if (refresh || !consumers.containsKey(topic))
        {
            if(!checkIfTopicExistsInCluster(topic))
            {
                addTopicToCluster(topic);
            }
            KafkaConsumer consumer = new KafkaConsumer(ip, port, topic);
            consumer.start();
            consumers.put(topic, consumer);

            waitForServerReply(0);
            if(!refresh) {
                System.out.println("OK - topic registered successfully");
            }
        }
        else {
            System.out.println("Error - you tried to registered topic twice");
        }
    }

    /**
     * A method that refresh the current consumers,
     * by close an recreate them
     */
    private static void refreshConsumers()
    {
        //clone topics set
        Set<String> topics = new HashSet<>(consumers.keySet());
        topics.forEach(topic -> {
            //kill current consumer
            consumers.get(topic).setStop(true);
            consumers.remove(topic);
            registerForTopic(topic, true);
        });
        System.out.println("Successfully refreshed kafka consumers");
    }

    /**
     * A method that creates a new topic in the cluster,
     * with replication factory 2
     * @param topic - the topic to create
     */
    private static void addTopicToCluster(String topic)
    {
        int numOfBrokers = 0;
        DescribeClusterResult cluster = adminClient.describeCluster();
        try {
            numOfBrokers = cluster.nodes().get().size();
        } catch (InterruptedException | ExecutionException e) {
            //doNothing
        }
        if(numOfBrokers > 1 && !checkIfTopicExistsInCluster(topic))
        {
            NewTopic newTopic = new NewTopic(topic, PARTITIONS, REPLICATION_FACTORY);
            adminClient.createTopics(Collections.singletonList(newTopic));
        }
    }

    /**
     * A method that checks if a topic exists in the cluster
     * @param topicToCheck - the topic to check on
     * @return - true if the topic exists in the cluster, false otherwise
     */
    private static boolean checkIfTopicExistsInCluster(String topicToCheck)
    {
        try {
            for(String topic : adminClient.listTopics().names().get())
            {
                if(topicToCheck.equals(topic))
                {
                    return true;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Failed to receive topics list from cluster");
        }
        return false;
    }

    /**
     * A method that stops the connection with the server
     */
    private static void stopConnection() {
        connected = false;
        consumers.values().forEach(consumer -> consumer.setStop(true));
        consumers.clear();
        producer.close();
        System.out.println("Disconnected successfully from kafka broker - " + ip + ":" + port);
    }

    /**
     * A m method that run in a loop, as long as the input is empty
     * @param sc - a scanner instance the connected to keyboard
     * @param input - the input from the user
     * @param message - the message to be printed
     * @return - the none empty input from the user
     */
    private static String isEmptyLoop(Scanner sc, String input, String message) {
        while (isEmpty(input)) {
            input = sc.nextLine();
            if (isEmpty(input)) {
                System.out.println(message);
                System.out.println("please try again:");
            }
        }
        return input;
    }

    /**
     * A method that check if a string is empty
     * @param str - the string to be checked
     * @return true if the string is empty, false otherwise
     */
    private static boolean isEmpty(String str) {
        return (str == null) || (str.trim().isEmpty());
    }

    /**
     * A method the cause the main ui thread to wait, until the server thread will reply
     */
    private static void waitForServerReply(int timeout) {
        try {
            synchronized (Client.class) {
                Client.class.wait(timeout);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method that retrieve the current time
     * @return A string of the current time
     */
    private static String getCurrentTimeStamp() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * A method that retrieve the current date and time
     * @return A string of the current date and time
     */
    public static String getCurrentDateTimeStamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    /**
     * A method that retrieves the name of the user
     * @return - the name of the user
     */
    public static String getUsername() {
        return username;
    }
}
