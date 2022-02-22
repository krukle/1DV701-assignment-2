package httpserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
  private PrintWriter header;
  private BufferedOutputStream payload;

  HttpServer(String rootDirectory, Socket clientSocket) {
    this.rootDirectory = rootDirectory;
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    System.out.println("Client connected, assigned a new thread.");
    try {
      header = new PrintWriter(clientSocket.getOutputStream(), false);
      payload = new BufferedOutputStream(clientSocket.getOutputStream());
      in = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));

      Map<String, String> headers = getHeaders(in);
      System.out.println("---------------------------------------------------------------------");
      System.out.println("Request from \"" + clientSocket.getRemoteSocketAddress() + "\":");
      System.out.println("  Method: " + headers.get("Method"));
      System.out.println("  Item: " + headers.get("Item"));
      System.out.println("  Version: " + headers.get("Version"));

      if (headers.get("Method").equalsIgnoreCase("GET")) {
        try {
          if (headers.get("Item").equalsIgnoreCase("/redirect.html")) {
            sendFile("index.html", StatusCode.REDIRECT);
          } else {
            sendFile(headers.get("Item"), StatusCode.OK);
          }
        } catch (NoSuchFileException e) {
          sendString(StatusCode.NOT_FOUND);
        }
      } else if (headers.get("Method").equalsIgnoreCase("POST")) {
        String fileName = "";
        while (!(fileName = in.readLine()).contains("filename=")) {} //Find file name
        while (!in.readLine().isBlank()) {} //Find start of image data
        fileName = fileName.split("filename=")[1].replace("\"", "");
        
        try {
          //TODO: Should user be able to overwite existing images?
          parseImage(getFile(fileName), in);
          send(StatusCode.OK, ("You will find your image at: <a href=\"/" + fileName + "\">" + fileName + "</>").getBytes(), "text/html");
        } catch (FileNotFoundException fnfe) {
          sendString(StatusCode.BAD_REQUEST);
          fnfe.printStackTrace();
        } catch (IllegalArgumentException iae) {
          sendString(StatusCode.BAD_REQUEST);
          iae.printStackTrace();
        }
      } else {
        sendString(StatusCode.INTERNAL_SERVER_ERROR);
      }
    } catch (SocketException se) {
      se.printStackTrace();
      System.err.println("Unable to access closed socket.");
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.err.println("Something went wrong. Closing thread.");
    } finally {
      try {
        in.close();
        header.close();
        payload.close();
        clientSocket.close();
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        System.err.println("Error closing stream: " + e.getMessage());
      }
    }
  }

  /**
   * Read the headers from the BufferedReader in and put them in a HashMap.
   *
   * @param in The BufferedReader
   * @return The HashMap containing the headers.
   */
  private Map<String, String> getHeaders(BufferedReader in) throws IOException {
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
   * @param file The File to write the data to.
   * @param in The BufferedReader to read data from.
   */
  private void parseImage(File file, BufferedReader in) 
      throws IOException, IllegalArgumentException {
    FileType fileType = FileType.fromString(
        Optional.ofNullable(file.getName()).filter(
          f -> f.contains(".")).map(f -> f.substring(file.getName().lastIndexOf(".") + 1)).get());  
    FileOutputStream fos = new FileOutputStream(file);
    List<Integer> test   = new ArrayList<>();

    while (test.add(in.read())) {
      if (test.size() == fileType.start.size()) {
        if (test.equals(fileType.start)) {
          test.clear();
          break;
        }
        test.remove(0);
      }
    }
    for (Integer b : fileType.start) {
      fos.write(b);
    }
    int ch = 0;
    while ((ch = in.read()) != -1) {
      test.add(ch);
      if (test.size() == fileType.end.size()) {
        if (test.equals(fileType.end)) {
          fos.write(ch);
          break;
        }
        test.remove(0);
      }
      fos.write(ch);
    }
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
    File htmlFile = new File(rootDirectory, item);
    if (htmlFile.isDirectory()) {
      throw new FileNotFoundException("The requested item is a directory.");
    }
    return htmlFile;
  }

  /**
   * Generates a header message using statusCode and sends it through PrintWriter; header.
   * Generates a bytearray from statusCode and sends it through BufferedOutputStream; payload.
   *
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
    File file;
    try {
      file   = getFile(item);
    } catch (FileNotFoundException e) {
      file = new File(rootDirectory, "index.html");
    }
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
      }
    } catch (IOException e) {
      System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
      System.out.println(e.getMessage());
    }
  }
}