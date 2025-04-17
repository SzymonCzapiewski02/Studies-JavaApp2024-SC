import java.io.*;
import java.net.*;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String currentUser;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }
    
    public String getCurrentUser() {
        return currentUser;
    }

    public boolean login(String username, String password) throws IOException {
        out.writeUTF("LOGIN");
        out.writeUTF(username);
        out.writeUTF(password);
        String response = in.readUTF();
        if (response.equals("SUCCESS")) {
            currentUser = username;
            return true;
        }
        return false;
    }
    
    public void logout() throws IOException {
        out.writeUTF("LOGOUT");
        String response = in.readUTF();
        if (response.equals("LOGGED_OUT_SUCCESSFULLY")) {
            currentUser = null;
            System.out.println("Wylogowano pomyślnie.");
        } else {
            System.out.println("Błąd wylogowania: " + response);
        }
    }

    public void uploadFile(File file, String currentDirectory) throws IOException {
        if (!file.exists()) {
            System.out.println("File does not exist: " + file.getAbsolutePath());
            return;
        }
        out.writeUTF("UPLOAD");
        out.writeUTF(currentDirectory);
        out.writeUTF(file.getName());
        out.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        String response = in.readUTF();
        if (response.equals("SUCCESS")) {
            System.out.println("File uploaded successfully: " + file.getName());
        } else {
            System.out.println("Failed to upload file: " + response);
        }
    }

    public void downloadFile(String fileName, String currentDirectory) throws IOException {
        out.writeUTF("DOWNLOAD");
        out.writeUTF(currentDirectory);
        out.writeUTF(fileName);

        String response = in.readUTF();
        if (response.equals("SUCCESS")) {
            long fileSize = in.readLong();
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = fileChooser.showSaveDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = fileChooser.getSelectedFile();
                if (selectedDirectory.exists() && selectedDirectory.isDirectory()) {
                    File file = new File(selectedDirectory, fileName);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while (fileSize > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                            fos.write(buffer, 0, read);
                            fileSize -= read;
                        }
                    }
                    System.out.println("Plik pobrany pomyślnie: " + fileName);
                } else {
                    System.out.println("Wybrany katalog nie istnieje lub nie jest katalogiem.");
                }
            } else {
                System.out.println("Pobieranie anulowane przez użytkownika.");
            }
        } else {
            throw new IOException(response);
        }
    }

    public boolean isDirectory(String path) throws IOException {
        out.writeUTF("IS_DIRECTORY");
        out.writeUTF(path);
        return in.readBoolean();
    }

    public void createDirectory(String dirName, String currentDirectory) throws IOException {
        out.writeUTF("CREATE_DIR");
        out.writeUTF(currentDirectory);
        out.writeUTF(dirName);
        String response = in.readUTF();
        if (response.equals("SUCCESS")) {
            System.out.println("Directory created successfully: " + dirName);
        } else {
            System.out.println("Failed to create directory: " + response);
        }
    }

    public void delete(String path) throws IOException {
        out.writeUTF("DELETE");
        out.writeUTF(path);
        String response = in.readUTF();
        if (response.equals("SUCCESS")) {
            System.out.println("Deleted successfully: " + path);
        } else {
            System.out.println("Failed to delete: " + response);
        }
    }

    public String[] browseDirectory(String dirName, String currentDirectory) throws IOException {
        out.writeUTF("BROWSE");
        out.writeUTF(currentDirectory);
        out.writeUTF(dirName);

        String response = in.readUTF();
        if (response.equals("SUCCESS")) {
            int fileCount = in.readInt();
            String[] files = new String[fileCount];
            for (int i = 0; i < fileCount; i++) {
                files[i] = in.readUTF();
            }
            return files;
        } else {
            throw new IOException(response);
        }
    }
}

class main {
    public static void main(String[] args) {
        try {
            Client client = new Client("localhost", 2020);
            SwingUtilities.invokeLater(() -> new ClientUI(client));
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
