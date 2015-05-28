import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread implements Runnable {

    private String TAG = "ServerThread";
    private final ChordNameServiceImpl cns;
    private final int port;
    private DistributedTextEditor dte;
    private Socket joiningSocket;
    private ServerSocket server;
    private ObjectOutputStream outStream;

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
        dte.setTitle("I'm listening on " + cns.getChordName().getAddress() + ":" + port);

        try {
            // First join
            if (server == null) {
                server = new ServerSocket(port);

                System.out.println(TAG + " is hosting");
                joiningSocket = server.accept();
                System.out.println(TAG + " has one friend");

                cns.setPreSocket(joiningSocket);
                dte.newEventReplayer(joiningSocket, myKey);

                System.out.println(TAG + " spawns ERP");

                cns.setSucSocket(new Socket(joiningSocket.getInetAddress(), port));
                //cns.setSucSocket(joiningSocket);
                //outStream = new ObjectOutputStream(joiningSocket.getOutputStream());
                //outStream.writeObject(new ConnectEvent());

                dte.newEventPlayer(joiningSocket, myKey);

                System.out.println(TAG + " spawns EP");
                ServerSocket hack = new ServerSocket(port+1);

                DisconnectThread disconnectThread = new DisconnectThread(dte, cns, hack.accept());
                new Thread(disconnectThread).start();

                joiningSocket = null;
            }

            while(true) {
                joiningSocket = server.accept();

                Socket preSocket = cns.getPreSocket();

                cns.notFirst();

                outStream = new ObjectOutputStream(preSocket.getOutputStream());

                outStream.writeObject(new DisconnectEvent(preSocket.getInetAddress()));
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