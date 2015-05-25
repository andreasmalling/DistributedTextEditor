import java.net.InetAddress;

/**
 * Role is the part you want the other party to play. E.g. contacting a node, and you want to be your successor,
 * the joinEvent should contain the Role.SUCCESSOR
 */
enum Role {
    PREDECESSOR, SUCCESSOR
}

public class JoinEvent {
    private final InetAddress ip;
    private final Role role;

    public JoinEvent(InetAddress ip, Role role) {
        this.ip = ip;
        this.role = role;
    }

    public InetAddress getIp() {
        return ip;
    }

    public Role getRole() {
        return role;
    }
}
