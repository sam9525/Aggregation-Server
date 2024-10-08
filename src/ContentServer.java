import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ContentServer {

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: ContentServer <server URL> <file path>");
      return;
    }

    String serverUrl = args[0];
    String filePath = args[1];

    try {
      // read data from txt
      Map<String, String> weatherData = readFile(filePath);

      // convert the weather data to json by using Gson
      Gson gson = new Gson();
      String json = gson.toJson(weatherData);

      // create URI
      URI uri = new URI(serverUrl + "/weather");
      URL url = uri.toURL();

      // send PUT request to the server
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("PUT");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);

      // Set Lamport Clock in request headers
      conn.setRequestProperty(
        "Lamport-Clock",
        String.valueOf(AggregationServer.LamportClock.getValue())
      );

      // output the weather data to the server
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = json.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      // get response code
      int responseCode = conn.getResponseCode();

      // Update Lamport Clock based on server response
      String serverClock = conn.getHeaderField("Lamport-Clock");
      if (serverClock != null) {
        int receivedClock = Integer.parseInt(serverClock);
        AggregationServer.LamportClock.updateAndGet(receivedClock);
      } else {
        // If no clock was received, just increment the local clock
        AggregationServer.LamportClock.incrementAndGet();
      }

      if (responseCode == 200 || responseCode == 201) {
        System.out.println("Weather data uploaded successfully.");
        System.out.println(
          "Current Lamport Clock: " + AggregationServer.LamportClock.getValue()
        );
      } else {
        System.out.println("Error: " + responseCode);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  // read the file
  private static Map<String, String> readFile(String filePath)
    throws IOException {
    Map<String, String> weatherData = new HashMap<>();
    List<String> lines = Files.readAllLines(Paths.get(filePath));

    for (String line : lines) {
      if (line.trim().isEmpty()) {
        continue;
      }
      String[] parts = line.split(":", 2);
      if (parts.length == 2) {
        weatherData.put(parts[0].trim(), parts[1].trim());
      }
    }
    return weatherData;
  }
}
