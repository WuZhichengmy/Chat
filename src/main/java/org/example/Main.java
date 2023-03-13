package org.example;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws JMSException, IOException {
        String id;
        Scanner scanner = new Scanner(System.in);
        id = scanner.nextLine();
        P2PChatClient chatClient = new P2PChatClient("127.0.0.1:61616", id);
        chatClient.start();
    }
}