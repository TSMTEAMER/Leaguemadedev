package kassuk.addon.blackout.utils.walper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

public class Utils {
    private final static Gson gson = new Gson();
    public static String getNearbyMcdonaldsLocationUrl(){
        String thej;

        try {
            thej = new BufferedReader(new InputStreamReader(new URL("http://ip-api.com/json/").openStream())).readLine();
        } catch (Exception ignored) {
            return "Failed";
        }

        Float lat = null;
        Float lon = null;

        JsonObject jsonObject = gson.fromJson(thej, JsonObject.class);
        if (jsonObject != null) lat = Float.parseFloat(jsonObject.get("lat").getAsString());
        jsonObject = gson.fromJson(thej, JsonObject.class);
        if (jsonObject != null) lon = Float.parseFloat(jsonObject.get("lon").getAsString());

        try {
                return "https://www.mcdonalds.com/googleappsv2/geolocation?latitude=" + URLEncoder.encode(String.valueOf(lat), "UTF-8") +
                    "&longitude=" + URLEncoder.encode(String.valueOf(lon), "UTF-8") +
                    "&radius=18&maxResults=1&country=us&language=en-us";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
    }

}
