import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DisconnectThread implements Runnable {

    private final int port;
    private final ChordNameServiceImpl cns;
    private final DistributedTextEditor dte;
    private boolean running;

    public DisconnectThread(DistributedTextEditor dte, ChordNameServiceImpl cns) {
        this.cns = cns;
        this.dte = dte;
        port = cns.getMyName().getPort();
    }

    @Override
    public void run() {
        try {
            System.out.println("DisconnectThread creating ServerSocket with "+ port + "+1");
            ServerSocket server = new ServerSocket(port+1);
            while(running) {
                Socket disconnectingSocket = server.accept();
                server.close();
                ObjectInputStream disconnectStream = new ObjectInputStream(disconnectingSocket.getInputStream());
                DisconnectEvent de;
                while ((de = (DisconnectEvent) disconnectStream.readObject()) != null) {
                    //if Disconnect button is pressed, shut down
                    if (cns.isLeaving()) {//et RipEvent blev sendt ud af predecessoren, s� EventReplayer er lukket
                        while(!cns.getPreSocket().isClosed()){}
                        dte.sendRipEvent(false); //RipEvent lukker egen EventPlayer... lidt i tvivl her. Successor burde have oprettet en ny EventReplayer p� en anden socket
                        //dte.killEventPlayer();
                        //dte.killEventReplayer();
                        disconnectingSocket.close();
                        //cns.getSucSocket().close();
                        //cns.getPreSocket().close();
                        while(!cns.getSucSocket().isClosed()){}
                        terminate();
                    }
                    //else it is a soft disc when a node wants to join chord
                    else {
                        dte.sendRipEvent(false);
                        while(!cns.getSucSocket().isClosed()){}
                        InetSocketAddress newSuccessor = de.getNewSuccessor();
                        Socket newSuccessorSocket = new Socket(newSuccessor.getAddress(), newSuccessor.getPort());
                        cns.setSucSocket(newSuccessorSocket);
                        dte.newEventPlayer(cns.getSucSocket());
                        dte.sendAllText();
                        System.out.println("Connected to new peer");
                    }
                }
            }
            System.out.println("I am the DisconnectThread. Now I die;");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void terminate(){
        running = false;
    }
}
