import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class kNode implements Runnable {
    Socket predecessor, successor, joining;
    ServerSocket sNode3;

    int port;
    private DistributedTextEditor dte;

    public kNode(DistributedTextEditor dte, int port) {
        this.dte = dte;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            sNode3 = new ServerSocket(port);

            // First client joins cirkel jerk
            joining = sNode3.accept();
            predecessor = joining;
            successor = joining;
            joining = null;

            // Other jerks
            while (true) {
                joining = sNode3.accept();

                ObjectInputStream joinIn = new ObjectInputStream(joining.getInputStream());

                JoinEvent je = (JoinEvent) joinIn.readObject();

                if (je.getRole() == Role.SUCCESSOR) {
                    successorJoin();
                } else if (je.getRole() == Role.PREDECESSOR) {
                    predecessorJoin();

                    throw new Exception("No valid role");
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void predecessorJoin() throws IOException {

        ObjectOutputStream joinOut = null;

        joinOut = new ObjectOutputStream(joining.getOutputStream());
        TextInsertEvent tie = new TextInsertEvent(0, dte.getAllText());

        joinOut.writeObject(tie);

        BufferedReader joinIn = new BufferedReader(new InputStreamReader(joining.getInputStream()));

        String response;
        while ((response = joinIn.readLine()) != null) {        // TODO: Time out?
        }

        if(response.equals("ok")){
            successor = joining;
            joining = null;
            dte.newSuccessor(successor);
        }
    }

    private void successorJoin() throws IOException {
        InetAddress preIP = predecessor.getInetAddress();

        ObjectOutputStream joinOut = null;

        joinOut = new ObjectOutputStream(joining.getOutputStream());
        joinOut.writeObject(new JoinEvent(preIP, Role.PREDECESSOR));

        BufferedReader joinIn = new BufferedReader(new InputStreamReader(joining.getInputStream()));

        String response;
        while ((response = joinIn.readLine()) != null) {        // TODO: Time out?
        }

        if(response.equals("ok")){
            predecessor = joining;
            joining = null;
            dte.newPredecessor(predecessor);
        }
    }
}
