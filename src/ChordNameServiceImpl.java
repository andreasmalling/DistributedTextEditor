import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChordNameServiceImpl extends Thread implements ChordNameService  {

    private DistributedTextEditor dte;
    private boolean joining;
    private int port;
    protected InetSocketAddress myName;
    protected int myKey;
    private InetSocketAddress suc;
    private InetSocketAddress pre;
    private InetSocketAddress connectedAt;
    private ServerSocket serverSocket;
    private Socket joiningSocket;
    private Socket preSocket;
    private Socket nextSocket;
    private boolean active;

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
        joining = false;
        active = true;
        myKey = keyOfName(myName);
        this.suc = getChordName();
        this.pre = getChordName();
        start();
    }

    public void joinGroup(InetSocketAddress knownPeer)  {
        joining = true;
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
        if(joining){
            //Create socket for connection with a listening DistributedTextEditor
            try{
                preSocket = new Socket(connectedAt.getAddress(), connectedAt.getPort());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            squeezeIn(preSocket);
            dte.setTitle("Connected to " + connectedAt);
            dte.newEventPlayer(nextSocket, myKey);
            dte.newEventReplayer(preSocket, myKey);
        }

        dte.setTitle("I'm listening on " + myName.getAddress()+":"+myName.getPort());
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(active) {
            //Create server socket and listen until another DistributedTextEditor connects
            try {
                joiningSocket = serverSocket.accept();
                acceptNode(joiningSocket);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        //leaveGroup sætter active=false, så her skal lukkes sockets og gøres rent

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

    public void squeezeIn(Socket preSocket){
        JoinEvent je = null;
        try {
            ObjectOutputStream out = new ObjectOutputStream(preSocket.getOutputStream());
            je = new JoinEvent(myName, Role.NEXT);
            out.writeObject(je);
            ObjectInputStream in = new ObjectInputStream(preSocket.getInputStream());
            while ((je = (JoinEvent) in.readObject()) != null) {
                //First time around, the listener returns himself as his own successor
                if(je.getName().equals(connectedAt)){
                    suc = pre;
                    nextSocket = preSocket;
                }
                else if(je.getRole().equals(Role.NEXT)){
                    suc=je.getName();
                    try{
                        nextSocket = new Socket(suc.getAddress(), suc.getPort());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                break;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void acceptNode(Socket joiningSocket){
        JoinEvent je = null;
        JoinEvent sucje = null;
        JoinEvent joiningje = null;
        InetSocketAddress newGuy = null;
        try {
            ObjectInputStream in = new ObjectInputStream(joiningSocket.getInputStream());
            ObjectOutputStream joiningOut = new ObjectOutputStream(joiningSocket.getOutputStream());
            while ((je = (JoinEvent) in.readObject()) != null) {
                if(je.getRole().equals(Role.NEXT)){
                    newGuy = je.getName();
                    //first time listener has himself as suc and pre
                    if(pre.equals(suc)){
                        //send self to joining node
                        joiningje = new JoinEvent(suc, Role.NEXT);
                        joiningOut.writeObject(joiningje);
                        //joining node is now pre and suc, EventPlayer and EventReplayer is spawned
                        preSocket = joiningSocket;
                        nextSocket = joiningSocket;
                        pre = newGuy;
                        suc = newGuy;
                        dte.newEventPlayer(nextSocket, myKey);
                        dte.newEventReplayer(preSocket, myKey);
                    }
                    else {
                        ObjectOutputStream sucOut = new ObjectOutputStream(nextSocket.getOutputStream());
                        //send new node to own successor
                        sucje = new JoinEvent(newGuy, Role.PREVIOUS);
                        sucOut.writeObject(sucje);
                        //send own successor to joining node
                        joiningje = new JoinEvent(suc, Role.NEXT);
                        joiningOut.writeObject(joiningje);
                        //drÃ¦b eventplayer, andens EventReplayer, mÃ¥ske et DisconnectEvent???
                        dte.disconnect();
                        //VED IKKE MED DEN HER
                        while (!nextSocket.isClosed()) {}
                        suc = newGuy;
                        nextSocket = joiningSocket;
                        dte.newEventPlayer(nextSocket, myKey);
                    }
                }
                else {//receive new node as new predecessor (je.getRole().equals(Role.PREVIOUS))
                    pre = je.getName();
                    preSocket.close();
                    preSocket = new Socket(pre.getAddress(),pre.getPort());
                    dte.newEventReplayer(preSocket, myKey);
                }
                break;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}