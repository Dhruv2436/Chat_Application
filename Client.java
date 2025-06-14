import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client {
    private JFrame frame;
    private JPanel chatPanel;
    private JTextField messageField;
    private JScrollPane scrollPane;
    private String username;
    private PrintWriter out;
    private StringBuilder chatHistory = new StringBuilder(); // For saving chats
    private JButton sendButton; // Now a class-level variable

    public Client() {
        frame = new JFrame("Client Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.getContentPane().setBackground(Color.decode("#121212"));
        frame.setLayout(new BorderLayout());

        // Title bar
        JLabel title = new JLabel(" Client Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setBackground(Color.decode("#1E1E1E"));
        title.setOpaque(true);
        title.setPreferredSize(new Dimension(frame.getWidth(), 40));
        frame.add(title, BorderLayout.NORTH);

        // Chat area
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.decode("#121212"));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Input and send button
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.WHITE);

        messageField = new JTextField();
        sendButton = new JButton("Send"); // Store in class-level variable
        sendButton.setBackground(Color.decode("#128C7E"));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setPreferredSize(new Dimension(frame.getWidth(), 40));
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Action listeners
        messageField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());

        // Menu for downloading chat
        JMenuBar menuBar = new JMenuBar();
        JMenuItem downloadItem = new JMenuItem("Download Chat");
        downloadItem.addActionListener(e -> downloadChat());
        menuBar.add(downloadItem);
        frame.setJMenuBar(menuBar);

        frame.setVisible(true);

        username = promptForUsername();
        if (username == null) {
            System.exit(0);
        }

        frame.setTitle("Client Dashboard - " + username);
        displayMessage("Hello, @" + username + "\nWelcome", false);

        connectToServer();
    }

    private String promptForUsername() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel("Enter your username:");
        JTextField textField = new JTextField(15);
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);
        panel.add(errorLabel, BorderLayout.SOUTH);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    frame, panel, "Username", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return null;
            String input = textField.getText().trim();
            if (!input.isEmpty()) return input;
            else errorLabel.setText("Enter username");
        }
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 12345);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username); // Send username

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        displayMessage(line, false);
                    }
                    showDisconnectedMessage(); // Server closed
                } catch (IOException e) {
                    showDisconnectedMessage(); // Exception occurred
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Server Error\nUnable to connect to the server at localhost:12345",
                    "Connection Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            String fullMessage = username + ": " + text;
            displayMessage(fullMessage, true);
            out.println(fullMessage);
            messageField.setText("");
        }
    }

    private void displayMessage(String text, boolean isOwnMessage) {
        chatHistory.append(text).append("\n"); // Save to history

        JPanel messagePanel = new JPanel(new BorderLayout());

        JTextArea bubble = new JTextArea(text);
        bubble.setWrapStyleWord(true);
        bubble.setLineWrap(true);
        bubble.setEditable(false);
        bubble.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bubble.setForeground(Color.WHITE);
        bubble.setBorder(new EmptyBorder(8, 10, 8, 10));

        boolean isWelcome = text.contains("Welcome") && text.contains("@");

        if (isWelcome) bubble.setBackground(Color.decode("#3C3C3C"));
        else if (isOwnMessage) bubble.setBackground(Color.decode("#128C7E"));
        else if (text.contains("⚠")) bubble.setBackground(Color.decode("#8B0000"));
        else bubble.setBackground(Color.decode("#2E2E2E"));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(bubble, isOwnMessage ? BorderLayout.EAST : BorderLayout.WEST);
        wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));
        messagePanel.add(wrapper, BorderLayout.CENTER);

        JLabel timeLabel = new JLabel(new SimpleDateFormat("HH:mm").format(new Date()));
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(Color.LIGHT_GRAY);

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setOpaque(false);
        timePanel.add(timeLabel, isOwnMessage ? BorderLayout.EAST : BorderLayout.WEST);
        messagePanel.add(timePanel, BorderLayout.SOUTH);

        chatPanel.add(messagePanel);
        chatPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void downloadChat() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Chat");
        int result = fileChooser.showSaveDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(chatHistory.toString());
                JOptionPane.showMessageDialog(frame, "Chat saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Failed to save chat.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Called when server is disconnected
    private void showDisconnectedMessage() {
        SwingUtilities.invokeLater(() -> {
            displayMessage("⚠ Server disconnected. You can no longer send messages.", false);
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
