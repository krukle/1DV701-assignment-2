import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class for HTTP server.
 */
public class HttpServer implements Runnable {
  private final String rootDirectory;
  private Socket clientSocket = null;
  private BufferedReader in;
  private PrintWriter header;
  private BufferedOutputStream payload;

  enum StatusCode {
    OK(200, "OK"),
    REDIRECT(302, "Found"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    public int code;
    public String msg;

    StatusCode(int code, String msg) {
      this.code = code;
      this.msg = msg;
    }

    @Override
    public String toString() {
      return this.code + " " + this.msg;
    }
  }

  HttpServer(String rootDirectory, Socket clientSocket) {
    this.rootDirectory = rootDirectory;
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      header = new PrintWriter(clientSocket.getOutputStream(), false);
      payload = new BufferedOutputStream(clientSocket.getOutputStream());
      InputStream is = clientSocket.getInputStream();
      in = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
      // in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String request = in.readLine();
      StringTokenizer parse = new StringTokenizer(request);
      String method = parse.nextToken();
      String item = parse.nextToken();
      String version = parse.nextToken();
      System.out.println("---------------------------------------------------------------------");
      System.out.println("Request from \"" + clientSocket.getRemoteSocketAddress() + "\":");
      System.out.println("  Method: " + method);
      System.out.println("  Path: " + item);
      System.out.println("  Version: " + version);

      if (method.equalsIgnoreCase("GET")) {
        try {
          if (item.equalsIgnoreCase("/redirect.html")) {
            sendFile("index.html", StatusCode.REDIRECT);
          } else {
            sendFile(item, StatusCode.OK);
          }
        } catch (NoSuchFileException e) {
          sendString(StatusCode.NOT_FOUND);
        }
      } else if (method.equalsIgnoreCase("POST")) {
        //TODO: Fix crash when no image is selected and Upload button is pressed.
        String boundary = "";
        String fileName = "";
        while (!(boundary = in.readLine()).contains("boundary=")) {} //Find boundary
        while (!(fileName = in.readLine()).contains("filename=")) {} //Find file name
        while (!in.readLine().isBlank()) {}                          //Find start of image data
        
        fileName = fileName.split("filename=")[1].replace("\"", "");
        FileOutputStream fos = new FileOutputStream(new File(rootDirectory, fileName));
        List<Integer> test = new ArrayList<>();

        // Construct end array
        List<Integer> end = new ArrayList<>(Arrays.asList(13, 10, 45, 45));
        boundary.split("boundary=")[1].chars().map(x -> (int)x).forEach(i -> end.add(i));

        int ch = 0;
        while ((ch = in.read()) != -1) {
          test.add(ch);
          if (test.size() == end.size()) {
            if (test.equals(end)) {
              break;
            }
            test.remove(0);
          }
          fos.write(ch);
        }
        fos.flush();
        fos.close();
        send(StatusCode.OK, ("You will find your image at: <a href=\"/" + fileName + "\">" + fileName + "</>").getBytes(), "text/html");
      } else {
        sendString(StatusCode.INTERNAL_SERVER_ERROR);
      }
    } catch (SocketException se) {
      se.printStackTrace();
      System.err.println("Unable to access closed socket.");
      Thread.currentThread().interrupt();
      return;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.err.println("Something went wrong. Closing thread.");
    } finally {
      try {
        in.close();
        header.close();
        payload.close();
        clientSocket.close();
      } catch (Exception e) {
        System.err.println("Error closing stream: " + e.getMessage());
      }
    }
  }

  /**
   * Get a File from the root directory.
   *
   * @param item The name of item to look for.
   * @return File object of the specified item.
   */
  private File getFile(String item) {
    File htmlFile = new File(rootDirectory, item);
    if (htmlFile.isDirectory()) {
      htmlFile = new File(htmlFile, "index.html");
    }
    return htmlFile;
  }

  /**
   * Generates a header message using statusCode and sends it through PrintWriter; header.
   * Generates a bytearray from statusCode and sends it through BufferedOutputStream; payload.
   * @param statusCode HTTP status code.
   * @throws IOException If an I/O occurs in BufferedOutputStream; payload.
   */
  private void sendString(StatusCode statusCode) throws IOException {
    String status = statusCode.toString();
    byte[] output = status.getBytes();
    send(statusCode, output, "text/plain");
  }

  /**
   * Send a HTTP 200 OK with specified item as payload.
   *
   * @param item The name of the item to look for.
   * @throws IOException if the file can't be read.
   */
  private void sendFile(String item, StatusCode statusCode) throws IOException {
    final File file   = getFile(item);
    final Path path   = file.toPath();
    final String type = Files.probeContentType(path);
    final byte[] data = Files.readAllBytes(path);
    send(statusCode, data, type);
  }

  private void send(StatusCode statusCode, byte[] data, String type) throws IOException {
    String version = "HTTP/1.1 ";
    String status = statusCode.toString();
    String server = "Server: Java Server from Christoffer and Olof";
    String date = "Date: " + new Date();
    String contentType = "Content-type: " + ((type != null) ? type : "text/html");
    String length = "Content-length: " + data.length;

    System.out.println("Response:");
    System.out.println("  Version: " + version);
    System.out.println("  Status: " + status);
    System.out.println("  " + String.join("\r\n  ", server, date, contentType, length));
    header.println(String.join("\r\n", (version + status), server, date, contentType, length));
    header.println();
    header.flush();
    payload.write(data);
    payload.flush();
  }

  /**
   * Verifies arguments and runs an instance of HttpServer. 
   *
   * @param args Integer; port number & String; path.
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println(
          "Usage: java HttpServer <port number> <relative path to /public directory>");
      System.exit(1);
    }

    int portNumber = 80;

    try {
      portNumber = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("The port needs to be a number.");
      System.exit(1);
    }

    String rootDirectory = args[1];
    Path path = Path.of(System.getProperty("user.dir"), rootDirectory);
    if (!Files.isDirectory(path)) {
      System.err.println("The directory \"" + path.toString() + "\" does not exist.");
      System.exit(1);
    }

    try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
      System.out.println("Server listening on port " + portNumber);
      System.out.println("Serving content from: " + path.toString());
      while (true) {
        HttpServer server = new HttpServer(path.toString(), serverSocket.accept());
        Thread thread = new Thread(server);
        thread.start();
        System.out.println();
        System.out.println("Client connected, assigned a new thread.");
      }
    } catch (IOException e) {
      System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
      System.out.println(e.getMessage());
    }
  }
}