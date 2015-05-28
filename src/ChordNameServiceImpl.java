import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChordNameServiceImpl {

    private DistributedTextEditor dte;
    private int port;
    protected InetSocketAddress myName;
    protected int myKey;
    private InetSocketAddress suc;
    private InetSocketAddress pre;
    private InetSocketAddress connectedAt;
    private ServerSocket serverSocket;

    private Socket preSocket, sucSocket;
    private DisconnectThread disconnectThread;

    public Socket getSucSocket() {
        return sucSocket;
    }

    public void setSucSocket(Socket sucSocket) {
        this.sucSocket = sucSocket;
    }

    public Socket getPreSocket() {
        return preSocket;
    }

    public void setPreSocket(Socket preSocket) {
        this.preSocket = preSocket;
    }

    private boolean active;
    private boolean first;
    private ServerThread serverThread;

    public ChordNameServiceImpl(InetSocketAddress myName, DistributedTextEditor dte){
        this.myName = myName;
        this.port = myName.getPort();
        this.dte = dte;
    }

    public int keyOfName(InetSocketAddress name)  {
        int tmp = name.hashCode()*1073741651 % 2147483647;
        if (tmp < 0) { tmp = -tmp; }
        return tmp;
    }

    public InetSocketAddress getChordName()  {
        return myName;
    }

    public void createGroup(){
        serverThread = new ServerThread(dte,this);
        new Thread(serverThread).start();
    }

    public void joinGroup(InetSocketAddress knownPeer)  {
        active = true;
        connectedAt = knownPeer;

        try {
            // Setup successor
            sucSocket = new Socket(knownPeer.getAddress(),port);

            // Start listening for disconnects from successor
            disconnectThread = new DisconnectThread(dte,this);
            new Thread(disconnectThread).start();

            dte.newEventPlayer(sucSocket, myKey);

            // Wait for new predecessor
            ServerSocket server = new ServerSocket(port);//FIXME
            preSocket = server.accept();
            dte.newEventReplayer(preSocket, myKey);

            // Keep listining for new joins
            serverThread = new ServerThread(dte,this,server);
            new Thread(serverThread).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leaveGroup() {
        try {
            Socket socket = new Socket(preSocket.getInetAddress(), preSocket.getPort()+1);
            ObjectOutputStream disconnectStream = new ObjectOutputStream(socket.getOutputStream());
            disconnectStream.writeObject(new DisconnectEvent(sucSocket.getInetAddress()));
            preSocket.close();
            sucSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

	/*
	 * If joining we should now enter the existing group and
	 * should at some point register this peer on its port if not
	 * already done and start listening for incoming connection
	 * from other peers who want to enter or leave the
	 * group. After leaveGroup() was called, the run() method
	 * should return so that the thread running it might
	 * terminate.
	 */
}