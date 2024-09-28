import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AggregationServer {

  public class LamportClock {

    private static final AtomicInteger lamportClock = new AtomicInteger(0);

    public static int getValue() {
      return lamportClock.get();
    }

    public static int incrementAndGet() {
      return lamportClock.incrementAndGet();
    }

    public static int updateAndGet(int receivedClock) {
      return lamportClock.updateAndGet(localClock ->
        Math.max(receivedClock, localClock) + 1
      );
    }
  }

  private static final int DEFAULT_PORT = 4567;
  private static final String WEATHER_DATA_FILE = "weather_data.json";
  private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private static final int MAX_SECONDS = 30;
  private static long lastCommunicationTime = System.currentTimeMillis();

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
      // get the request method
      String method = exchange.getRequestMethod();

      // Check for Lamport Clock in request headers
      String clientClock = exchange
        .getRequestHeaders()
        .getFirst("Lamport-Clock");
      if (clientClock != null) {
        int receivedClock = Integer.parseInt(clientClock);
        // Update the local clock: max(received clock, local clock) + 1
        LamportClock.updateAndGet(receivedClock);
      } else {
        // If no clock was received, just increment the local clock
        LamportClock.incrementAndGet();
      }

      // Set the updated Lamport Clock in the response headers
      exchange
        .getResponseHeaders()
        .set("Lamport-Clock", String.valueOf(LamportClock.getValue()));

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

    private void handleGetRequest(HttpExchange exchange) throws IOException {
      rwLock.readLock().lock();
      try {
        File file = new File(WEATHER_DATA_FILE);
        if (file.exists()) {
          // Read the data from the file
          String json = new String(Files.readAllBytes(file.toPath()));

          // Parse the JSON to update the Lamport Clock if necessary
          Gson gson = new Gson();
          Map<String, Object> weatherData = gson.fromJson(
            json,
            new TypeToken<Map<String, Object>>() {}.getType()
          );

          // Check if the data is older than 30 seconds
          if (
            System.currentTimeMillis() -
            lastCommunicationTime >
            MAX_SECONDS *
            1000
          ) {
            // Data is too old, delete it
            file.delete();
            exchange.sendResponseHeaders(204, -1);
            return;
          }
          // Update the Lamport Clock in the JSON if it's older than the current clock
          int storedClock =
            (
              (Double) weatherData.getOrDefault("lamport_clock", 0.0)
            ).intValue();
          if (LamportClock.getValue() > storedClock) {
            weatherData.put("lamport_clock", LamportClock.getValue());
            json = gson.toJson(weatherData);
            // Write the updated JSON back to the file
            Files.write(Paths.get(WEATHER_DATA_FILE), json.getBytes());
          }

          // Update the last communication time
          lastCommunicationTime = System.currentTimeMillis();

          exchange
            .getResponseHeaders()
            .set("Lamport-Clock", String.valueOf(LamportClock.getValue()));
          exchange.sendResponseHeaders(200, json.length());
          OutputStream os = exchange.getResponseBody();
          os.write(json.getBytes());
          os.close();
        } else {
          exchange
            .getResponseHeaders()
            .set("Lamport-Clock", String.valueOf(LamportClock.getValue()));
          exchange.sendResponseHeaders(204, -1);
        }
      } finally {
        rwLock.readLock().unlock();
        exchange.close();
      }
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
      rwLock.writeLock().lock();
      try {
        // input the weather data
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes());
        is.close();

        if (isValidJson(body)) {
          // Parse the JSON
          Gson gson = new Gson();
          Map<String, Object> weatherData = gson.fromJson(
            body,
            new TypeToken<Map<String, Object>>() {}.getType()
          );

          // Add the Lamport Clock to the JSON data
          weatherData.put("lamport_clock", LamportClock.getValue());

          // Convert back to JSON
          String updatedJson = gson.toJson(weatherData);

          // Write the validated data to the weather_data.json file
          Files.write(Paths.get(WEATHER_DATA_FILE), updatedJson.getBytes());

          // Update the last communication time
          lastCommunicationTime = System.currentTimeMillis();

          exchange.sendResponseHeaders(200, 0);
        } else {
          exchange.sendResponseHeaders(500, 0);
        }
      } finally {
        rwLock.writeLock().unlock();
        exchange.close();
      }
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
