/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
/**
 *
 * @author bensoutendijk
 */
public class WeatherBot{

    public static void main(String args[]) throws Exception{
        WeatherBot bot = new WeatherBot();
        bot.connect();
        bot.joinChannel("#irchacks");
        String line = null;
        while((line = bot.readLine()) != null){
            System.out.println(line);
        }
    }

    public final String DEFAULT_LOC = "London";

    public final String APPID = "";

    public final String HELP_COPY = "To get the weather of a location type '!weather <us_zip_code>' replacing <us_zip_code> with a valid United States zip code.";

    class Response{
        List<Weather> weather;
        void setWeather(List<Weather> weather){
            this.weather = weather;
        }
        List<Weather> getWeather(){
            return this.weather;
        }
        Main main;
        void setMain(Main main){
            this.main = main;
        }
        Main getMain(){
            return this.main;
        }
        String name;
        void setName(String name){
            this.name = name;
        }
        String getName(){
            return this.name;
        }
    }
    class Weather{
        String description;
        void setDescription(String description){
            this.description = description;
        }
        String getDescription(){
            return this.description;
        }
    }
    class Main{
        Double temp, temp_min, temp_max;
        void setTemp(Double temp){
            this.temp = temp;
        }
        Double getTemp(){
            return this.temp;
        }
        void setTemp_Min(Double temp_min){
            this.temp_min = temp_min;
        }
        Double getTemp_Min(){
            return this.temp_min;
        }
        void setTemp_Max(Double temp_max){
            this.temp_max = temp_max;
        }
        Double getTemp_Max(){
            return this.temp_max;
        }
    }

    HttpClient httpClient = HttpClientBuilder.create().build();

    private String server, channel, name;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    public WeatherBot() throws Exception{
        server = "irc.freenode.net";
        channel = "#irchacks";
        name = "WeatherBot";
        socket = new Socket(server, 6667);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void connect() throws Exception{

        writer.write("NICK " + name + "\r\n");
        writer.write("USER " + name + " 8 * :Soutendijk Weather Bot\r\n");
        writer.flush();

        String line = null;
        while ((line = reader.readLine()) != null){
            if (line.indexOf("004") >= 0){
//                We have logged in
                break;
            } else if (line.indexOf("433") >= 0){
                System.out.println("Nickname already in use");
                return;
            }
        }
    }

    public void joinChannel(String channel) throws Exception {
        writer.write("JOIN " + channel + "\r\n");
        writer.flush();
    }

    public String readLine() throws Exception {
        String line = null;
        if ((line = reader.readLine()) != null){
            if (line.startsWith("PING ")) {
//                We must respond to pings to prevent getting disconnect from the server
                writer.write("PONG " + line.substring(5) + "\r\n");
                writer.flush();
            } else {
//                Check if chat message is sent by user
                if (line.contains("PRIVMSG")){
                    int i = line.indexOf(":",1);
                    String message = line.substring(i+1);
                    onMessage(message);
                }
            }
        } else {
            throw new Exception();
        }
        return line;
    }

    public void onMessage(String line) throws Exception{
//        Parse the message for applicable commands
        if (line.startsWith("!weather ")){
            String loc = DEFAULT_LOC;
            String arg = "";
            int i = line.indexOf(" ");
            if (line.substring(i+1).toLowerCase().startsWith("-help")){
                sendMessage(HELP_COPY);
                arg = "help";
            } else
            if (isZip(line.substring(i+1))){
                loc = line.substring(i+1);
                String q = getWeather(loc, "zip");
                sendMessage(q);
            } else
            if (arg.isEmpty()){
                loc = line.substring(i+1);
                String q = getWeather(loc, "city");
                sendMessage(q);
            }
        }
    }

    private String getWeather(String loc, String type) throws Exception{
        String description = null;
        String name = null;
        Double temp, temp_min, temp_max;
        Gson gson = new Gson();
        URIBuilder url = new URIBuilder("https://api.openweathermap.org/data/2.5/weather?");
        if(type.equals("city")){
            url.addParameter("q", loc);
        } else if (type.equals("zip")){
            url.addParameter("zip", loc+",us");
        }
        url.addParameter("APPID", APPID);
        HttpGet get = new HttpGet(url.toString());

        HttpResponse httpResponse = httpClient.execute(get);
        Scanner s = new Scanner(httpResponse.getEntity().getContent()).useDelimiter("\\A");
        String jsonResponse = s.hasNext() ? s.next() : "";

        Response response = gson.fromJson(jsonResponse, Response.class);
        description = response.getWeather().get(0).getDescription();
        temp = response.getMain().getTemp();
        temp_min = response.getMain().getTemp_Min();
        temp_max = response.getMain().getTemp_Max();
        name = response.getName();
        return "Current temp in " + name + ": " + (int)(temp-273)+"°C" + ", " + description + ", with a high of " + (int)(temp_max-273)+"°C" + " and a low of " + (int)(temp_min-273)+"°C";
    }

    private void sendMessage(String message) throws Exception{
        writer.write("PRIVMSG " + channel + " :" + message + "\r\n");
        writer.flush();
    }

    static boolean isZip(String s){
        int numberOfDigits = 0;
        for (int i = 0; i < s.length(); i++){
            if (tryParseInt(s.charAt(i))){
                numberOfDigits++;
            }
        }
        if (numberOfDigits == 5){
            return true;
        }
        return false;
    }

    static boolean tryParseInt(char c) {
       try {
            Integer.parseInt(Character.toString(c));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
