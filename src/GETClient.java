import java.io.*;
import java.net.*;

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

      int responseCode = conn.getResponseCode();
      if (responseCode == 200) {
        // read the data
        BufferedReader in = new BufferedReader(
          new InputStreamReader(conn.getInputStream())
        );
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
          content.append(inputLine).append("\n");
        }
        in.close();
        conn.disconnect();

        // display the weather data
        System.out.println("Weather Data:");
        System.out.println(content.toString());
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
