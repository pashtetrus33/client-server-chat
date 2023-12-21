package ru.geekbrains.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class ClientManager implements Runnable {

    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    private final String id;
    private boolean isAdmin;
    private final String ADMIN_SEQ = "#IamAdmin#";

    public final static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        id = String.valueOf(UUID.randomUUID());
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            bufferedWriter.write(id);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            clients.add(this);
            System.out.println(id + ": " + name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String massageFromClient;

        while (socket.isConnected()) {
            try {
                massageFromClient = bufferedReader.readLine();
//                if (massageFromClient == null) {
//                    // для  macOS
//                    closeEverything(socket, bufferedReader, bufferedWriter);
//                    break;
//                }
                processMessage(massageFromClient);

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void processMessage(String message) throws IOException {
        if (message.split(" ")[1].equals(ADMIN_SEQ)) {
            isAdmin = true;
            broadcastMessage(name + " is ADMIN");
        } else if (isAdmin && message.split(" ")[1].equals("kick")) {
            String target = message.split(" ")[2];
            for (ClientManager client : clients) {
                if (client.name.equals(target)) {
                    broadcastMessage(name + " kicked " + target);
                    kick(client);
                }
            }
        } else if (message.split(" ")[1].charAt(0) == '@') {
            String privateTo = message.split(" ")[1].substring(1);
            for (ClientManager client : clients) {
                try {
                    if ((client.name.equals(privateTo) || client.id.equals(privateTo)) && !client.name.equals(name)) {
                        client.bufferedWriter.write(message.replace("@" + privateTo, "private message ->"));
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        } else {
            broadcastMessage(message);
        }
    }

    private void broadcastMessage(String message) {
        for (ClientManager client : clients) {
            try {
                if (!client.id.equals(id)) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }


    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void kick(ClientManager target) {
        // Удаление клиента из коллекции
        removeClient(target.getName());
        try {
            // Закрытие соединения с клиентским сокетом
            if (target.getSocket() != null) {
                target.getSocket().close();
            }
            // Завершаем работу буфера на чтение данных
            if (target.getBufferedReader() != null) {
                target.getBufferedReader().close();
            }
            // Завершаем работу буфера для записи данных
            if (target.getBufferedWriter() != null) {
                target.getBufferedWriter().close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    private void removeClient(String name) {
        clients.remove(name);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public String getName() {
        return name;
    }
}
