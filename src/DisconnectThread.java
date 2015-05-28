import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;

public class DisconnectThread implements Runnable {

    private Socket socket;
    private final ChordNameServiceImpl cns;
    private final DistributedTextEditor dte;

    public DisconnectThread(DistributedTextEditor dte, ChordNameServiceImpl cns, Socket predecessor) {
        socket = predecessor;
        this.cns = cns;
        this.dte = dte;
    }

    @Override
    public void run() {
        try {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(true) {
                ObjectInputStream disconnectStream = new ObjectInputStream(socket.getInputStream());
                Object de;
                while ((de = disconnectStream.readObject()) != null) {
                    if (de instanceof DisconnectEvent) {
                        InetAddress newSuccessor = ((DisconnectEvent) de).getNewSuccessor();
                        cns.setSucSocket(new Socket(newSuccessor, cns.getChordName().getPort()));

                        dte.newEventPlayer(cns.getSucSocket(), cns.keyOfName(cns.getChordName()));

                        // New successor
                        socket = cns.getSucSocket();
                    } else if (de instanceof ConnectEvent) {
                        cns.setSucSocket(cns.getPreSocket());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
