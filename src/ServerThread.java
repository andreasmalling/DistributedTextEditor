import java.io.IOException;
import java.io.ObjectOutputStream;
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
        port = cns.getMyName().getPort();
    }

    public ServerThread(DistributedTextEditor dte, ChordNameServiceImpl cns, ServerSocket ss) {
        this.dte = dte;
        this.cns = cns;
        port = cns.getMyName().getPort();
        server = ss;
    }

    @Override
    public void run() {
        String ipaddress = cns.getMyName().getAddress().getHostAddress();
        dte.setTitle("I'm listening on: " + ipaddress + ":" + port);

        try {
            // First join
            if (server == null) {
                server = new ServerSocket(port);

                System.out.println(TAG + " is hosting");
                joiningSocket = server.accept();
                System.out.println(TAG + " has one friend");

                cns.setPreSocket(joiningSocket);
                dte.newEventReplayer(joiningSocket);

                System.out.println(TAG + " spawns ERP");

                cns.setSucSocket(joiningSocket);

                dte.newEventPlayer(joiningSocket);

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

                outStream = new ObjectOutputStream(new Socket(preSocket.getInetAddress(), preSocket.getPort()+1).getOutputStream());
                System.out.println("Sending disconnectEvent...");

                InetSocketAddress joiningAddress = new InetSocketAddress(joiningSocket.getInetAddress(), joiningSocket.getPort());
                outStream.writeObject(new DisconnectEvent(joiningAddress));
                System.out.println("Sent");
                cns.setPreSocket(joiningSocket);
                dte.newEventReplayer(cns.getPreSocket());

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
