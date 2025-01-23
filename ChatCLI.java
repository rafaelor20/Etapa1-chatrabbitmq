package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class ChatCLI {

    public static void main(String[] argv) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Solicita o nome do usuário
        System.out.print("User: ");
        String userName = scanner.nextLine();

        // Configuração do RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("44.211.138.214"); // Alterar
        factory.setUsername("admin"); // Alterar
        factory.setPassword("password"); // Alterar
        factory.setVirtualHost("/");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Cria fila para o usuário
        String userQueue = userName;
        channel.queueDeclare(userQueue, false, false, false, null);

        System.out.println("Bem-vindo, " + userName + "! Você está conectado.");

        // Thread para escutar mensagens recebidas
        Thread receiverThread = new Thread(() -> {
            try {
                Consumer consumer = new DefaultConsumer(channel) {
                    public void handleDelivery(String consumerTag, Envelope envelope,
                            AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String sender = properties.getReplyTo();
                        String message = new String(body, StandardCharsets.UTF_8);
                        String timestamp = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(new Date());

                        System.out.println("\n(" + timestamp + ") " + sender + " diz: " + message);
                        System.out.print(">> ");
                    }
                };
                channel.basicConsume(userQueue, true, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();

        // Envio de mensagens
        String currentRecipient = null;

        while (true) {
            System.out.print((currentRecipient == null ? ">> " : "@" + currentRecipient + ">> "));
            String input = scanner.nextLine();

            if (input.startsWith("@")) {
                // Define o destinatário
                currentRecipient = input.substring(1).trim();
                System.out.println("Destinatário alterado para: " + currentRecipient);
            } else if (currentRecipient != null) {
                // Envia mensagem para o destinatário atual
                String message = input;

                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .replyTo(userName)
                        .build();

                channel.basicPublish("", currentRecipient, props, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Mensagem enviada para @" + currentRecipient);
            } else {
                System.out.println("Por favor, escolha um destinatário com @nome.");
            }
        }
    }
}
