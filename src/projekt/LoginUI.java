import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class LoginUI {
    private JDialog dialog;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private Client client;
    private ClientUI clientUI;

    public LoginUI(Client client, ClientUI clientUI) {
        this.client = client;
        this.clientUI = clientUI;
        initComponents();
        attachEventHandlers();
        dialog.setVisible(true);
    }

    private void initComponents() {
        dialog = new JDialog((Frame) null, "Logowanie", true);
        dialog.setLayout(new GridLayout(3, 2));
        dialog.setSize(300, 150);

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        loginButton = new JButton("Zaloguj");

        dialog.add(new JLabel("Nazwa użytkownika:"));
        dialog.add(usernameField);
        dialog.add(new JLabel("Hasło:"));
        dialog.add(passwordField);
        dialog.add(new JLabel());
        dialog.add(loginButton);
    }

    private void attachEventHandlers() {
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                if (client.login(username, password)) {
                    clientUI.setLoggedInUser(username);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Nieprawidłowa nazwa użytkownika lub hasło");
                }
            } catch (IOException we) {
                JOptionPane.showMessageDialog(dialog, "Błąd logowania: " + we);
            }
        });
    }
}
