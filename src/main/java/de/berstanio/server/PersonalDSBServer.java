package de.berstanio.server;

import de.berstanio.ghgparser.DSBNotLoadableException;
import de.berstanio.ghgparser.GHGParser;
import de.berstanio.ghgparser.Plan;
import de.berstanio.ghgparser.User;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PersonalDSBServer {

    private static HashMap<Integer, Plan> plans11 = new HashMap<>();
    private static HashMap<Integer, Plan> plans12 = new HashMap<>();
    private static String freeRooms;
    private static HashMap<UUID, User> users = new HashMap<>();

    public static void main(String[] args) throws IOException, DSBNotLoadableException, ClassNotFoundException {
        loadUsers();
        GHGParser.init(GHGParser.class.getResourceAsStream("/rawPage.htm"), new File("user"));
        DSBSocket dsbSocket = new DSBSocket(InetAddress.getByName("62.75.210.181"), 21589);
        dsbSocket.start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    saveUsers();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    boolean b = update();
                    if (b){
                        System.out.println("Update gefunden!");
                        updateFreeRooms();
                        //Sende Nachricht an alle Clients
                    }
                } catch (DSBNotLoadableException | IOException e) {
                    e.printStackTrace();
                }
            }
        }, 500, TimeUnit.MINUTES.toMillis(5));
    }

    public static void saveUsers() throws IOException {
        if (Files.exists(Paths.get("users.yml"))){
            Files.move(Paths.get("users.yml"), Paths.get("usersTMP.yml"), StandardCopyOption.REPLACE_EXISTING);
        }
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("users.yml"))) {
            objectOutputStream.writeObject(getUsers());
        }catch (IOException e){
            Files.move(Paths.get("usersTMP.yml"), Paths.get("users.yml"), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(Paths.get("usersTMP.yml"));
    }

    public static void loadUsers() throws IOException, ClassNotFoundException {
        if (Files.exists(Paths.get("users.yml"))){
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("users.yml"))){
                setUsers((HashMap<UUID, User>) objectInputStream.readObject());
            } catch (IOException | ClassNotFoundException e) {
                if (Files.exists(Paths.get("usersTMP.yml"))){
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("usersTMP.yml"))) {
                        setUsers((HashMap<UUID, User>) objectInputStream.readObject());
                    }catch (IOException | ClassNotFoundException e2){

                    }
                }
                throw e;
            }
        }else if (Files.exists(Paths.get("usersTMP.yml"))){
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("usersTMP.yml"))) {
                setUsers((HashMap<UUID, User>) objectInputStream.readObject());
            }
        }
    }

    public static boolean update() throws DSBNotLoadableException {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        for (int i = 0; i < 2; i++) {
            int year = 11 + i;
            for (int j = 0; j < 2; j++) {
                int tmpWeek = week + j;
                Plan plan = new Plan(year, tmpWeek);
                if (getPlans(year).containsKey(tmpWeek)){
                    System.out.println("Vergleiche " + plan.getLastUpdate().toString() + " mit altem " + getPlans(year).get(tmpWeek).getLastUpdate());
                    if (!plan.getLastUpdate().after(getPlans(year).get(tmpWeek).getLastUpdate())){
                        System.out.println("Kein Update wird durchgeführt!");
                        return false;
                    }
                }
                getPlans(year).put(tmpWeek, plan);
            }
        }
        return true;
    }

    public static void updateFreeRooms() throws IOException {
        setFreeRooms(FreeRoomDSB.refresh());
    }

    public static HashMap<Integer, Plan> getPlans(int year) {
        return year == 12 ? plans12 : plans11;
    }

    public static String getFreeRooms() {
        return freeRooms;
    }

    public static void setFreeRooms(String freeRooms) {
        PersonalDSBServer.freeRooms = freeRooms;
    }

    public static HashMap<UUID, User> getUsers() {
        return users;
    }

    public static void setUsers(HashMap<UUID, User> users) {
        PersonalDSBServer.users = users;
    }
}
