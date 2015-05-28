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

            System.out.println("EP spawned");
            dte.newEventPlayer(sucSocket, myKey);
            System.out.println("Wait for new predecessor");
            // Wait for new predecessor
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000);
            preSocket = serverSocket.accept();
            System.out.println("accepted");

            dte.newEventReplayer(preSocket, myKey);
            System.out.println("ERP spawned");
            // Keep listening for new joins
            serverThread = new ServerThread(dte,this,serverSocket);
            new Thread(serverThread).start();
            System.out.println("serverThread spawned");
        }catch ( java.io.InterruptedIOException e1 ) {
            System.out.println("Time out, must be first time");
            preSocket = sucSocket;
            System.out.println("accepted in exception");

            dte.newEventReplayer(preSocket, myKey);
            System.out.println("ERP spawned in exception");
            // Keep listening for new joins
            try {
                serverSocket.close();
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverThread = new ServerThread(dte,this,serverSocket);
            new Thread(serverThread).start();
            System.out.println("serverThread spawned in exception");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leaveGroup() {
        try {
            Socket socket = new Socket(preSocket.getInetAddress(), preSocket.getPort()+1);
            ObjectOutputStream disconnectStream = new ObjectOutputStream(socket.getOutputStream());
            disconnectStream.writeObject(new DisconnectEvent(sucSocket.getInetAddress()));

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