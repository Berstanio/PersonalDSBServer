package de.berstanio.server;



import de.berstanio.ghgparser.GHGParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import de.berstanio.ghgparser.DayOfWeek;

public class FreeRoomDSB {

    public static String html = "";

    public static void main(String[] args) throws IOException {
        Files.write(Paths.get("out.html"), refresh().getBytes(StandardCharsets.UTF_8));
    }

    static {
        html = readHtmlFile();
    }

    public static String readHtmlFile(){
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(GHGParser.class.getResourceAsStream("/rawPage.htm")));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            stringBuilder.append(bufferedReader.readLine());
            while ((line = bufferedReader.readLine()) != null){
                stringBuilder.append("\n").append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String generateHTML(HashMap<DayOfWeek, ArrayList<String>> dayMap){
        String s = html;
        for (Map.Entry<DayOfWeek, ArrayList<String>> entry : dayMap.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            ArrayList<String> strings = entry.getValue();
            for (int i = 0; i < 5; i++) {
                StringBuilder stringBuilder = new StringBuilder(strings.get(i));
                String identifier = dayOfWeek.name().substring(0, 2) + (i + 1);
                for (int j = 3; j < stringBuilder.length(); j += 4) {
                    if (j% 3 == 0){
                        if (j + 4 == stringBuilder.length()){
                            s = s.replace(identifier + "C", stringBuilder.substring(j-3,j) + "<br>" + identifier + "C");
                        }else {
                            s = s.replace(identifier + "R", stringBuilder.substring(j-3,j) + "<br>" + identifier + "R");
                        }
                    }else if(j % 3 == 1){
                        s = s.replace(identifier + "C", stringBuilder.substring(j-3,j) + "<br>" + identifier + "C");
                    }else {
                        s = s.replace(identifier + "L", stringBuilder.substring(j-3,j) + "<br>" + identifier + "L");
                    }
                }
                s = s.replace("<br>" + identifier + "R", "")
                        .replace("<br>" + identifier + "C", "")
                        .replace("<br>" + identifier + "L", "");
            }
        }
        for (int i = 0; i < 5; i++) {
            DayOfWeek dayOfWeek = DayOfWeek.of(i + 1);
            for (int j = 0; j < 8; j++) {
                String identifier = dayOfWeek.name().substring(0, 2) + (j + 1);
                s = s.replace(identifier + "R", "")
                        .replace(identifier + "C", "")
                        .replace(identifier + "L", "");
            }
        }
        return s;
    }

    public static String download(String roomNumber, String token) throws IOException {

        Calendar calendar = Calendar.getInstance(Locale.GERMANY);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

        URL connectwat = new URL("https://light.dsbcontrol.de/DSBlightWebsite/Data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + token + "/" + weekOfYear + "/r/r" + roomNumber + ".htm");
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        urlConnection.connect();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
            stringBuilder.append(c);
        }
        bufferedInputStream.close();
        return stringBuilder.toString();

    }

    public static String getToken() throws JSONException, IOException {

        URL connectwat = new URL("https://mobileapi.dsbcontrol.de/dsbtimetables?authid=a7f2b46b-4d23-446e-8382-404d55c31f90");
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        urlConnection.connect();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
            stringBuilder.append(c);
        }
        bufferedInputStream.close();
        JSONArray array = new JSONArray(stringBuilder.toString());
        JSONObject object = (JSONObject)array.get(0);
        return (String)object.get("Id");

    }

    public static String refresh() throws IOException, JSONException {
        String token = getToken();
        HashMap<DayOfWeek, ArrayList<String>> allMap = getFreeLessons(download("00027", token));
        for (int i = 28; i <= 46; i++) {
            String dl = download("000" + i, token);
            HashMap<DayOfWeek, ArrayList<String>> map = getFreeLessons(dl);
            for (Map.Entry<DayOfWeek, ArrayList<String>> entry : map.entrySet()) {
                DayOfWeek dayOfWeek = entry.getKey();
                ArrayList<String> strings = entry.getValue();
                ArrayList<String> allStrings = allMap.get(dayOfWeek);
                for (int j = 0; j < strings.size(); j++) {
                    String s = strings.get(j);
                    if (!s.isEmpty()) {
                        if (!allStrings.get(j).isEmpty()) {
                            allStrings.set(j, allStrings.get(j) + ";" + s);
                        } else {
                            allStrings.set(j, s);
                        }
                    }
                }
            }
        }
        return generateHTML(allMap);
    }

    public static HashMap<DayOfWeek, ArrayList<String>> getFreeLessons(String s){
        HashMap<DayOfWeek, ArrayList<String>> dayListMap = new HashMap<>();
        dayListMap.put(DayOfWeek.MONDAY, new ArrayList<>());
        dayListMap.put(DayOfWeek.TUESDAY, new ArrayList<>());
        dayListMap.put(DayOfWeek.WEDNESDAY, new ArrayList<>());
        dayListMap.put(DayOfWeek.THURSDAY, new ArrayList<>());
        dayListMap.put(DayOfWeek.FRIDAY, new ArrayList<>());

        Document document = Jsoup.parse(s);

        String room = document.getElementsByAttributeValue("color","#0000FF").get(0).text();

        Elements tables = document.getElementsByAttributeValue("rules", "all");
        Element table = tables.get(0);
        Elements columnsLessons =  table.child(0).children();
        for (int i = 1, z = 0; i < columnsLessons.size() - 1; i += 2, z++) {
            Element columnLessons = columnsLessons.get(i);
            Elements days = columnLessons.children();
            days.remove(0);
            for (int j = 0, dayi = 0; j < days.size(); j++, dayi++) {
                Element day = days.get(j);
                DayOfWeek dayEnum = DayOfWeek.of(dayi+1);
                if (dayListMap.get(dayEnum).size() != z){
                    j--;
                    continue;
                }
                dayEnum = DayOfWeek.of(dayi+1);
                String write;
                if (day.attr("colspan").equals("6")){
                    write = "";
                    j++;
                }else if (day.getElementsByTag("tr").size() <= 1){
                    write = room;
                }else if (day.getElementsByTag("strike").size() != 0){
                    write = room;
                }else {
                    write = "";
                }

                int row = Integer.parseInt(day.attr("rowspan"));
                int count = row / 2;
                for (int k = 0; k < count; k++) {
                    dayListMap.get(dayEnum).add(write);
                }
            }
        }
        return dayListMap;
    }


}
