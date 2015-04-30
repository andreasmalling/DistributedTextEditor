
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class EventPlayer implements Runnable {

    private Socket socket;
    private DocumentEventCapturer dec;
    private boolean running = true;

    public EventPlayer(Socket socket, DocumentEventCapturer dec) {
        this.dec = dec;
        this.socket = socket;
    }

    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            while (running) {
                //Take every MyTextEvent and send it to the connected DistributedTextEditor's EventReplayer
                MyTextEvent mte = dec.take();
                out.writeObject(mte);
                //If the MyTextEvent received is a DisconnectEvent, close the thread
                if (mte instanceof DisconnectEvent) {
                    terminate();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("I'm the thread running the EventPlayer, now I die!");
    }

    public void terminate() {
        running = false;
    }
}
