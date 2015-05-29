import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChordNameServiceImpl {

    private DistributedTextEditor dte;
    private int port;
    protected InetSocketAddress myName;
    protected int myKey;
    private ServerSocket serverSocket;

    private Socket preSocket, sucSocket;
    private boolean leaving;

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

    private ServerThread serverThread;

    public ChordNameServiceImpl(InetSocketAddress myName, DistributedTextEditor dte){
        this.myName = myName;
        this.port = myName.getPort();
        this.dte = dte;
    }



    public InetSocketAddress getChordName()  {
        return myName;
    }

    public void createGroup(){
        serverThread = new ServerThread(dte,this);
        new Thread(serverThread).start();
    }

    public void joinGroup(InetSocketAddress knownPeer)  {

        try {
            // Setup successor
            sucSocket = new Socket(knownPeer.getAddress(),port);

            // Start listening for disconnects from successor
            DisconnectThread disconnectThread = new DisconnectThread(dte, this);
            new Thread(disconnectThread).start();

            System.out.println("EP spawned");
            dte.newEventPlayer(sucSocket);
            System.out.println("Wait for new predecessor");
            // Wait for new predecessor
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000);
            preSocket = serverSocket.accept();
            serverSocket.setSoTimeout(0);
            System.out.println("accepted");

            dte.newEventReplayer(preSocket);
            System.out.println("ERP spawned");
            // Keep listening for new joins
            serverThread = new ServerThread(dte,this,serverSocket);
            new Thread(serverThread).start();
            System.out.println("serverThread spawned");
        }catch ( java.io.InterruptedIOException e1 ) {
            System.out.println("Time out, must be first time");
            preSocket = sucSocket;
            System.out.println("accepted in exception");

            dte.newEventReplayer(preSocket);
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
        //When only 2 are in chord, suc and pre are the same
        if(sucSocket.equals(preSocket)){
            dte.sendRipEvent(true);
        }
        try {
            Socket socket = new Socket(preSocket.getInetAddress(), port+1);
            ObjectOutputStream disconnectStream = new ObjectOutputStream(socket.getOutputStream());
            disconnectStream.writeObject(new DisconnectEvent(sucSocket.getInetAddress()));
            leaving = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public boolean isLeaving(){
        return leaving;
    }

}