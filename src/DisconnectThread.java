import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DisconnectThread implements Runnable {

    private final int port;
    private final ChordNameServiceImpl cns;
    private final DistributedTextEditor dte;
    private Socket disconnectingSocket;

    public DisconnectThread(DistributedTextEditor dte, ChordNameServiceImpl cns) {
        this.cns = cns;
        this.dte = dte;
        port = cns.getChordName().getPort();
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(43922);
            while(true) {
                disconnectingSocket = server.accept();
                ObjectInputStream disconnectStream = new ObjectInputStream(disconnectingSocket.getInputStream());
                DisconnectEvent de;
                while ((de = (DisconnectEvent) disconnectStream.readObject()) != null) {
                    InetAddress newSuccessor = de.getNewSuccessor();
                    cns.setSucSocket(new Socket(newSuccessor, cns.getChordName().getPort()));
                    dte.newEventPlayer(cns.getSucSocket(), cns.keyOfName(cns.getChordName()));
                    System.out.println("allahu akbar");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
