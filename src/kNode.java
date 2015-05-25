import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class kNode implements Runnable {
    Socket predecessor, successor, joining;
    ServerSocket serverSocket;

    int port;
    private DistributedTextEditor dte;

    public kNode(DistributedTextEditor dte, int port) {
        this.dte = dte;
        this.port = port;
    }

    public Socket getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Socket predecessor) {
        this.predecessor = predecessor;
    }

    public Socket getSuccessor() {
        return successor;
    }

    public void setSuccessor(Socket successor) {
        this.successor = successor;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);

            // First client joins cirkel jerk
            joining = serverSocket.accept();

            // Throw away JoinEvent (to successor)
            ObjectInputStream inputStream = new ObjectInputStream(joining.getInputStream());
            inputStream.readObject();

            // Send self as predecessor
            ObjectOutputStream joinOut = new ObjectOutputStream(joining.getOutputStream());
            joinOut.writeObject(new JoinEvent(InetAddress.getLocalHost(), Role.PREDECESSOR));

            // OBS, client have to check if SUCCESSOR and suc are the same

            // Send all text
            TextInsertEvent tie = new TextInsertEvent(0, dte.getAllText());
            joinOut.writeObject(tie);

            // OK response
            BufferedReader inResponse = new BufferedReader(new InputStreamReader(joining.getInputStream()));
            String response;
            while ((response = inResponse.readLine()) != null) {        // TODO: Time out?
            }

            if(response.equals("ok")){
                successor = joining;
                predecessor = joining;
                joining = null;
                dte.newSuccessor(successor);
                dte.newPredecessor(predecessor);
            }

            // Other jerks
            while (true) {
                System.out.println("In while");
                joining = serverSocket.accept();

                JoinEvent je = (JoinEvent) inputStream.readObject();

                if (je.getRole() == Role.SUCCESSOR) {
                    successorJoin();
                } else if (je.getRole() == Role.PREDECESSOR) {
                    predecessorJoin();
                } else
                    throw new Exception("No valid role");
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
