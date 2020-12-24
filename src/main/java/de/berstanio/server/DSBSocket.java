package de.berstanio.server;

import de.berstanio.ghgparser.CoreCourse;
import de.berstanio.ghgparser.DSBNotLoadableException;
import de.berstanio.ghgparser.GHGParser;
import de.berstanio.ghgparser.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class DSBSocket {

    private ServerSocket socket;

    public DSBSocket(InetAddress address, int port) throws IOException {
        setSocket(new ServerSocket(port, 50, address));

    }

    public void start(){
        new Thread(this::listen).start();
    }

    public void listen(){
        System.out.println("Listen to new client!");

        boolean b = false;
        try (Socket client = getSocket().accept()) {
            new Thread(this::listen).start();
            b = true;
            System.out.println(client.getInetAddress().getHostAddress() + " hat sich verbunden");
            ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            int week = objectInputStream.readInt();

            switch (week){
                case -2: {
                    //Create User
                    int year = objectInputStream.readInt();
                    ArrayList<CoreCourse> coreCourses = (ArrayList<CoreCourse>) objectInputStream.readObject();
                    User user = new User(coreCourses, year);
                    UUID uuid = UUID.randomUUID();
                    PersonalDSBServer.getUsers().put(uuid.toString(), user);
                    objectOutputStream.writeObject(uuid.toString());
                    break;
                }
                case -1: {
                    //FreeRooms
                    objectOutputStream.writeObject(PersonalDSBServer.getFreeRooms());
                    break;
                }
                case 0: {
                    //JahresStundenPlan
                    // TODO: 18.12.2020 Jahresstundeplan regelmäßig updaten
                    int year = objectInputStream.readInt();
                    objectOutputStream.writeObject(GHGParser.getJahresStundenPlan(year));
                    break;
                }
                default: {
                    //WochenPlan
                    String uuid = (String) objectInputStream.readObject();
                    User user = PersonalDSBServer.getUsers().get(uuid);
                    String html = GHGParser.generateHtmlFile(user, PersonalDSBServer.getPlans(user.getYear()).get(week));
                    objectOutputStream.writeObject(html);
                    break;
                }
            }
            objectOutputStream.flush();

            objectOutputStream.close();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException | DSBNotLoadableException e) {
            if (!b){
                new Thread(this::listen).start();
            }
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
