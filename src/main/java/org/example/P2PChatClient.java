package org.example;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

public class P2PChatClient{

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
        this.connectionFactory = new ActiveMQConnectionFactory("tcp://" + serverAddr);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
    }

    public void start() throws JMSException, IOException {
        String destName;
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入对方ID：");
        destName = scanner.nextLine();
        destination = session.createQueue(String.format(pattern, destName));

        MessageConsumer consumer = session.createConsumer(session.createQueue(String.format(pattern, id)));
        consumer.setMessageListener(new TextListener(this));

        System.out.println("1/发送消息，2/发送文件：");
        String c = scanner.nextLine();

        System.out.println("输入消息内容/文件路径（输入exit退出）：");
        String sendMsg = scanner.nextLine();

        while(!sendMsg.equalsIgnoreCase("exit")){
            if(c.equals("1"))
                send(sendMsg);
            else
                sendFileInByte(sendMsg);
            sendMsg = scanner.nextLine();
        }

        session.close();
        connection.close();
    }

    /**
     * 发送文字信息
     * @param msg
     * @throws JMSException
     */
    public void send(String msg) throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        TextMessage textMessage = session.createTextMessage(msg);
        producer.send(textMessage);
        session.commit();
    }

    /**
     * 发送文件
     * @param filePath
     * @throws IOException
     * @throws JMSException
     */
    public void sendFileInByte(String filePath) throws IOException, JMSException {
        File file = new File(filePath);
        String fileName = file.getName();
        byte[] bytes = Files.readAllBytes(file.toPath());
        MessageProducer producer = session.createProducer(destination);
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(bytes);
        send("file::"+fileName);
        producer.send(bytesMessage);
        session.commit();
    }

    public void forwardFile(String filePath) throws JMSException, IOException {

    }
}
