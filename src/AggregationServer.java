import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class AggregationServer {

  private static final int DEFAULT_PORT = 4567;

  public static void main(String[] args) throws IOException {
    int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Server is running on port: " + port);
  }

  static class WeatherHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String method = exchange.getRequestMethod();

      if (method.equalsIgnoreCase("GET")) {
        handleGetRequest(exchange);
      } else if (method.equalsIgnoreCase("PUT")) {
        handlePutRequest(exchange);
      } else {
        exchange.sendResponseHeaders(400, 0);
        exchange.close();
      }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {}

    private void handlePutRequest(HttpExchange exchange) throws IOException {}
  }
}
