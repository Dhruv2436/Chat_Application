import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private JFrame frame;
    private JPanel chatPanel;
    private JTextField messageField;
    private JScrollPane scrollPane;
    private JComboBox<String> userDropdown;
    private static final int PORT = 12345;

    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Server() {
        frame = new JFrame("Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.getContentPane().setBackground(Color.decode("#121212"));
        frame.setLayout(new BorderLayout());

        JLabel title = new JLabel(" Server Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setBackground(Color.decode("#1E1E1E"));
        title.setOpaque(true);
        title.setPreferredSize(new Dimension(frame.getWidth(), 40));
        frame.add(title, BorderLayout.NORTH);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.decode("#121212"));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.WHITE);

        userDropdown = new JComboBox<>();
        userDropdown.addItem("All");

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        sendButton.setBackground(Color.decode("#128C7E"));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        inputPanel.add(userDropdown, BorderLayout.WEST);
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setPreferredSize(new Dimension(frame.getWidth(), 40));
        frame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendServerMessage());
        messageField.addActionListener(e -> sendServerMessage());

        frame.setVisible(true);

        new Thread(this::startServer).start();
    }

    private void sendServerMessage() {
        String msg = messageField.getText().trim();
        if (!msg.isEmpty() && !clients.isEmpty()) {
            String target = (String) userDropdown.getSelectedItem();
            String fullMsg = "[Server]: " + msg;
            displayMessage(fullMsg, true);
            if ("All".equalsIgnoreCase(target)) {
                for (ClientHandler ch : clients) {
                    ch.sendMessage(fullMsg);
                }
            } else {
                for (ClientHandler ch : clients) {
                    if (ch.getUsername().equalsIgnoreCase(target)) {
                        ch.sendMessage(fullMsg);
                        break;
                    }
                }
            }
            messageField.setText("");
        } else if (clients.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No connected clients!", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void displayMessage(String text, boolean isServerMessage) {
        JPanel messagePanel = new JPanel(new BorderLayout());

        JTextArea bubble = new JTextArea(text);
        bubble.setWrapStyleWord(true);
        bubble.setLineWrap(true);
        bubble.setEditable(false);
        bubble.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bubble.setForeground(Color.WHITE);
        bubble.setBorder(new EmptyBorder(8, 10, 8, 10));

        boolean isConnectionInfo = text.startsWith("[Connected]") || text.startsWith("[Disconnected]");

        bubble.setBackground(isConnectionInfo ? Color.decode("#2E2E2E")
                : (isServerMessage ? Color.decode("#128C7E") : Color.decode("#2E2E2E")));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(bubble, isServerMessage && !isConnectionInfo ? BorderLayout.EAST : BorderLayout.WEST);
        wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));

        messagePanel.add(wrapper, BorderLayout.CENTER);

        JLabel timeLabel = new JLabel(new SimpleDateFormat("HH:mm").format(new Date()));
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(Color.LIGHT_GRAY);

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setOpaque(false);
        timePanel.add(timeLabel, isServerMessage && !isConnectionInfo ? BorderLayout.EAST : BorderLayout.WEST);
        messagePanel.add(timePanel, BorderLayout.SOUTH);

        chatPanel.add(messagePanel);
        chatPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            displayMessage("Server is running on port " + PORT, true);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            displayMessage("Server Error: " + e.getMessage(), true);
        }
    }

    public void updateClientsDropdown() {
        SwingUtilities.invokeLater(() -> {
            userDropdown.removeAllItems();
            userDropdown.addItem("All");
            for (ClientHandler ch : clients) {
                userDropdown.addItem(ch.getUsername());
            }
        });
    }

    public void showClientMessage(String msg) {
        displayMessage(msg, false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private Server server;

        public ClientHandler(Socket socket, Server server) {
            this.socket = socket;
            this.server = server;
        }

        public String getUsername() {
            return username;
        }

        public void sendMessage(String msg) {
            if (out != null) out.println(msg);
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = in.readLine(); // First line from client
                server.displayMessage("[Connected] " + username, false);
                server.updateClientsDropdown();

                String msg;
                while ((msg = in.readLine()) != null) {
                    server.showClientMessage(msg);
                }
            } catch (IOException e) {
                server.displayMessage("[Disconnected] " + username, false);
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                clients.remove(this);
                server.updateClientsDropdown();
            }
        }
    }
}
