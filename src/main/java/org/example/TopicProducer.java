package org.example;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.util.StringUtils;

import javax.jms.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * @author cjr
 */
public class TopicProducer {
    //set topic
    private static String topicName = "test.topic";
    private static ConnectionFactory connectionFactory;
    private static Connection connection = null;
    private static Session session;
    private static Destination destination;
    private static MessageProducer producer;

    public static void chat(){
        connectionFactory=new ActiveMQConnectionFactory(
                ActiveMQConnectionFactory.DEFAULT_USER,
                ActiveMQConnectionFactory.DEFAULT_PASSWORD,
                "tcp://localhost:61616"
        );
        try{
            connection=connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(Boolean.TRUE,Session.AUTO_ACKNOWLEDGE);

            //set topic
            destination=session.createTopic(topicName);
            producer=session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            //operation type
            System.out.println("choose producer operation: 1 for text message, 2 for file");
            Scanner scanner=new Scanner(System.in);
            int type = scanner.nextInt();

            switch (type){
                case 1:
                    //send message
                    sendMessage(session,producer);
                    break;
                case 2:
                    //send file
                    sendFile(session,producer);
                    break;
                default:
                    break;
            }

            //commit
            session.commit();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(null!=connection){
                    connection.close();
                }
            }catch (Throwable ignore){
            }
        }
    }

    public static void sendMessage(Session session, MessageProducer producer)throws Exception{
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(System.in));
        System.out.println("please input producer message:");
        String str=bufferedReader.readLine();

        TextMessage message=session.createTextMessage(str);
        System.out.println(message.getText());
        message.setStringProperty("groups","10");
        producer.send(message);

        System.out.println("send activeMq message successfully!");
    }


    public static void sendFile(Session session, MessageProducer producer)throws Exception{
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(System.in));
        System.out.println("please input file path to send:");
        String str=bufferedReader.readLine();

        //read file
        File file=new File(str);
        FileInputStream fileInputStream=new FileInputStream(file);
        byte[] buffer=new byte[fileInputStream.available()];
        fileInputStream.read(buffer);

        //send bytes
        BytesMessage bytesMessage=session.createBytesMessage();
        bytesMessage.writeBytes(buffer);

        bytesMessage.setStringProperty("filename", str.substring(str.lastIndexOf("\\")+1));
        producer.send(bytesMessage);

        fileInputStream.close();

        System.out.println("send activeMq file successfully!");
    }

    public static void main(String[] args){
        chat();
    }
}
