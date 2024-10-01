import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AggregationServerTest {

  private static final int TEST_PORT = 8000;
  private static final String TEST_URL =
    "http://localhost:" + TEST_PORT + "/weather";
  private static final String TEST_WEATHER_DATA_FILE = "./weather_data.json";
  private HttpServer server;

  @Before
  public void setUp() throws Exception {
    // Start the server
    server = HttpServer.create(new InetSocketAddress(TEST_PORT), 0);
    server.createContext("/weather", new AggregationServer.WeatherHandler());
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    // Stop the server and delete test file
    server.stop(0);
    Files.deleteIfExists(Paths.get(TEST_WEATHER_DATA_FILE));
  }

  @Test
  public void testPutAndGetRequest() throws Exception {
    // test data
    String testData =
      "{\"id\":\"IDS60901\",\"name\":\"Adelaide (West Terrace /  ngayirdapira)\",\"state\":\"SA\",\"air_temp\":13.3,\"wind_spd_kmh\":15}";

    // send PUT request
    HttpURLConnection putConn = (HttpURLConnection) new URL(TEST_URL)
      .openConnection();
    putConn.setRequestProperty(
      "Lamport-Clock",
      String.valueOf(AggregationServer.LamportClock.getValue())
    );
    putConn.setRequestMethod("PUT");
    putConn.setRequestProperty("Content-Type", "application/json");
    putConn.setDoOutput(true);

    try (OutputStream os = putConn.getOutputStream()) {
      byte[] input = testData.getBytes("utf-8");
      os.write(input, 0, input.length);
    }

    assertEquals(200, putConn.getResponseCode());
    putConn.disconnect();

    // test the GET request
    HttpURLConnection getConn = (HttpURLConnection) new URL(TEST_URL)
      .openConnection();
    getConn.setRequestMethod("GET");

    assertEquals(200, getConn.getResponseCode());

    try (
      BufferedReader br = new BufferedReader(
        new InputStreamReader(getConn.getInputStream(), "utf-8")
      )
    ) {
      StringBuilder response = new StringBuilder();
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }

      // Parse JSON response
      Gson gson = new Gson();
      Map<String, Object> jsonResponse = gson.fromJson(
        response.toString(),
        new TypeToken<Map<String, Object>>() {}.getType()
      );

      // Check data
      assertEquals("IDS60901", jsonResponse.get("id"));
      assertEquals(
        "Adelaide (West Terrace /  ngayirdapira)",
        jsonResponse.get("name")
      );
      assertEquals("SA", jsonResponse.get("state"));
      assertEquals(
        13.3,
        ((Number) jsonResponse.get("air_temp")).doubleValue(),
        0.01
      );
      assertEquals(
        15.0,
        ((Number) jsonResponse.get("wind_spd_kmh")).doubleValue(),
        0.01
      );
    }

    getConn.disconnect();
  }

  @Test
  public void testLamportClock() throws Exception {
    // Send a PUT request with a Lamport clock value
    HttpURLConnection conn = (HttpURLConnection) new URL(TEST_URL)
      .openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Lamport-Clock", "5");
    conn.setDoOutput(true);

    String testData = "{\"temperature\":\"20\"}";
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = testData.getBytes("utf-8");
      os.write(input, 0, input.length);
    }

    assertEquals(200, conn.getResponseCode());

    // The server should have incremented the clock
    String serverClock = conn.getHeaderField("Lamport-Clock");
    assertTrue(Integer.parseInt(serverClock) > 5);
  }

  @Test
  public void testDifferentStatusCodes() throws Exception {
    // Test 200 OK (already covered in testPutAndGetRequest)

    // Test 201 Created
    testStatusCode("PUT", "{\"new\":\"data\"}", 200);

    // Test 400 for invalide request
    testStatusCode("DELETE", "{\"new\":\"data\"}", 400);

    // Test 204 for no Content
    // testStatusCode("PUT", "", 204);

    // Test 404 Not Found
    testStatusCode("GET", "", 404, "/nonexistent");

    // Test 500 Internal Server Error
    testStatusCode("PUT", "new:data", 500);
  }

  private void testStatusCode(String method, String body, int expectedStatus)
    throws Exception {
    testStatusCode(method, body, expectedStatus, "/weather");
  }

  private void testStatusCode(
    String method,
    String body,
    int expectedStatus,
    String path
  ) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(
      "http://localhost:" + TEST_PORT + path
    )
      .openConnection();
    conn.setRequestMethod(method);
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty(
      "Lamport-Clock",
      String.valueOf(AggregationServer.LamportClock.getValue())
    );

    if (!body.isEmpty()) {
      conn.setDoOutput(true);
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = body.getBytes("utf-8");
        os.write(input, 0, input.length);
      }
    }

    assertEquals(expectedStatus, conn.getResponseCode());
    conn.disconnect();
  }
}
