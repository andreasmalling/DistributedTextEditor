
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class DistributedTextEditor extends JFrame {

    private final int id;
    private JTextArea area1 = new JTextArea(20,120);
    private JTextField ipaddress = new JTextField("localhost");
    private JTextField portNumber = new JTextField("43921");

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;

    private DocumentEventCapturer dec = new DocumentEventCapturer();
    private Socket socket;
    private ServerSocket serverSocket;

    JupiterSynchronizer jupiterSynchronizer = new JupiterSynchronizer();
    private EventReplayer eventReplayer;
    private EventPlayer eventPlayer;

    public DistributedTextEditor() {
        // Generate "unique" id
        id = (int) (10000 * Math.random());

        area1.setFont(new Font("Monospaced",Font.PLAIN,12));
        ((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1,BorderLayout.CENTER);

        content.add(ipaddress,BorderLayout.CENTER);
        content.add(portNumber,BorderLayout.CENTER);

        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMB.add(file);
        JMB.add(edit);

        file.add(Listen);
        file.add(Connect);
        file.add(Disconnect);
        file.addSeparator();
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);

        edit.add(Copy);
        edit.add(Paste);
        edit.getItem(0).setText("Copy");
        edit.getItem(1).setText("Paste");

        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        area1.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);
        area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
                "Try to type and delete stuff in the top area.\n" +
                "Then figure out how it works.\n", 0);
    }

    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
            dec.toggleMakeEvents(true);
        }
    };

    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            listen();
        }
    };

    private void listen() {
        //Prepare editor for connection
        saveOld();
        area1.setText("");
        changed = false;
        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        //Get own address for hosting
        String address = null;
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e1) {
            e1.printStackTrace();
        }
        int port = Integer.parseInt(portNumber.getText());
        setTitle("I'm listening on "+address + ":" + port);

        //Create server socket and listen until another DistributedTextEditor connects
        kNode mykNode = new kNode(this, port);
        Thread kNodeThread = new Thread(mykNode);
        kNodeThread.run();
    }

    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            //Prepare editor for connection
            saveOld();
            area1.setText("");
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);

            //Connect with a listening DistributedTextEditor
            String address = ipaddress.getText();
            int port = Integer.parseInt(portNumber.getText());
            setTitle("Connecting to " + address +":"+ port + "...");

            //Create socket for connection with a listening DistributedTextEditor
            try{
                connect(address, port);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
            //Create threads for sending and receiving text
            establishConnection(socket, dec);
            setTitle("Connected to " + address + ":" + port);

        }
    };

    private void connect(String ip, int port) throws IOException, ClassNotFoundException {
        Socket sucSocket = new Socket(ip, port);
        ObjectOutputStream outputStream = new ObjectOutputStream(sucSocket.getOutputStream());
        outputStream.writeObject(new JoinEvent(InetAddress.getLocalHost(), Role.SUCCESSOR));

        ObjectInputStream inputStream = new ObjectInputStream(sucSocket.getInputStream());
        JoinEvent je = (JoinEvent) inputStream.readObject();

        if( je.getIp().toString().equals(ip) ){
            // Receive all text
            TextInsertEvent tie = (TextInsertEvent) inputStream.readObject();
            area1.setText(tie.getText());

            // TODO: set suc and pre and start server

            // Acknowledge
            PrintWriter out = new PrintWriter(sucSocket.getOutputStream());
            out.write("ok");

        } else {
            Socket preSocket = new Socket(je.getIp(), port);

            // Ask for new predecessor
            je = new JoinEvent(InetAddress.getLocalHost(), Role.PREDECESSOR);
            ObjectOutputStream preOutputStream = new ObjectOutputStream(preSocket.getOutputStream());
            preOutputStream.writeObject(je);

            // Receive all text
            TextInsertEvent tie = (TextInsertEvent) inputStream.readObject();
            area1.setText(tie.getText());

            // TODO: set suc and pre and start server

            // Acknowledge
            PrintWriter out = new PrintWriter(sucSocket.getOutputStream());
            out.write("ok");
            out = new PrintWriter(preSocket.getOutputStream());
            out.write("ok");
        }

    }

    //Disconnect method for this DistributedTextEditor
    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            try {
                DisconnectEvent disconnectEvent = new DisconnectEvent(0);
                disconnectEvent.setShouldDisconnect();
                dec.eventHistory.put(disconnectEvent);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            setTitle("Disconnected");
            jupiterSynchronizer.clear();
        }
    };

    //Disconnect method for the connected DistributedTextEditor
    public void disconnect(){
        try {
            dec.eventHistory.put(new DisconnectEvent(0));
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        setTitle("Disconnected");
        jupiterSynchronizer.clear();
    }

    Action Save = new AbstractAction("Save") {
        public void actionPerformed(ActionEvent e) {
            if(!currentFile.equals("Untitled"))
                saveFile(currentFile);
            else
                saveFileAs();
        }
    };

    Action SaveAs = new AbstractAction("Save as...") {
        public void actionPerformed(ActionEvent e) {
            saveFileAs();
        }
    };

    Action Quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            System.exit(0);
        }
    };

    ActionMap m = area1.getActionMap();

    Action Copy = m.get(DefaultEditorKit.copyAction);
    Action Paste = m.get(DefaultEditorKit.pasteAction);
    private void saveFileAs() {
        if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if(changed) {
            if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        }
        catch(IOException e) {
        }
    }

    private void establishConnection(Socket socket, DocumentEventCapturer dec) {
        newPredecessor(socket);
        newSuccessor(socket);
    }

    public void newPredecessor(Socket predecessor){
        setTitle("Connected to " + predecessor.getInetAddress().toString() + ":" +  + predecessor.getPort());

        eventReplayer = new EventReplayer(predecessor, area1, this, jupiterSynchronizer);
        Thread ert = new Thread(eventReplayer);
        System.out.println("new erp");
        ert.start();
    }

    public void newSuccessor(Socket successor){
        eventPlayer = new EventPlayer(successor, dec, this, id, jupiterSynchronizer);
        Thread ept = new Thread(eventPlayer);
        System.out.println("New ep");
        ept.start();
    }

    public String getAllText(){
        return area1.getText();
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}