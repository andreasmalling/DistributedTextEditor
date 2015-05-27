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
    private Socket sucSocket;
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
            dte.newEventPlayer(sucSocket, myKey);
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
                processNode(joiningSocket);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        //we want to leave the chord
        JoinEvent je = null;
        try {
            //Send own suc to pre, so it knows which to connect on
            ObjectOutputStream preout = new ObjectOutputStream(preSocket.getOutputStream());
            je = new JoinEvent(suc, Role.ABORTSUCCESSOR);
            preout.writeObject(je);
            //We cut connection with own suc
            dte.disconnect();
            //DETTE KUNNE VIRKE, hvis pre hele tiden s� hvad der kommer ind i streamen, alts� altid er i processNode. Det er den bare ikke :(((

        } catch (IOException e) {
            e.printStackTrace();
        }

        //leaveGroup s�tter active=false, s� her skal lukkes sockets og g�res rent

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
            je = new JoinEvent(myName, Role.SUCCESSOR);
            out.writeObject(je);
            ObjectInputStream in = new ObjectInputStream(preSocket.getInputStream());
            while ((je = (JoinEvent) in.readObject()) != null) {
                //First time around, the listener returns himself as his own successor
                if(je.getName().equals(connectedAt)){
                    suc = pre;
                    sucSocket = preSocket;
                }
                else if(je.getRole().equals(Role.SUCCESSOR)){
                    suc=je.getName();
                    sucSocket = new Socket(suc.getAddress(), suc.getPort());
                }
                break;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void processNode(Socket joiningSocket){
        JoinEvent je;
        JoinEvent sucje;
        JoinEvent joiningje;
        InetSocketAddress newGuy;
        try {
            ObjectInputStream in = new ObjectInputStream(joiningSocket.getInputStream());
            ObjectOutputStream joiningOut = new ObjectOutputStream(joiningSocket.getOutputStream());
            while ((je = (JoinEvent) in.readObject()) != null) {
                if(je.getRole().equals(Role.SUCCESSOR)){
                    newGuy = je.getName();
                    //first time listener has himself as suc and pre
                    if(pre.equals(suc)){
                        //send self to joining node
                        joiningje = new JoinEvent(suc, Role.SUCCESSOR);
                        joiningOut.writeObject(joiningje);
                        //joining node is now pre and suc, EventPlayer and EventReplayer is spawned
                        preSocket = joiningSocket;
                        sucSocket = joiningSocket;
                        pre = newGuy;
                        suc = newGuy;
                        dte.newEventPlayer(sucSocket, myKey);
                        dte.newEventReplayer(preSocket, myKey);
                    }
                    else {
                        ObjectOutputStream sucOut = new ObjectOutputStream(sucSocket.getOutputStream());
                        //send new node to own successor
                        sucje = new JoinEvent(newGuy, Role.PREDECESSOR);
                        sucOut.writeObject(sucje);
                        //send own successor to joining node
                        joiningje = new JoinEvent(suc, Role.SUCCESSOR);
                        joiningOut.writeObject(joiningje);
                        //dræb eventplayer, andens EventReplayer, måske et DisconnectEvent???
                        dte.disconnect();
                        //VED IKKE MED DEN HER
                        while (!sucSocket.isClosed()) {}
                        suc = newGuy;
                        sucSocket = joiningSocket;
                        dte.newEventPlayer(sucSocket, myKey);
                        Runnable discoStream = new Runnable() {
                            @Override
                            public void run() {
                                ObjectInputStream discoIn = null;
                                JoinEvent discoje = null;
                                try {
                                    discoIn = new ObjectInputStream(sucSocket.getInputStream());
                                    while ((discoje = (JoinEvent) discoIn.readObject()) != null) {
                                        if(discoje.getRole().equals(Role.ABORTSUCCESSOR)){
                                            //node is leaving, so we need new successor
                                            suc = discoje.getName();
                                            //cut connection to old suc
                                            dte.disconnect();
                                            sucSocket = new Socket(suc.getAddress(), suc.getPort());
                                            break;
                                        }
                                    }
                                } catch (IOException | ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        discoStream.run();
                    }
                }
                else {//receive new node as new predecessor (je.getRole().equals(Role.PREVIOUS))
                    pre = je.getName();
                    //ved ikke med close, det burde den gamle predecessors disconnect have taget sig af
                    //preSocket.close();
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