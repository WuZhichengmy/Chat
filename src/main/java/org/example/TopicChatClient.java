package org.example;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Scanner;

public class TopicChatClient {
    private static final String pattern = "topic:%s";
    private String id;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private String serverAddr;
    private Destination destination;

    public TopicChatClient(String address, String id)throws JMSException {
        this.id = id;
        this.serverAddr = address;
        this.connectionFactory = new ActiveMQConnectionFactory("tcp://"+this.serverAddr);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(Boolean.TRUE, Session.AUTO_ACKNOWLEDGE);
    }

    public void start() throws JMSException, IOException{
        String destName;
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入群发topic：");
        destName = scanner.nextLine();

        this.destination = session.createTopic(String.format(pattern,destName));
        MessageConsumer consumer = session.createConsumer(this.destination);
        consumer.setMessageListener(new TextListener(this.id));

        System.out.println("1/发送消息，2/发送文件：");
        String choice = scanner.nextLine();

        System.out.println("输入消息内容/文件路径（输入exit退出）：");
        String sendMsg = scanner.nextLine();

        while(!sendMsg.equalsIgnoreCase("exit")){
            if(choice.equals("1"))
                sendMessage(sendMsg);
            else
                sendFile(sendMsg);
            sendMsg = scanner.nextLine();
        }

        session.close();
        connection.close();
    }

    /**
     * @param msg
     * @return: void
     * 发送文字消息
     */
    public void sendMessage(String msg)throws JMSException{
        MessageProducer producer = session.createProducer(destination);
        TextMessage message=session.createTextMessage(msg);
        message.setStringProperty("sender",this.id);
        producer.send(message);
        session.commit();
    }


    /**
     * @param filePath
     * @return: void
     * 发送文件
     */
    public void sendFile(String filePath)throws JMSException, IOException{
        //read file
        File file=new File(filePath);
        String fileName=file.getName();
        byte[] buffer=Files.readAllBytes(file.toPath());

        //send bytes
        MessageProducer producer=session.createProducer(destination);
        BytesMessage bytesMessage=session.createBytesMessage();
        bytesMessage.writeBytes(buffer);
        bytesMessage.setStringProperty("sender",this.id);

        sendMessage("file::"+fileName);
        producer.send(bytesMessage);
        session.commit();
        System.out.println("send file:"+fileName+"successfully!");
    }

}
