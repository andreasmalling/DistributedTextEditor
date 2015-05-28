
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class EventPlayer implements Runnable {

    private JupiterSynchronizer jupiterSynchronizer;
    private int id;
    private DistributedTextEditor distributedTextEditor;
    private Socket socket;
    private DocumentEventCapturer dec;
    private boolean running = true;
    private ObjectOutputStream out;

    public EventPlayer(Socket socket, DocumentEventCapturer dec, DistributedTextEditor distributedTextEditor, int id, JupiterSynchronizer jupiterSynchronizer) {
        this.dec = dec;
        this.socket = socket;
        this.distributedTextEditor = distributedTextEditor;
        this.id = id;
        this.jupiterSynchronizer = jupiterSynchronizer;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("EP " + socket.toString());
            while (running) {
                //Take every MyTextEvent and send it to the connected DistributedTextEditor's EventReplayer
                MyTextEvent mte = dec.take();
                mte = jupiterSynchronizer.generate(mte);
                mte.setId(id);
                out.writeObject(mte);
                if(mte instanceof RipEvent){
                    System.out.println("EP Received RipEvent");
                    terminate();
                }
            }
            //socket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("I'm the thread running the EventPlayer, now I die!");
    }

    public void terminate() {
        running = false;
    }
}
