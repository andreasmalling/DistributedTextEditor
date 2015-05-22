import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class kNode implements Runnable {
    Socket predecessor, successor, joining;
    ServerSocket sNode3;

    int port;

    public void kNode(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            sNode3 = new ServerSocket(port);

            // First client joins cirkel jerk
            joining = sNode3.accept();
            predecessor = joining;
            successor = joining;
            joining = null;

            // Other jerks
            while (true) {
                joining = sNode3.accept();

                ObjectInputStream joinIn = new ObjectInputStream(joining.getInputStream());

                JoinEvent je = (JoinEvent) joinIn.readObject();

                if (je.getRole() == Role.SUCCESSOR) {
                    successorJoin();
                } else if (je.getRole() == Role.PREDECESSOR) {
                    predecessorJoin();

                    throw new Exception("No valid role");
                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void predecessorJoin() throws IOException {

        ObjectOutputStream joinOut = null;

        joinOut = new ObjectOutputStream(joining.getOutputStream());
        // TODO: Send all text
        // TODO: Wait for acknowledge
        // TODO: Switch to new successor

    }

    private void successorJoin() throws IOException {
        InetAddress preIP = predecessor.getInetAddress();

        ObjectOutputStream joinOut = null;

        joinOut = new ObjectOutputStream(joining.getOutputStream());
        joinOut.writeObject(new JoinEvent(preIP, Role.PREDECESSOR));
        // TODO: Wait for acknowledge
        // TODO: Switch to new predecessor
    }
}
