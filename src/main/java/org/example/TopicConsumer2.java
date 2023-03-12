package org.example;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.FileOutputStream;

/**
 * @author cjr
 */
public class TopicConsumer2 {
    //set topic
    private static String topicName = "test.topic";
    //consumer name
    private static String consumerName="consumer2";

    private static ConnectionFactory connectionFactory;
    private static Connection connection = null;
    private static Session session;
    private static Destination destination;
    private static MessageConsumer consumer;

    public static void chat(){
        connectionFactory=new ActiveMQConnectionFactory(
                ActiveMQConnectionFactory.DEFAULT_USER,
                ActiveMQConnectionFactory.DEFAULT_PASSWORD,
                "tcp://localhost:61616"
        );
        try {
            connection=connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(Boolean.FALSE,Session.AUTO_ACKNOWLEDGE);
            System.out.println("receiver="+connection.getClientID()+", name="+consumerName);

            //set topic
            destination=session.createTopic(topicName);
            consumer=session.createConsumer(destination);

            //receive
            while (true){
                Message message=consumer.receive(100000);//100s

                if(message instanceof BytesMessage){//if file
                    BytesMessage msg=(BytesMessage)message;
                    String filename=msg.getStringProperty("filename");
                    FileOutputStream outputStream=new FileOutputStream("D:\\"+filename);

                    //read
                    byte[] bytes=new byte[1024];
                    int len=0;
                    while((len=msg.readBytes(bytes))!=-1){
                        outputStream.write(bytes,0,len);
                    }

                    outputStream.close();
                }
                else if(null!=message){//if text
                    System.out.println("receive message:");
                    TextMessage msg=(TextMessage)message;
                    System.out.println(msg.getText());
                }else {
                    break;
                }
            }

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

    public static void main(String[] args){
        chat();
    }
}
