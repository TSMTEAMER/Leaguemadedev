// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.utils.vh;

public enum TimeFormat {
    TWENTY_FOUR_HOUR("24 hour"),
    TWELVE_HOUR("12 hour");

    private final String title;

    private TimeFormat(String title) {
        this.title = title;
    }

    public String toString() {
        return this.title;
    }
}
