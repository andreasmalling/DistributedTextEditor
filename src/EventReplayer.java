
import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 *
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

    private JupiterSynchronizer jupiterSynchronizer;
    private DistributedTextEditor distributedTextEditor;
    private Socket socket;
    private JTextArea area;
    private boolean running = true;
    private ObjectInputStream in;


    public EventReplayer(Socket socket, JTextArea area, DistributedTextEditor distributedTextEditor, JupiterSynchronizer jupiterSynchronizer) {
        this.area = area;
        this.socket = socket;
        this.distributedTextEditor = distributedTextEditor;
        this.jupiterSynchronizer = jupiterSynchronizer;
    }

    public void updateSocket(Socket socket) {
        this.socket = socket;
        try {
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            System.out.println("ERP " + socket.toString());
            while (running) {
                in = new ObjectInputStream(socket.getInputStream());
                MyTextEvent mte = null;
                try {
                    while ((mte = (MyTextEvent) in.readObject()) != null && running) {
                        mte = jupiterSynchronizer.receive(mte);
                        if (mte instanceof TextInsertEvent) {
                            final TextInsertEvent tie = (TextInsertEvent) mte;
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        area.insert(tie.getText(), tie.getOffset());
                                    } catch (Exception e) {
                                        System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                                    }
                                }
                            });
                        } else if (mte instanceof TextRemoveEvent) {
                            final TextRemoveEvent tre = (TextRemoveEvent) mte;
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                                    } catch (Exception e) {
                                        System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception _) {
                    _.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }
    public void terminate() {
        running = false;
    }
}