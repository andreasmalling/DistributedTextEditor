import java.io.Serializable;
import java.net.InetSocketAddress;

enum Role{
    PREDECESSOR, SUCCESSOR, ABORTSUCCESSOR
}
public class JoinEvent implements Serializable {
    private final InetSocketAddress name;
    private final Role role;

    public JoinEvent(InetSocketAddress name, Role role){
        this.name = name;
        this.role = role;
    }

    public InetSocketAddress getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }
}
