import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author User
 */
public class Server {
    static Map<String, String> users = new HashMap<>();
    static Map<String, String> userDirectories = new HashMap<>();

    static {
        users.put("Maciej12", "haslo1");
        users.put("Michal2", "haslo2");
    }

    private ServerSocket serverSocket;
    private String basePath;

    /**
     *
     * @param port
     * @throws IOException
     */
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        basePath = System.getProperty("user.home") + File.separator + "Dane_uzytkownikow";
        ensureUserDirectoriesExist();
    }

    private void ensureUserDirectoriesExist() {
        for (String userName : users.keySet()) {
            String userDirectory = basePath + File.separator + userName + "_files";
            File dir = new File(userDirectory);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("Directory " + userDirectory + " created successfully.");
                    userDirectories.put(userName, userDirectory);
                } else {
                    System.err.println("Failed to create directory " + userDirectory);
                }
            } else {
                userDirectories.put(userName, userDirectory);
            }
        }
    }

    /**
     *
     * @throws IOException
     */
    public void start() throws IOException {
        System.out.println("Server started");
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            Server server = new Server(2020);
            server.start();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String currentUser;
    private String currentDirectory;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = in.readUTF();
                switch (command) {
                    case "LOGIN":
                        handleLogin();
                        break;
                    case "LOGOUT":
                        handleLogout();
                        break;
                    case "UPLOAD":
                        handleUpload();
                        break;
                    case "DOWNLOAD":
                        handleDownload();
                        break;
                    case "CREATE_DIR":
                        handleCreateDirectory();
                        break;
                    case "BROWSE":
                        handleBrowseDirectory();
                        break;
                    case "IS_DIRECTORY":
                        handleIsDirectory();
                        break;
                    case "DELETE":
                        handleDelete();
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    private void handleLogin() throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();
        if (Server.users.containsKey(username) && Server.users.get(username).equals(password)) {
            currentUser = username;
            currentDirectory = Server.userDirectories.get(username);
            out.writeUTF("SUCCESS");
            System.out.println("User " + username + " logged in successfully. Directory: " + currentDirectory);
        } else {
            out.writeUTF("FAIL");
            System.out.println("User " + username + " failed to log in.");
        }
    }

    private void handleLogout() throws IOException {
        if (currentUser != null) {
            System.out.println("User " + currentUser + " logged out.");
            currentUser = null;
            currentDirectory = null;
            out.writeUTF("LOGGED_OUT_SUCCESSFULLY");
        } else {
            out.writeUTF("No user logged in.");
        }
    }

    private void handleUpload() throws IOException {
        String relativePath = in.readUTF();
        String fileName = in.readUTF();
        long fileSize = in.readLong();

        File file = new File(currentDirectory, relativePath + File.separator + fileName);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                out.writeUTF("FAIL");
                System.out.println("Failed to create directory structure for " + file.getAbsolutePath());
                return;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while (fileSize > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, read);
                fileSize -= read;
            }
        }
        out.writeUTF("SUCCESS");
        System.out.println("File " + fileName + " uploaded successfully to " + file.getAbsolutePath());
    }

    private void handleDownload() throws IOException {
        String relativePath = in.readUTF();
        String fileName = in.readUTF();
        File file = new File(currentDirectory, relativePath + File.separator + fileName);
        if (file.exists()) {
            out.writeUTF("SUCCESS");
            out.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            System.out.println("File " + fileName + " downloaded successfully from " + file.getAbsolutePath());
        } else {
            out.writeUTF("File not found");
            System.out.println("File " + fileName + " not found in " + file.getAbsolutePath());
        }
    }

    private void handleCreateDirectory() throws IOException {
        String relativePath = in.readUTF();
        String dirName = in.readUTF();
        File dir = new File(currentDirectory, relativePath + File.separator + dirName);
        if (!dir.exists()) {
            if (dir.mkdir()) {
                out.writeUTF("SUCCESS");
                System.out.println("Directory " + dirName + " created successfully in " + dir.getAbsolutePath());
            } else {
                out.writeUTF("FAIL");
                System.out.println("Failed to create directory " + dirName + " in " + dir.getAbsolutePath());
            }
        } else {
            out.writeUTF("Directory already exists");
            System.out.println("Directory " + dirName + " already exists in " + dir.getAbsolutePath());
        }
    }

    private void handleBrowseDirectory() throws IOException {
        String relativePath = in.readUTF();
        String dirName = in.readUTF();
        File dir;
        if (dirName.isEmpty()) {
            dir = new File(currentDirectory, relativePath);
        } else {
            dir = new File(currentDirectory, relativePath + File.separator + dirName);
        }

        if (dir.exists() && dir.isDirectory()) {
            out.writeUTF("SUCCESS");
            File[] files = dir.listFiles();
            out.writeInt(files.length);
            for (File file : files) {
                out.writeUTF(file.getName());
            }
        } else {
            out.writeUTF("Directory not found");
        }
    }

    private void handleIsDirectory() throws IOException {
        String path = in.readUTF();
        File file = new File(currentDirectory, path);
        out.writeBoolean(file.exists() && file.isDirectory());
    }

    private void handleDelete() throws IOException {
        String relativePath = in.readUTF();
        File file = new File(currentDirectory, relativePath);

        if (!file.exists()) {
            out.writeUTF("File or directory not found");
            return;
        }

        if (file.isDirectory()) {
            if (deleteDirectory(file)) {
                out.writeUTF("SUCCESS");
                System.out.println("Directory " + relativePath + " deleted successfully.");
            } else {
                out.writeUTF("FAIL");
                System.out.println("Failed to delete directory " + relativePath);
            }
        } else {
            if (file.delete()) {
                out.writeUTF("SUCCESS");
                System.out.println("File " + relativePath + " deleted successfully.");
            } else {
                out.writeUTF("FAIL");
                System.out.println("Failed to delete file " + relativePath);
            }
        }
    }

    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteDirectory(child)) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
}
