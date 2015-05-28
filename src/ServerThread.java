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
    private boolean running;

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
        dte.setTitle("I'm listening on: " + cns.getChordName().getAddress() + ":" + port);

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

                cns.setSucSocket(joiningSocket);

                dte.newEventPlayer(joiningSocket, myKey);

                System.out.println(TAG + " spawns EP");

                DisconnectThread disconnectThread = new DisconnectThread(dte, cns);
                new Thread(disconnectThread).start();
                System.out.println("DiscoThread going strong");

                joiningSocket = null;
            }

            while(running) {
                System.out.println("in serverLoop");
                joiningSocket = server.accept();

                Socket preSocket = cns.getPreSocket();

                outStream = new ObjectOutputStream(new Socket(preSocket.getInetAddress(), port+1).getOutputStream());
                System.out.println("Sending disconnectEvent...");
                outStream.writeObject(new DisconnectEvent(joiningSocket.getInetAddress()));
                System.out.println("Sent");
                cns.setPreSocket(joiningSocket);
                dte.newEventReplayer(cns.getPreSocket(), myKey);

                joiningSocket = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void terminate(){
        running = false;
    }
}
