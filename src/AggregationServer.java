import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.Executors;

public class AggregationServer {

  private static final int DEFAULT_PORT = 4567;
  private static final String WEATHER_DATA_FILE = "weather_data.json";

  public static void main(String[] args) throws IOException {
    int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/weather", new WeatherHandler());
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Server is running on port: " + port);
  }

  static class WeatherHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String method = exchange.getRequestMethod();

      // determine the request method
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

    private void handlePutRequest(HttpExchange exchange) throws IOException {
      // input the weather data
      InputStream is = exchange.getRequestBody();
      String body = new String(is.readAllBytes());
      is.close();

      if (isValidJson(body)) {
        // Write the validated data to the weather_data.json file
        Files.write(Paths.get(WEATHER_DATA_FILE), body.getBytes());
        exchange.sendResponseHeaders(200, 0);
      } else {
        exchange.sendResponseHeaders(500, 0);
      }
      exchange.close();
    }

    // check if the provided string is valid JSON
    private boolean isValidJson(String json) {
      try {
        new Gson().fromJson(json, Map.class);
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }
}
