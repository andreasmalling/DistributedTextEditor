
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

    private DistributedTextEditor distributedTextEditor;
    private Socket socket;
    private JTextArea area;
    private boolean running = true;

    public EventReplayer(Socket socket, JTextArea area, DistributedTextEditor distributedTextEditor) {
        this.area = area;
        this.socket = socket;
        this.distributedTextEditor = distributedTextEditor;
    }

    public void run() {
        try {
            while (running) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                MyTextEvent mte = null;
                try {
                    while ((mte = (MyTextEvent) in.readObject()) != null) {
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
                        } else if (mte instanceof DisconnectEvent) {
                            terminate();
                            //The DisconnectEvent send first should make own client send a DisconnectEvent and then close itself
                            if(((DisconnectEvent) mte).shouldDisconnect()) {
                                distributedTextEditor.disconnect();
                            }
                            //Second and last DisconnectEvent should close socket
                            else socket.close();
                            break;
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