
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.net.*;

public class DistributedTextEditor extends JFrame {

    private JTextArea area1 = new JTextArea(20,120);
    private JTextField ipaddress = new JTextField("localhost");
    private JTextField portNumber = new JTextField("43921");

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;

    private DocumentEventCapturer dec = new DocumentEventCapturer();

    private JupiterSynchronizer jupiterSynchronizer = new JupiterSynchronizer();

    private ChordNameServiceImpl chordNameService;
    private EventPlayer ep = null;
    private EventReplayer er = null;

    public DistributedTextEditor() {
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

    /**
     * Computes the name of this peer by resolving the local host name
     * and adding the current portname.
     */
    protected InetSocketAddress _getMyName() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            InetSocketAddress name = new InetSocketAddress(localhost, Integer.parseInt(portNumber.getText()));
            return name;
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
        }
        return null;
    }

    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            listen();
        }
    };

    public void listen(){
        //Prepare editor for connection
        saveOld();
        area1.setText("");
        changed = false;
        Save.setEnabled(false);
        SaveAs.setEnabled(false);
        Connect.setEnabled(false);
        InetSocketAddress name = _getMyName();

        chordNameService = new ChordNameServiceImpl(name, this);
        chordNameService.createGroup();
    }

    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            connect();
        }
    };

    public void connect(){
        //Prepare editor for connection
        saveOld();
        area1.setText("");
        changed = false;
        Save.setEnabled(false);
        SaveAs.setEnabled(false);
        Listen.setEnabled(false);

        //Connect with a listening DistributedTextEditor
        String address = ipaddress.getText();
        int port = Integer.parseInt(portNumber.getText());
        InetSocketAddress knownPeer = new InetSocketAddress(address, port);

        InetSocketAddress name = _getMyName();

        chordNameService = new ChordNameServiceImpl(name, this);
        chordNameService.joinGroup(knownPeer);
    }

    //Disconnect method for this DistributedTextEditor
    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            chordNameService.leaveGroup();
            setTitle("Disconnected");
            jupiterSynchronizer.clear();
        }
    };

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

    public void newEventPlayer(Socket socket, int id){
        if (ep == null) {
            System.out.println("EP: i am null");
            ep = new EventPlayer(socket, dec, this, id, jupiterSynchronizer);
            Thread ept = new Thread(ep);
            ept.start();
        } else{
            System.out.println("EP not null");
            //killEventPlayer();
            ep = new EventPlayer(socket, dec, this, id, jupiterSynchronizer);
            Thread ept = new Thread(ep);
            ept.start();
        }
    }

    public void killEventPlayer(){
        ep.terminate();
    }

    public void newEventReplayer(Socket socket, int id){
        if(er == null) {
            System.out.println("ERP: i am null");
            er = new EventReplayer(socket, area1, this, jupiterSynchronizer);
            Thread ert = new Thread(er);
            ert.start();
        } else {
            System.out.println("ERP not null");
            //killEventReplayer();
            er = new EventReplayer(socket, area1, this, jupiterSynchronizer);
            Thread ert = new Thread(er);
            ert.start();
        }
    }

    public void killEventReplayer(){
        er.terminate();
    }

    public void sendRipEvent(){
        try {
            dec.eventHistory.put(new RipEvent());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendAllText(){
        try {
            dec.eventHistory.put(new TextInsertEvent(0, area1.getText()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}