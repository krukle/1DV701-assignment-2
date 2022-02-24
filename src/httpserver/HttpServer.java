package httpserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Class for HTTP server.
 */
public class HttpServer implements Runnable {
  private final String rootDirectory;
  private Socket clientSocket = null;
  private BufferedReader in;
  private BufferedOutputStream out;

  HttpServer(String rootDirectory, Socket clientSocket) {
    this.rootDirectory = rootDirectory;
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    System.out.println("Client connected, assigned a new thread.");
    try {
      out = new BufferedOutputStream(clientSocket.getOutputStream());
      in  = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));

      Map<String, String> headers = getHeaders();
      printRequestDetails(headers);

      try {
        File item = getFile(headers.get("Item"));

        if (headers.get("Method").equalsIgnoreCase("GET") && item != null) {
          sendFile(item, StatusCode.OK); 
        } else if (headers.get("Method").equalsIgnoreCase("POST") && item != null) {
          String fileName = getFilename();
          parseImage(Integer.parseInt(headers.get("Content-Length")), 
            new File(rootDirectory, fileName));
          send(StatusCode.OK,
              ("You will find your image at: <a href=\"/" + fileName + "\">" + fileName + "</a>")
              .getBytes(), "text/html");
        } else if (headers.get("Item").equalsIgnoreCase("/redirect.html")) {
          sendFile(new File(rootDirectory, "index.html"), StatusCode.REDIRECT);
        } else if (item == null) {
          sendStatusCode(StatusCode.NOT_FOUND);
        } else {
          sendStatusCode(StatusCode.INTERNAL_SERVER_ERROR);
        }
      } catch (FileNotFoundException | IllegalArgumentException e) {
        sendStatusCode(StatusCode.BAD_REQUEST);
      }
    } catch (SocketException se) {
      System.err.println("Unable to access closed socket: " + se.getMessage());
    } catch (IOException ioe) {
      System.err.println("Something went wrong. Closing thread: " + ioe.getMessage());
    
    } finally {
      try {
        in.close();
        out.close();
        clientSocket.close();
        Thread.currentThread().interrupt();
        return;
      } catch (IOException ioe) {
        System.err.println("Error closing stream: " + ioe.getMessage());
      }
    }
  }

  /**
   * Finds filename in request body.
   *
   * @return the filename.
   * @throws IOException if an I/O error occurs in inputstream.
   */
  private String getFilename() throws IOException {
    String fileName = "";
    while (!(fileName = in.readLine()).contains("filename=")) {} //Find file name
    return fileName.split("filename=")[1].replace("\"", "");
  }

  /**
   * Read the headers from the BufferedReader in and put them in a HashMap.
   *
   * @param in The BufferedReader
   * @return The HashMap containing the headers.
   */
  private Map<String, String> getHeaders() throws IOException {
    Map<String, String> headers = new HashMap<>();
    String header = in.readLine();
    StringTokenizer parse = new StringTokenizer(header);
    headers.put("Method", parse.nextToken());
    headers.put("Item", parse.nextToken());
    headers.put("Version", parse.nextToken());
    while (!(header = in.readLine()).isBlank()) {
      String[] kv = header.split(": ");
      headers.put(kv[0], kv[1]);
    }
    return headers;
  }

  /**
   * Parse the image data.
   *
   * @param contentLength The length of the content.
   * @param file The File to write the data to.
   */
  private void parseImage(int contentLength, File file) 
      throws IOException, IllegalArgumentException {
    while (!in.readLine().isBlank()) {} //Find start of image data
    char[] charData = new char[contentLength];
    in.read(charData);
    byte[] data = new String(charData).getBytes(StandardCharsets.ISO_8859_1);
    
    FileType fileType = FileType.fromString(
        Optional.ofNullable(file.getName()).filter(
          f -> f.contains(".")).map(f -> f.substring(file.getName().lastIndexOf(".") + 1)).get()); 
 
    int startIndex = KmpMatch.indexOf(data, fileType.start);
    int endIndex = KmpMatch.indexOf(data, fileType.end);
    if (startIndex < 0 || endIndex < 0) {
      return;
    }
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(data, startIndex, endIndex + fileType.end.length);
    fos.flush();
    fos.close();
  }

  /**
   * Get a File from the root directory.
   *
   * @param item The name of item to look for.
   * @return File object of the specified item.
   * @throws FileNotFoundException if the item is a directory.
   */
  private File getFile(String item) throws FileNotFoundException {
    File f = new File(rootDirectory, item);
    if (f.isDirectory()) {
      if (f.listFiles((dir, name) -> name.equals("index.html")).length == 1) {
        return f = new File(f, "index.html");
      }
    }
    return f.exists() ? f : null;
  }

  /**
   * Print the request details.
   *
   * @param headers The headers to get details from.
   */
  private void printRequestDetails(Map<String, String> headers) {
    System.out.println("---------------------------------------------------------------------");
    System.out.println("Request from \"" + clientSocket.getRemoteSocketAddress() + "\":");
    System.out.println("  Method: " + headers.get("Method"));
    System.out.println("  Item: " + headers.get("Item"));
    System.out.println("  Version: " + headers.get("Version"));
  }

  /**
   * Send a status code to outputstream.
   *
   * @param statusCode the statusCode to send.
   * @throws IOException if an I/O error occurs in BufferedOuutputStream.
   */
  private void sendStatusCode(StatusCode statusCode) throws IOException {
    String status = statusCode.toString();
    byte[] output = status.getBytes();
    send(statusCode, output, "text/plain");
  }

  /**
   * Send a HTTP 200 OK with specified item as payload.
   *
   * @param item The name of the item to look for.
   * @param statusCode the status code to send.
   * @throws IOException if the file can't be read.
   * @throws NoSuchFileException if the item does not point to a file.
   */
  private void sendFile(File item, StatusCode statusCode) 
      throws IOException, NoSuchFileException {

    final Path path   = item.toPath();
    final String type = Files.probeContentType(path);
    final byte[] data = Files.readAllBytes(path);
    send(statusCode, data, type);
  }

  /**
   * Writes data to outputstream with following status code and type.
   *
   * @param statusCode the status code to be sent.
   * @param data the data to be sent.
   * @param type the type of data.
   * @throws IOException if an I/O error occurs in BufferedOutputStream.
   */
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

    out.write(
        (String.join("\r\n", (version + status), server, date, contentType, length)).getBytes());
    out.write("\r\n\r\n".getBytes());
    out.write(data);
    out.flush();
  }

  /**
   * Verifies arguments and runs an instance of HttpServer. 
   *
   * @param args Integer port number & String path.
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
      }
    } catch (IOException e) {
      System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
      System.out.println(e.getMessage());
    }
  }
}