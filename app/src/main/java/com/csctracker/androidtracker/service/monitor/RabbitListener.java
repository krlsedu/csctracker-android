package com.csctracker.androidtracker.service.monitor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RabbitListener {

    String QUEUE_NAME = "android_notification";
    ConnectionFactory factory = new ConnectionFactory();

    public void init(MonitorNotification monitorNotification) {

        Thread thread = new Thread(() -> {
            try {
                factory.setHost("198.27.96.88");
                factory.setUsername("guest");
                factory.setPassword("guest");
                factory.setPort(5672);
                factory.setVirtualHost("/");

                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.queueDeclare(QUEUE_NAME, true, false, false, null);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received '" + message + "'");
                    monitorNotification.notify(message);
                };

                channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
                });
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException("Rabbitmq problem", e);
            }
        });
        thread.start();
    }
}
