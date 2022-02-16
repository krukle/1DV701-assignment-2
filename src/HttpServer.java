import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Class for HTTP server.
 */
public class HttpServer {
  private final int portNumber;
  private final String rootDirectory;

  HttpServer(int portNumber, String rootDirectory) {
    this.portNumber = portNumber;
    this.rootDirectory = rootDirectory;
  }

  private void run() {
    try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
      System.out.println("Server started, listening on port " + portNumber);

      // Socket clientSocket = serverSocket.accept();
      // Thread thread = new Thread();
      while (true) {
        Socket clientSocket = serverSocket.accept();
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = 
            new PrintWriter(clientSocket.getOutputStream(), false);
        BufferedOutputStream dataOut = 
            new BufferedOutputStream(clientSocket.getOutputStream());
        String inputLine = in.readLine();

        do {
          System.out.println(inputLine);
          StringTokenizer parse = new StringTokenizer(inputLine);
          String method = parse.nextToken();
          String item = parse.nextToken();
          
          if (method.equalsIgnoreCase("GET")) {
            try {
              File file   = getPath(item);
              Path path   = file.toPath();
              String type = Files.probeContentType(path);

              out.println("HTTP/1.1 200 OK");
              out.println("Server: Java Server from Christoffer and Olof");
              out.println("Date: " + new Date());
              out.println("Content-type: " + ((type != null) ? type : "text/html"));
              out.println("Content-length: " + file.length());
              out.println();
              out.flush();
              
              byte[] data = Files.readAllBytes(path);
              dataOut.write(data);
              dataOut.flush();
            } catch (NoSuchFileException e) {
              System.err.println(e);
              fileNotFound(out, dataOut);
            }

    
          }
          in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } while ((inputLine = in.readLine()) != null);
      }
    } catch (IOException e) {
      System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
      System.out.println(e.getMessage());
    }
  }

  private File getPath(String item) {
    File htmlFile = new File(rootDirectory, item);
    if (htmlFile.isDirectory()) {
      htmlFile = new File(htmlFile, "index.html");
    }
    return htmlFile;
  }

  private void fileNotFound(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
    byte[] output = "404 Not Found".getBytes();
    out.println("HTTP/1.1 404 File Not Found");
    out.println("Server: Java Server from Christoffer and Olof");
    out.println("Date: " + new Date());
    out.println("Content-type: text/plain");
    out.println("Content-length: " + output.length);
    out.println();
    out.flush();
    dataOut.write(output);
    dataOut.flush();
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

    try {
      Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("The port needs to be a number.");
      System.exit(1);
    }

    HttpServer server = new HttpServer(Integer.parseInt(args[0]), args[1]);
    server.run();
  }
}