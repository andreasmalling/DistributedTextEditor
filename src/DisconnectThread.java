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
            while(true) {
                ObjectInputStream disconnectStream = new ObjectInputStream(socket.getInputStream());
                DisconnectEvent de;
                while ((de = (DisconnectEvent) disconnectStream.readObject()) != null) {
                }

                InetAddress newSuccessor = de.getNewSuccessor();
                cns.setSucSocket(new Socket(newSuccessor, cns.getChordName().getPort()));

                dte.newEventPlayer(cns.getSucSocket(), cns.keyOfName(cns.getChordName()));

                // New successor
                socket = cns.getSucSocket();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
