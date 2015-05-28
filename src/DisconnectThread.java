import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
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
        port = cns.getChordName().getPort();
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(port+1);
            while(running) {
                Socket disconnectingSocket = server.accept();
                ObjectInputStream disconnectStream = new ObjectInputStream(disconnectingSocket.getInputStream());
                DisconnectEvent de;
                while ((de = (DisconnectEvent) disconnectStream.readObject()) != null) {
                    //if Disconnect button is pressed, shut down
                    if (cns.isLeaving()) {//et RipEvent blev sendt ud af predecessoren, så EventReplayer er lukket
                        dte.sendRipEvent(); //RipEvent lukker egen EventPlayer... lidt i tvivl her. Successor burde have oprettet en ny EventReplayer på en anden socket
                        //dte.killEventPlayer();
                        //dte.killEventReplayer();
                        disconnectingSocket.close();
                        //cns.getSucSocket().close();
                        //cns.getPreSocket().close();
                        terminate();
                    }
                    //else it is a soft disc when a node wants to join chord
                    else {
                        dte.sendRipEvent();
                        InetAddress newSuccessor = de.getNewSuccessor();
                        cns.setSucSocket(new Socket(newSuccessor, cns.getChordName().getPort()));
                        dte.newEventPlayer(cns.getSucSocket(), cns.keyOfName(cns.getChordName()));
                        dte.sendAllText();
                        System.out.println("Connected to new peer");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void terminate(){
        running = false;
    }
}
