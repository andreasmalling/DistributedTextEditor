import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Originally created by Kresten Axelsen on 27-04-3015, a day we will all remember.
 */
public class DisconnectEvent implements Serializable {
    InetSocketAddress newSuccessor;

    public DisconnectEvent(InetSocketAddress successor) {
        newSuccessor = successor;
    }

    public InetSocketAddress getNewSuccessor() {
        return newSuccessor;
    }
}