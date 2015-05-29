
import java.awt.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class EventPlayer implements Runnable {

    private JupiterSynchronizer jupiterSynchronizer;
    private DistributedTextEditor distributedTextEditor;
    private Socket socket;
    private DocumentEventCapturer dec;
    private boolean running = true;
    private ObjectOutputStream out;

    public EventPlayer(Socket socket, DocumentEventCapturer dec, DistributedTextEditor distributedTextEditor, JupiterSynchronizer jupiterSynchronizer) {
        this.dec = dec;
        this.socket = socket;
        this.distributedTextEditor = distributedTextEditor;
        this.jupiterSynchronizer = jupiterSynchronizer;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("EP " + socket.toString());
            LinkedBlockingQueue directLine = distributedTextEditor.getDirectLine();
            //Thread that sends on own events
            while (running) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        //Take every MyTextEvent and send it to the connected DistributedTextEditor's EventReplayer
                        MyTextEvent ownMTE = null;
                        try {
                            ownMTE = dec.take();
                            ownMTE = jupiterSynchronizer.generate(ownMTE);
                            out.writeObject(ownMTE);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                //Thread that sends all other events
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        //Take every MyTextEvent and send it to the connected DistributedTextEditor's EventReplayer
                        MyTextEvent otherMTE = null;
                        try {
                            otherMTE = (MyTextEvent) directLine.take();
                            otherMTE = jupiterSynchronizer.generate(otherMTE);
                            out.writeObject(otherMTE);
                            if(otherMTE instanceof RipEvent){
                                System.out.println("EP Received RipEvent");
                                terminate();
                            }
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (IOException  e) {
            e.printStackTrace();
        }
        System.out.println("I'm the thread running the EventPlayer, now I die!");
    }

    public void terminate() {
        running = false;
    }
}
