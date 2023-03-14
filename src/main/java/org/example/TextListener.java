package org.example;

import javax.jms.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 监听消息
 */
class TextListener implements MessageListener {
    private String fileName;
    private ChatClient client;

    public TextListener(ChatClient client){
        this.client = client;
    }

    @Override
    public void onMessage(Message message) {
        //not to receive topicMsg sent by itself
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
            } catch (JMSException | IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("无法处理的消息类型");
        }
    }
}