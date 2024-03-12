package kassuk.addon.blackout.utils.walper;
import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import meteordevelopment.starscript.value.Value;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotifyUtils {
    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public static void initialize(){
        executor.scheduleAtFixedRate(SpotifyUtils::update, 0, 15, TimeUnit.SECONDS);
    }
    private static String currentArtist = "Not Initialized";
    private static String currentTrack = "Not Initialized";

    public static Value spotifyArtist(){
        return Value.string(currentArtist);
    }

    public static Value spotifyTrack(){
        return Value.string(currentTrack);
    }

    private static void update(){
        SpotifyAPI api = SpotifyAPIFactory.createInitialized();
        // There's probably a way better way to do this than initializing a new spotify api
        // It is to prevent using a disconnected spotify instance
        // Maybe todo: Disconnect Listener

        boolean hasTrack = api.hasTrack();

        currentArtist = hasTrack ? api.getTrack().getArtist(): "No song playing";
        currentTrack = hasTrack ? api.getTrack().getName(): "No song playing";
    }


}
