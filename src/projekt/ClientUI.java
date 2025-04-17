import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class ClientUI {
    private JFrame frame;
    private JMenuBar menuBar;
    private JMenu fileMenu, loginMenu;
    private JMenuItem loginMenuItem, logoutMenuItem, uploadMenuItem, downloadMenuItem, createDirMenuItem, browseMenuItem, backMenuItem;
    private JFileChooser fileChooser;
    private JList<String> fileList;
    private JScrollPane scrollPane;
    private Client client;
    private Stack<String> navigationStack;
    private JMenuItem deleteMenuItem;

    public ClientUI(Client client) {
        this.client = client;
        this.navigationStack = new Stack<>();
        initComponents();
        attachEventHandlers();
        frame.setVisible(true);
    }

    private void initComponents() {
        frame = new JFrame("Klient");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        menuBar = new JMenuBar();
        fileMenu = new JMenu("Plik");
        loginMenu = new JMenu("Logowanie");

        loginMenuItem = new JMenuItem("Zaloguj");
        logoutMenuItem = new JMenuItem("Wyloguj");
        uploadMenuItem = new JMenuItem("Prześlij");
        downloadMenuItem = new JMenuItem("Pobierz");
        createDirMenuItem = new JMenuItem("Utwórz katalog");
        browseMenuItem = new JMenuItem("Przeglądaj");
        backMenuItem = new JMenuItem("Wstecz");
        deleteMenuItem = new JMenuItem("Usuń");

        loginMenu.add(loginMenuItem);
        loginMenu.add(logoutMenuItem);
        fileMenu.add(uploadMenuItem);
        fileMenu.add(downloadMenuItem);
        fileMenu.add(createDirMenuItem);
        fileMenu.add(browseMenuItem);
        fileMenu.add(backMenuItem);
        fileMenu.add(deleteMenuItem);

        menuBar.add(loginMenu);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        fileChooser = new JFileChooser();
        fileList = new JList<>();
        scrollPane = new JScrollPane(fileList);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void attachEventHandlers() {
        loginMenuItem.addActionListener(e -> new LoginUI(client, this));
        logoutMenuItem.addActionListener(e -> logout());
        uploadMenuItem.addActionListener(e -> uploadFile());
        downloadMenuItem.addActionListener(e -> downloadFile());
        createDirMenuItem.addActionListener(e -> createDirectory());
        browseMenuItem.addActionListener(e -> browseDirectory());
        backMenuItem.addActionListener(e -> navigateBack());
        deleteMenuItem.addActionListener(e -> delete());

        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedFileName = fileList.getSelectedValue();
                    if (selectedFileName != null) {
                        try {
                            String currentDirectory = String.join(File.separator, navigationStack);
                            if (client.isDirectory(currentDirectory + File.separator + selectedFileName)) {
                                navigateToDirectory(selectedFileName);
                            } else {
                                System.out.println("Wybrany plik nie jest katalogiem.");
                            }
                        } catch (IOException we) {
                            JOptionPane.showMessageDialog(frame, "Błąd sprawdzania katalogu: " + we);
                        }
                    }
                }
            }
        });
    }

    private void uploadFile() {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String currentDirectory = String.join(File.separator, navigationStack);
                client.uploadFile(file, currentDirectory);
                JOptionPane.showMessageDialog(frame, "Plik przesłany pomyślnie");
                refreshCurrentDirectory();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Przesyłanie nie powiodło się: " + e);
            }
        }
    }

    private void delete() {
        String path = JOptionPane.showInputDialog(frame, "Wpisz nazwę pliku lub katalogu do usunięcia:");
        if (path != null) {
            try {
                String currentDirectory = String.join(File.separator, navigationStack);
                boolean isFileExists = client.isDirectory(currentDirectory + File.separator + path);
                if (isFileExists) {
                    client.delete(currentDirectory + File.separator + path);
                    refreshCurrentDirectory();
                } else {
                    JOptionPane.showMessageDialog(frame, "Plik nie istnieje.");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Usuwanie nie powiodło się: " + e);
            }
        }
    }

    private void downloadFile() {
        String fileName = JOptionPane.showInputDialog(frame, "Wpisz nazwę pliku do pobrania:");
        if (fileName != null) {
            try {
                String currentDirectory = String.join(File.separator, navigationStack);
                client.downloadFile(fileName, currentDirectory);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Pobieranie nie powiodło się: " + e);
            }
        }
    }

    private void createDirectory() {
        String dirName = JOptionPane.showInputDialog(frame, "Wpisz nazwę nowego katalogu:");
        if (dirName != null) {
            try {
                String currentDirectory = String.join(File.separator, navigationStack);
                client.createDirectory(dirName, currentDirectory);
                refreshCurrentDirectory();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Tworzenie katalogu nie powiodło się: " + e);
            }
        }
    }

    private void browseDirectory() {
        String dirName = JOptionPane.showInputDialog(frame, "Wpisz nazwę katalogu do przeglądania:");
        if (dirName != null) {
            navigateToDirectory(dirName);
        }
    }

    private void navigateToDirectory(String dirName) {
        String currentDirectory = String.join(File.separator, navigationStack);
        try {
            if (client.isDirectory(currentDirectory + File.separator + dirName)) {
                navigationStack.push(dirName);
                refreshCurrentDirectory();
            } else {
                JOptionPane.showMessageDialog(frame, "Katalog nie istnieje.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Błąd sprawdzania katalogu: " + e);
        }
    }

    private void navigateBack() {
        if (!navigationStack.isEmpty()) {
            navigationStack.pop();
            refreshCurrentDirectory();
        }
    }

    private void refreshCurrentDirectory() {
        String currentDirectory = String.join(File.separator, navigationStack);
        System.out.println("Odświeżanie bieżącego katalogu: " + currentDirectory);
        try {
            String[] files = client.browseDirectory("", currentDirectory);
            fileList.setListData(files);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Błąd przeglądania katalogu: " + e);
        }
    }

    public void setLoggedInUser(String username) {
        frame.setTitle("Klient - Zalogowany jako " + username);
        refreshCurrentDirectory();
    }

    private void logout() {
        try {
            client.logout();
            JOptionPane.showMessageDialog(frame, "Wylogowano pomyślnie.");
            navigationStack.clear();
            frame.setTitle("Klient");
            fileList.setListData(new String[0]);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Błąd wylogowania: " + e);
        }
    }
}
