import java.net.InetAddress;

enum Role {
    PREDECESSOR,SUCCESSOR
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
