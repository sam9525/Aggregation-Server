import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.Map;

public class GETClient {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: GETClient <server URL>");
      return;
    }

    String serverUrl = args[0];

    try {
      // create URI
      URI uri = new URI(serverUrl + "/weather");
      URL url = uri.toURL();

      // send GET request to the server
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");

      // Set Lamport Clock in request headers
      conn.setRequestProperty(
        "Lamport-Clock",
        String.valueOf(AggregationServer.LamportClock.getValue())
      );

      int responseCode = conn.getResponseCode();

      // Update Lamport Clock based on server response
      String serverClock = conn.getHeaderField("Lamport-Clock");
      if (serverClock != null) {
        int receivedClock = Integer.parseInt(serverClock);
        AggregationServer.LamportClock.updateAndGet(receivedClock);
      } else {
        AggregationServer.LamportClock.incrementAndGet();
      }

      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(
          new InputStreamReader(conn.getInputStream())
        );
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
          content.append(inputLine);
        }

        // Parse the JSON response
        Gson gson = new Gson();
        Map<String, Object> weatherData = gson.fromJson(
          content.toString(),
          Map.class
        );

        // Extract the server's Lamport clock from the JSON
        Object serverLamportClock = weatherData.get("lamport_clock");

        System.out.println("Weather Data get successfully.");

        // Display the weather data including Lamport Clock
        System.out.println(
          "Server Lamport Clock: " +
          serverLamportClock +
          ", Client Lamport Clock: " +
          AggregationServer.LamportClock.getValue()
        );

        for (Map.Entry<String, Object> entry : weatherData.entrySet()) {
          System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        in.close();
        conn.disconnect();
      } else if (responseCode == 204) {
        System.out.println("No data avaliable.");
      } else {
        System.out.println("Error: " + responseCode);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      System.err.println("Invalid URI: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
