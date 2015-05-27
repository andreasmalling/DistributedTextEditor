import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChordNameServiceImpl extends Thread implements ChordNameService  {

    private DistributedTextEditor dte;
    private int port;
    protected InetSocketAddress myName;
    protected int myKey;
    private InetSocketAddress suc;
    private InetSocketAddress pre;
    private InetSocketAddress connectedAt;
    private ServerSocket serverSocket;
    private Socket joiningSocket;
    private Socket preSocket;
    private Socket sucSocket;
    private boolean active;
    private boolean first;

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

    public void createGroup() {
        active = true;
        first = true;
        myKey = keyOfName(myName);
        this.suc = getChordName();
        this.pre = getChordName();
        start();
    }

    public void joinGroup(InetSocketAddress knownPeer)  {
        active = true;
        this.port = knownPeer.getPort();
        connectedAt = knownPeer;
        myKey = keyOfName(myName);
        this.pre = knownPeer;
        start();
    }

    public void leaveGroup() {
        active = false;
    }

    public void run() {
        System.out.println("My name is " + myName + " and my key is " + myKey);
        while(active) {
            //Create server socket and listen until another DistributedTextEditor connects
            try {
                joiningSocket = serverSocket.accept();
                if(first){
                    firstConnection(joiningSocket);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
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

    private void firstConnection(Socket joiningSocket) {
        preSocket = joiningSocket;
        dte.newEventPlayer(preSocket, myKey);
        sucSocket = joiningSocket;
        dte.newEventReplayer(sucSocket, myKey);
        first = false;
    }

}