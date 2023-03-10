package org.example;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class P2PChatClient {
    private static final String pattern = "queue:%s";
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private String serverAddr;
    private String id;
    private Destination destination;
    public P2PChatClient(String addr, String id) throws JMSException {
        this.id = id;
        this.serverAddr=addr;
        this.connectionFactory = new ActiveMQConnectionFactory(serverAddr);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
    }
    public void start() throws JMSException {
        String destName;
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入对方ID：");
        destName = scanner.nextLine();
        destination = session.createQueue(String.format(pattern, destName));
        MessageConsumer consumer = session.createConsumer(session.createQueue(String.format(pattern, id)));
        consumer.setMessageListener(new TextListener());
        System.out.println("输入消息内容：");
        String sendMsg = scanner.nextLine();
        while(!sendMsg.equalsIgnoreCase("exit")){
            send(sendMsg);
            sendMsg = scanner.nextLine();
        }
        session.close();
        connection.close();
    }
    public void send(String msg) throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        TextMessage textMessage = session.createTextMessage(msg);
        producer.send(textMessage);
        session.commit();
    }

}

class TextListener implements MessageListener {

    @Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
        try {
            String text = textMessage.getText();
            System.out.println(LocalDateTime.now()+" "+text);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}