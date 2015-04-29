/**
 * Created by Kresten Axelsen on 27-04-2015.
 */
public class DisconnectEvent extends MyTextEvent {
    DisconnectEvent(int offset) {
        super(offset);
    }
    private boolean shouldDisconnect;

    public boolean shouldDisconnect() {
        return shouldDisconnect;
    }

    public void setShouldDisconnect() {
        shouldDisconnect = true;
    }
}
