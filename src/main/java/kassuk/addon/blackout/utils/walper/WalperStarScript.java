package kassuk.addon.blackout.utils.walper;

import com.google.gson.*;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.ValueMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class WalperStarScript {
    public static void init() {
        URL url = null;
        try {
            url = new URL(Utils.getNearbyMcdonaldsLocationUrl());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String jsonString = null;
        try {
            jsonString = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JsonObject jsonResponse = new Gson().fromJson(jsonString, JsonObject.class);
        JsonArray featuresArray = jsonResponse.getAsJsonArray("features");
        if (featuresArray != null && featuresArray.size() > 0) {
            JsonObject firstFeature = featuresArray.get(0).getAsJsonObject();
            JsonObject properties = firstFeature.getAsJsonObject("properties");

            String mcAddress = properties.get("addressLine1").getAsString();
            String mcHours = properties.get("todayHours").getAsString();
            MeteorStarscript.ss.set("walper", new ValueMap()
                .set("mcaddress", mcAddress)
                .set("mchours", mcHours)
                .set("spotifyTrack", SpotifyUtils::spotifyTrack)
                .set("spotifyArtist", SpotifyUtils::spotifyArtist)
            );
        }
    }
}
