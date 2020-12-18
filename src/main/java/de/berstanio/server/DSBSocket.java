package de.berstanio.server;

import de.berstanio.ghgparser.DSBNotLoadableException;
import de.berstanio.ghgparser.GHGParser;
import de.berstanio.ghgparser.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DSBSocket {

    private ServerSocket socket;

    public DSBSocket(InetAddress address, int port) throws IOException {
        setSocket(new ServerSocket(port, 50, address));

    }

    public void start(){
        new Thread(this::listen).start();
    }

    public void listen(){
        try {
            System.out.println("Listen to new client!");
            Socket client = getSocket().accept();
            new Thread(this::listen).start();
            System.out.println(client.getInetAddress().getHostAddress() + " hat sich verbunden");
            ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            int week = objectInputStream.readInt();

            if (week == -1){
                objectOutputStream.writeObject(PersonalDSBServer.getFreeRooms());
            }else if (week == 0) {
                int year = objectInputStream.readInt();
                // TODO: 18.12.2020 Jahresstundeplan regelmäßig updaten
                objectOutputStream.writeObject(GHGParser.getJahresStundenPlan(year));
            } else {
                User user = (User) objectInputStream.readObject();
                String html = GHGParser.generateHtmlFile(user, PersonalDSBServer.getPlans(user.getYear()).get(week));
                objectOutputStream.writeObject(html);
            }
            objectOutputStream.flush();

            objectOutputStream.close();
            objectInputStream.close();
            client.close();
        } catch (IOException | ClassNotFoundException | DSBNotLoadableException e) {
            e.printStackTrace();
        }
    }

    public ServerSocket getSocket() {
        return socket;
    }

    public void setSocket(ServerSocket socket) {
        this.socket = socket;
    }
}
