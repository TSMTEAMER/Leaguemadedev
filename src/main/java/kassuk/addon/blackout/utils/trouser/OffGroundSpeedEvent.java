package kassuk.addon.blackout.utils.trouser;

public class OffGroundSpeedEvent {
    public static final OffGroundSpeedEvent INSTANCE = new OffGroundSpeedEvent();

    public float speed;

    public static OffGroundSpeedEvent get(float speed) {
        INSTANCE.speed = speed;
        return INSTANCE;
    }
}
