package de.berstanio.server;

import de.berstanio.ghgparser.DSBNotLoadableException;
import de.berstanio.ghgparser.GHGParser;
import de.berstanio.ghgparser.Plan;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class PersonalDSBServer {

    private static HashMap<Integer, Plan> plans11 = new HashMap<>();
    private static HashMap<Integer, Plan> plans12 = new HashMap<>();
    private static String freeRooms;

    public static void main(String[] args) throws IOException, DSBNotLoadableException {
        GHGParser.init(GHGParser.class.getResourceAsStream("/rawPage.htm"), new File("user"));
        DSBSocket dsbSocket = new DSBSocket(InetAddress.getByName("62.75.210.181"), 21589);
        dsbSocket.start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    boolean b = update();
                    if (b){
                        System.out.println("Update gefunden!");
                        GHGParser.getJahresStundenPlan(11).refresh();
                        GHGParser.getJahresStundenPlan(12).refresh();
                        updateFreeRooms();
                        //Sende Nachricht an alle Clients
                    }
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }, 500, TimeUnit.MINUTES.toMillis(5));
    }

    public static boolean update() {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        for (int i = 0; i < 2; i++) {
            int year = 11 + i;
            for (int j = 0; j < 2; j++) {
                int tmpWeek = week + j;
                if (tmpWeek >= 54) tmpWeek = 1;
                try {
                    Plan plan = new Plan(year, tmpWeek);
                    if (getPlans(year).containsKey(tmpWeek)) {
                        System.out.println("Vergleiche " + plan.getLastUpdate().toString() + " mit altem " + getPlans(year).get(tmpWeek).getLastUpdate());
                        if (!plan.getLastUpdate().after(getPlans(year).get(tmpWeek).getLastUpdate())) {
                            System.out.println("Kein Update wird durchgeführt!");
                            return false;
                        }
                    }
                    getPlans(year).put(tmpWeek, plan);
                }catch (DSBNotLoadableException e){
                    e.printStackTrace();
                }
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
}
