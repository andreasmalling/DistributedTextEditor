import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread implements Runnable {

    private final ChordNameServiceImpl cns;
    private final int port;
    private DistributedTextEditor dte;
    private Socket joiningSocket;
    private ServerSocket server;

    public ServerThread(DistributedTextEditor dte, ChordNameServiceImpl cns) {
        this.dte = dte;
        this.cns = cns;
        port = cns.getChordName().getPort();
    }

    public ServerThread(DistributedTextEditor dte, ChordNameServiceImpl cns, ServerSocket ss) {
        this.dte = dte;
        this.cns = cns;
        port = cns.getChordName().getPort();
        server = ss;
    }

    @Override
    public void run() {
        int myKey = cns.keyOfName(cns.getChordName());

        try {
            // First join
            if (server == null) {
                server = new ServerSocket(port);

                joiningSocket = server.accept();
                cns.setPreSocket(joiningSocket);
                dte.newEventReplayer(joiningSocket, myKey);

                cns.setSucSocket(new Socket(joiningSocket.getInetAddress(), port));
                dte.newEventPlayer(joiningSocket, myKey);

                DisconnectThread disconnectThread = new DisconnectThread(dte, cns, cns.getSucSocket());

                joiningSocket = null;
            }

            while(true) {
                joiningSocket = server.accept();

                Socket preSocket = cns.getPreSocket();

                ObjectOutputStream disconnectStream = new ObjectOutputStream(preSocket.getOutputStream());

                disconnectStream.writeObject(new DisconnectEvent(preSocket.getInetAddress()));
                preSocket.close();

                cns.setPreSocket(joiningSocket);
                dte.newEventReplayer(preSocket, myKey);

                joiningSocket = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
