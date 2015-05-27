import java.io.Serializable;
import java.net.InetAddress;

/**
 * Originally created by Kresten Axelsen on 27-04-3015, a day we will all remember.
 */
public class DisconnectEvent implements Serializable {
    InetAddress newSuccessor;

    public DisconnectEvent(InetAddress successor) {
        newSuccessor = successor;
    }

    public InetAddress getNewSuccessor() {
        return newSuccessor;
    }
}