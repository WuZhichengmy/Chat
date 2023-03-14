package org.example;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Scanner;

enum MsgType{
    TEXT((byte) 0), FILE((byte) 1);
    final byte val;
    MsgType(byte i) {
        val=i;
    }
}

enum ChatType{
    QUEUE((byte) 0), TOPIC((byte) 1);
    final byte val;
    ChatType(byte i) {
        val=i;
    }
}

public class ChatApp {

    private static final String patternQueue = "queue:%s";
    private static final String patternTopic = "topic:%s";
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private String serverAddr;
    private String id;
    private Destination destination;
    private String lastMsg; //上一条收到的消息
    private String lastFilePath; //上一次接收到的文件路径
    private MsgType msgType; //信息的类型
    private ChatType mode; //0为单发
    private MsgType lastMsgType; //上一条收到的消息类型

    public MsgType getLastMsgType() {
        return lastMsgType;
    }

    public void setLastMsgType(MsgType lastMsgType) {
        this.lastMsgType = lastMsgType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(String lastMsg) {
        this.lastMsg = lastMsg;
    }

    public String getLastFilePath() {
        return lastFilePath;
    }

    public void setLastFilePath(String lastFilePath) {
        this.lastFilePath = lastFilePath;
    }

    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

    public ChatType getMode() {
        return mode;
    }

    public void setMode(ChatType mode) {
        this.mode = mode;
    }

    public String getId() {
        return id;
    }

    public ChatApp(String addr, String id) throws JMSException {
        this.id = id;
        this.serverAddr=addr;
        this.connectionFactory = new ActiveMQConnectionFactory("tcp://" + serverAddr);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
    }

    public void start() throws JMSException, IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("1/单发，2/群发：");
        String type = scanner.nextLine();
        MessageConsumer consumer;
        if(type.equals("1")) {
            mode = ChatType.QUEUE;
        }
        else {
            mode = ChatType.TOPIC;
        }

        String destName;
        System.out.println("输入对方ID/群发对象Topic：");
        destName = scanner.nextLine();
        if(mode == ChatType.QUEUE) {
            destination = session.createQueue(String.format(patternQueue, destName));
            consumer = session.createConsumer(session.createQueue(String.format(patternQueue, id)));
        }
        else {
            destination = session.createTopic(String.format(patternTopic, destName));
            consumer = session.createConsumer(session.createTopic(String.format(patternTopic, destName)));
        }

        consumer.setMessageListener(new TextListener(this));

        System.out.println("1/发送消息，2/发送文件：");
        String c = scanner.nextLine();
        if(c.equals("1"))
            msgType=MsgType.TEXT;
        else
            msgType=MsgType.FILE;

        System.out.println("输入消息内容/文件路径（输入exit退出，“forward:对方id” 转发）：");
        String sendMsg = scanner.nextLine();

        while(!sendMsg.equalsIgnoreCase("exit")){
            if(mode == ChatType.TOPIC){
                if(sendMsg.startsWith("forward:")){
                    String forwardDest = sendMsg.substring(8);
                    Destination forwardDestQueue = session.createTopic(String.format(patternTopic, forwardDest));
                    if(lastMsgType == MsgType.TEXT){
                        sendTopicMsg(lastMsg, forwardDestQueue);
                    } else{
                        sendTopicFile(lastFilePath, forwardDestQueue);
                    }
                } else if(msgType == MsgType.TEXT){
                    sendTopicMsg(sendMsg, destination);
                } else{
                    sendTopicFile(sendMsg, destination);
                }
            } else{
                if(sendMsg.startsWith("forward:")){
                    String forwardDest = sendMsg.substring(8);
                    Destination forwardDestQueue = session.createQueue(String.format(patternQueue, forwardDest));
                    if(lastMsgType == MsgType.TEXT){
                        sendQueueMsg(lastMsg, forwardDestQueue);
                    } else{
                        sendQueueFile(lastFilePath, forwardDestQueue);
                    }
                } else if(msgType == MsgType.TEXT){
                    sendQueueMsg(sendMsg, destination);
                } else{
                    sendQueueFile(sendMsg, destination);
                }
            }
            sendMsg = scanner.nextLine();
        }

        session.close();
        connection.close();
    }

    /**
     * 向Queue发送文字信息
     * @param msg
     * @throws JMSException
     */
    public void sendQueueMsg(String msg, Destination msgDest) throws JMSException {
        MessageProducer producer = session.createProducer(msgDest);
        TextMessage textMessage = session.createTextMessage(msg);
        producer.send(textMessage);
        session.commit();
    }

    /**
     * 向Queue以Byte形式发送文件
     * @param filePath
     * @throws IOException
     * @throws JMSException
     */
    public void sendQueueFile(String filePath, Destination msgDest) throws IOException, JMSException {
        File file = new File(filePath);
        String fileName = file.getName();
        byte[] bytes = Files.readAllBytes(file.toPath());
        MessageProducer producer = session.createProducer(msgDest);
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(bytes);
        sendQueueMsg("file::"+fileName, msgDest);
        producer.send(bytesMessage);
        session.commit();
    }

    /**
     * @param msg
     * @return: void
     * 向Topic发送文字消息
     */
    public void sendTopicMsg(String msg, Destination msgDest)throws JMSException{
        MessageProducer producer = session.createProducer(msgDest);
        TextMessage message=session.createTextMessage(msg);
        message.setStringProperty("sender",this.id);
        producer.send(message);
        session.commit();
    }

    /**
     * @param filePath
     * @return: void
     * 发送文件给当前topic
     */
    public void sendTopicFile(String filePath, Destination msgDest)throws JMSException, IOException{
        //read file
        File file=new File(filePath);
        String fileName=file.getName();
        byte[] buffer=Files.readAllBytes(file.toPath());

        //send bytes
        MessageProducer producer=session.createProducer(msgDest);
        BytesMessage bytesMessage=session.createBytesMessage();
        bytesMessage.writeBytes(buffer);
        bytesMessage.setStringProperty("sender",this.id);

        sendTopicMsg("file::"+fileName, msgDest);
        producer.send(bytesMessage);
        session.commit();
        System.out.println("send file: "+fileName+" successfully!");
    }

}

/**
 * 监听消息
 */
class TextListener implements MessageListener {
    private String fileName;

    private ChatApp client;

    public TextListener(ChatApp client){
        this.client = client;
    }

    @Override
    public void onMessage(Message message) {
        try {
            if(message.getStringProperty("sender")!=null){
                if(message.getStringProperty("sender").equals(this.client.getId())){
                    return;
                }
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        if(message instanceof TextMessage) {
            // 普通消息
            TextMessage textMessage = (TextMessage) message;
            try {
                String text = textMessage.getText();
                if(text.startsWith("file::")){
                    fileName=text.substring(6);
                    System.out.println("接收到文件：" + fileName);
                } else{
                    System.out.println(LocalDateTime.now() + " " + text);
                    this.client.setLastMsgType(MsgType.TEXT);
                    this.client.setLastMsg(text);
                }
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        } else if(message instanceof BytesMessage) {
            // 文件
            BytesMessage bytesMessage = (BytesMessage) message;
            byte[] bytes = new byte[1024];
            try {
                bytesMessage.readBytes(bytes);
                Path path = Paths.get(".//" + fileName);
                Files.write(path, bytes);
                this.client.setLastMsgType(MsgType.FILE);
                this.client.setLastFilePath(".//" + fileName);
            } catch (JMSException | IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("无法处理的消息类型");
        }
    }
}