/**
 * DisconnectEvent is a MyTextEvent used to let all the threads know
 * when to close. It contains a boolean to let the threads close
 * in the right order, and in the end close the socket.
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
